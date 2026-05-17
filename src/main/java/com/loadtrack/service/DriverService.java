package com.loadtrack.service;

import com.loadtrack.dto.DriverRequest;
import com.loadtrack.entity.*;
import com.loadtrack.exception.DuplicateEntryException;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class DriverService {

    @Autowired private DriverRepository driverRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DriverSalaryCreditRepository salaryCreditRepository;
    @Autowired private TripRepository tripRepository;

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriverById(Integer id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }

    @Transactional
    public Driver addDriver(DriverRequest request) {
        // Check duplicate license
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateEntryException("License number already exists: " + request.getLicenseNumber());
        }
        // Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateEntryException("Username '" + request.getUsername() + "' is already taken. Please choose another.");
        }

        // Create driver
        Driver driver = new Driver();
        mapRequestToDriver(request, driver);
        Driver savedDriver = driverRepository.save(driver);

        // Auto-create login account
        createUserAccount(request.getUsername(), request.getPassword(), "DRIVER", savedDriver, null);

        return savedDriver;
    }

    @Transactional
    public Driver updateDriver(Integer id, DriverRequest request) {
        Driver driver = getDriverById(id);

        if (!driver.getLicenseNumber().equals(request.getLicenseNumber())
                && driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateEntryException("License number already exists: " + request.getLicenseNumber());
        }

        // Check username uniqueness (only if username is being changed)
        User existingUser = userRepository.findAll().stream()
                .filter(u -> u.getLinkedDriver() != null && u.getLinkedDriver().getId().equals(id))
                .findFirst().orElse(null);

        if (existingUser != null && !existingUser.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateEntryException("Username '" + request.getUsername() + "' is already taken. Please choose another.");
            }
            existingUser.setUsername(request.getUsername());
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(existingUser);
        }

        mapRequestToDriver(request, driver);
        return driverRepository.save(driver);
    }

    @Transactional
    public void deleteDriver(Integer id) {
        Driver driver = getDriverById(id);

        // 1. Delete linked user account
        userRepository.findAll().stream()
                .filter(u -> u.getLinkedDriver() != null && u.getLinkedDriver().getId().equals(id))
                .forEach(userRepository::delete);

        // 2. Delete salary credits
        salaryCreditRepository.findByDriverIdOrderByCreditedAtDesc(id)
                .forEach(salaryCreditRepository::delete);

        // 3. Unlink driver from trips (set driver to null not possible due to NOT NULL — nullify assigned truck instead)
        //    Trips referencing this driver must be handled — set truck free first
        tripRepository.findByDriverId(id).forEach(trip -> {
            if (trip.getTruck() != null) {
                trip.getTruck().setStatus(Truck.TruckStatus.AVAILABLE);
                truckRepository.save(trip.getTruck());
            }
        });

        // 4. Delete driver (trips will cascade if FK allows, otherwise unlink)
        driver.setAssignedTruck(null);
        driverRepository.save(driver);
        driverRepository.delete(driver);
    }

    private void mapRequestToDriver(DriverRequest request, Driver driver) {
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setAddress(request.getAddress());
        driver.setSalaryPerTrip(request.getSalaryPerTrip());

        if (request.getAssignedTruckId() != null) {
            Truck truck = truckRepository.findById(request.getAssignedTruckId())
                    .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));
            driver.setAssignedTruck(truck);
        } else {
            driver.setAssignedTruck(null);
        }
    }

    private void createUserAccount(String username, String password, String roleName,
                                    Driver driver, com.loadtrack.entity.Dealer dealer) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(true);
        user.setLinkedDriver(driver);
        user.setLinkedDealer(dealer);
        userRepository.save(user);
    }
}
