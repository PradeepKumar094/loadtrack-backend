package com.loadtrack.service;

import com.loadtrack.dto.DashboardResponse;
import com.loadtrack.entity.Driver;
import com.loadtrack.entity.Trip;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Service
public class DashboardService {

    @Autowired private TruckRepository truckRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SandRequestRepository sandRequestRepository;

    public DashboardResponse getAdminDashboard() {
        LocalDate now = LocalDate.now();
        BigDecimal monthlyEarnings = tripRepository.getMonthlyEarnings(now.getMonthValue(), now.getYear());

        long pendingVerifications = paymentRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getDealerPaidPending()))
                .count();

        long pendingSandRequests = sandRequestRepository.countByStatus(
                com.loadtrack.entity.SandRequest.RequestStatus.PENDING);

        return DashboardResponse.builder()
                .totalTrucks(truckRepository.count())
                .totalDrivers(driverRepository.count())
                .totalDealers(dealerRepository.count())
                .totalTrips(tripRepository.count())
                .pendingPayments(paymentRepository.countPendingPayments())
                .pendingVerifications(pendingVerifications)
                .pendingSandRequests(pendingSandRequests)
                .monthlyEarnings(monthlyEarnings != null ? monthlyEarnings : BigDecimal.ZERO)
                .build();
    }
    public DashboardResponse getDriverDashboard(Integer driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        List<Trip> trips = tripRepository.findByDriverId(driverId);
        long tripCount = trips.size();
        long completed = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED).count();
        long pending   = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.PENDING).count();
        long cancelled = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.CANCELLED).count();

        BigDecimal totalSalary = driver.getSalaryPerTrip()
                .multiply(BigDecimal.valueOf(completed));

        return DashboardResponse.builder()
                .assignedTrips(tripCount)
                .completedTrips(completed)
                .pendingTrips(pending)
                .cancelledTrips(cancelled)
                .totalSalaryEarned(totalSalary)
                .build();
    }

    public DashboardResponse getDealerDashboard(Integer dealerId) {
        BigDecimal pendingAmount = paymentRepository.getPendingAmountByDealer(dealerId);

        BigDecimal totalAmount = paymentRepository.findByTripDealerId(dealerId)
                .stream()
                .map(p -> p.getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = paymentRepository.findByTripDealerId(dealerId)
                .stream()
                .map(p -> p.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.builder()
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .pendingAmount(pendingAmount != null ? pendingAmount : BigDecimal.ZERO)
                .build();
    }
}
