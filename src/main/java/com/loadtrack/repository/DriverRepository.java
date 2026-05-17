package com.loadtrack.repository;

import com.loadtrack.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Integer> {
    boolean existsByLicenseNumber(String licenseNumber);
    List<Driver> findByAssignedTruckId(Integer truckId);
}
