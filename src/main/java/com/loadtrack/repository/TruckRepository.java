package com.loadtrack.repository;

import com.loadtrack.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TruckRepository extends JpaRepository<Truck, Integer> {
    boolean existsByTruckNumber(String truckNumber);
    Optional<Truck> findByTruckNumber(String truckNumber);
}
