package com.loadtrack.service;

import com.loadtrack.dto.TruckRequest;
import com.loadtrack.entity.Truck;
import com.loadtrack.exception.DuplicateEntryException;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.TruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TruckService {

    @Autowired
    private TruckRepository truckRepository;

    public List<Truck> getAllTrucks() {
        return truckRepository.findAll();
    }

    public Truck getTruckById(Integer id) {
        return truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));
    }

    public Truck addTruck(TruckRequest request) {
        if (truckRepository.existsByTruckNumber(request.getTruckNumber())) {
            throw new DuplicateEntryException("Truck number already exists: " + request.getTruckNumber());
        }
        Truck truck = new Truck();
        mapRequestToTruck(request, truck);
        return truckRepository.save(truck);
    }

    public Truck updateTruck(Integer id, TruckRequest request) {
        Truck truck = getTruckById(id);

        // Allow same truck number on update (only check if changed)
        if (!truck.getTruckNumber().equals(request.getTruckNumber())
                && truckRepository.existsByTruckNumber(request.getTruckNumber())) {
            throw new DuplicateEntryException("Truck number already exists: " + request.getTruckNumber());
        }

        mapRequestToTruck(request, truck);
        return truckRepository.save(truck);
    }

    public void deleteTruck(Integer id) {
        Truck truck = getTruckById(id);
        truckRepository.delete(truck);
    }

    private void mapRequestToTruck(TruckRequest request, Truck truck) {
        truck.setTruckNumber(request.getTruckNumber());
        truck.setModel(request.getModel());
        truck.setCapacityTons(request.getCapacityTons());
        truck.setInsuranceNumber(request.getInsuranceNumber());
        truck.setRcNumber(request.getRcNumber());
        if (request.getStatus() != null) {
            truck.setStatus(Truck.TruckStatus.valueOf(request.getStatus()));
        }
    }
}
