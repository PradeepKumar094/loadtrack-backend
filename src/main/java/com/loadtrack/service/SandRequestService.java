package com.loadtrack.service;

import com.loadtrack.dto.AcceptRequestDto;
import com.loadtrack.dto.SandRequestDto;
import com.loadtrack.dto.TripRequest;
import com.loadtrack.entity.*;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SandRequestService {

    @Autowired private SandRequestRepository sandRequestRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private SandTypeRepository sandTypeRepository;
    @Autowired private TripService tripService;

    // ── Dealer creates a sand request ─────────────────────────────
    public SandRequest createRequest(Integer dealerId, SandRequestDto dto) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found"));
        SandType sandType = sandTypeRepository.findById(dto.getSandTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sand type not found"));

        SandRequest req = new SandRequest();
        req.setDealer(dealer);
        req.setSandType(sandType);
        req.setTons(dto.getTons());
        req.setSourceLocation(dto.getSourceLocation());
        req.setDestinationLocation(dto.getDestinationLocation());
        req.setDistanceKm(dto.getDistanceKm());
        req.setRequestedDate(dto.getRequestedDate());
        req.setRemarks(dto.getRemarks());
        req.setStatus(SandRequest.RequestStatus.PENDING);

        return sandRequestRepository.save(req);
    }

    // ── Get all requests (admin) ──────────────────────────────────
    public List<SandRequest> getAllRequests() {
        return sandRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    // ── Get pending requests (admin) ──────────────────────────────
    public List<SandRequest> getPendingRequests() {
        return sandRequestRepository.findByStatusOrderByCreatedAtDesc(SandRequest.RequestStatus.PENDING);
    }

    // ── Get requests by dealer ────────────────────────────────────
    public List<SandRequest> getRequestsByDealer(Integer dealerId) {
        return sandRequestRepository.findByDealerIdOrderByCreatedAtDesc(dealerId);
    }

    // ── Admin accepts request → auto-creates trip ─────────────────
    @Transactional
    public SandRequest acceptRequest(Integer requestId, AcceptRequestDto dto) {
        SandRequest req = sandRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (req.getStatus() != SandRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Request is already " + req.getStatus());
        }

        // Build TripRequest from sand request
        TripRequest tripRequest = new TripRequest();
        tripRequest.setTruckId(dto.getTruckId());
        tripRequest.setDriverId(dto.getDriverId());
        tripRequest.setDealerId(req.getDealer().getId());
        tripRequest.setSandTypeId(req.getSandType().getId());
        tripRequest.setTons(req.getTons());
        tripRequest.setSourceLocation(req.getSourceLocation());
        tripRequest.setDestinationLocation(req.getDestinationLocation());
        tripRequest.setDistanceKm(req.getDistanceKm());
        tripRequest.setTripDate(req.getRequestedDate());
        tripRequest.setStatus("PENDING");

        // Create the trip
        Trip trip = tripService.createTrip(tripRequest);

        // Update request status
        req.setStatus(SandRequest.RequestStatus.ACCEPTED);
        req.setAdminRemarks(dto.getAdminRemarks());
        req.setTrip(trip);

        return sandRequestRepository.save(req);
    }

    // ── Admin rejects request ─────────────────────────────────────
    public SandRequest rejectRequest(Integer requestId, String adminRemarks) {
        SandRequest req = sandRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (req.getStatus() != SandRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Request is already " + req.getStatus());
        }

        req.setStatus(SandRequest.RequestStatus.REJECTED);
        req.setAdminRemarks(adminRemarks);
        return sandRequestRepository.save(req);
    }

    public long countPendingRequests() {
        return sandRequestRepository.countByStatus(SandRequest.RequestStatus.PENDING);
    }
}
