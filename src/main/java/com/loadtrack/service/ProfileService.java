package com.loadtrack.service;

import com.loadtrack.dto.ChangePasswordRequest;
import com.loadtrack.dto.UpdateProfileRequest;
import com.loadtrack.entity.User;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public User getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User updateProfile(String username, UpdateProfileRequest request) {
        User user = getProfile(username);

        // Check if new username is taken by someone else
        if (!user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfilePhoto() != null) user.setProfilePhoto(request.getProfilePhoto());

        return userRepository.save(user);
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        User user = getProfile(username);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Confirm new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
