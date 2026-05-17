package com.loadtrack.service;

import com.loadtrack.dto.CreateUserRequest;
import com.loadtrack.dto.UserResponse;
import com.loadtrack.entity.*;
import com.loadtrack.exception.DuplicateEntryException;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Get all users (excluding admin)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !u.getRole().getName().equals("ADMIN"))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    // Create login account for driver or dealer
    public UserResponse createUser(CreateUserRequest request) {
        // Check username not taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateEntryException("Username '" + request.getUsername() + "' is already taken");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(true);

        // Link to driver
        if ("DRIVER".equals(request.getRole()) && request.getLinkedDriverId() != null) {
            Driver driver = driverRepository.findById(request.getLinkedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

            // Check driver doesn't already have an account
            boolean alreadyHasAccount = userRepository.findAll().stream()
                    .anyMatch(u -> u.getLinkedDriver() != null
                            && u.getLinkedDriver().getId().equals(request.getLinkedDriverId()));
            if (alreadyHasAccount) {
                throw new DuplicateEntryException("This driver already has a login account");
            }
            user.setLinkedDriver(driver);
        }

        // Link to dealer
        if ("DEALER".equals(request.getRole()) && request.getLinkedDealerId() != null) {
            Dealer dealer = dealerRepository.findById(request.getLinkedDealerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dealer not found"));

            boolean alreadyHasAccount = userRepository.findAll().stream()
                    .anyMatch(u -> u.getLinkedDealer() != null
                            && u.getLinkedDealer().getId().equals(request.getLinkedDealerId()));
            if (alreadyHasAccount) {
                throw new DuplicateEntryException("This dealer already has a login account");
            }
            user.setLinkedDealer(dealer);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    // Toggle active/inactive
    public UserResponse toggleStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole().getName().equals("ADMIN")) {
            throw new RuntimeException("Cannot deactivate admin account");
        }

        user.setStatus(!user.getStatus());
        return toResponse(userRepository.save(user));
    }

    // Reset password by admin
    public void resetPassword(Integer userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Delete user account
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole().getName().equals("ADMIN")) {
            throw new RuntimeException("Cannot delete admin account");
        }
        userRepository.delete(user);
    }

    // Get drivers that don't have a login account yet
    public List<Driver> getDriversWithoutAccount() {
        List<Integer> linkedDriverIds = userRepository.findAll().stream()
                .filter(u -> u.getLinkedDriver() != null)
                .map(u -> u.getLinkedDriver().getId())
                .collect(Collectors.toList());

        return driverRepository.findAll().stream()
                .filter(d -> !linkedDriverIds.contains(d.getId()))
                .collect(Collectors.toList());
    }

    // Get dealers that don't have a login account yet
    public List<Dealer> getDealersWithoutAccount() {
        List<Integer> linkedDealerIds = userRepository.findAll().stream()
                .filter(u -> u.getLinkedDealer() != null)
                .map(u -> u.getLinkedDealer().getId())
                .collect(Collectors.toList());

        return dealerRepository.findAll().stream()
                .filter(d -> !linkedDealerIds.contains(d.getId()))
                .collect(Collectors.toList());
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .phone(user.getPhone())
                .linkedDriverId(user.getLinkedDriver() != null ? user.getLinkedDriver().getId() : null)
                .linkedDriverName(user.getLinkedDriver() != null ? user.getLinkedDriver().getName() : null)
                .linkedDealerId(user.getLinkedDealer() != null ? user.getLinkedDealer().getId() : null)
                .linkedDealerName(user.getLinkedDealer() != null ? user.getLinkedDealer().getName() : null)
                .build();
    }
}
