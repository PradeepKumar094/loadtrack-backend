package com.loadtrack.service;

import com.loadtrack.dto.LoginRequest;
import com.loadtrack.dto.LoginResponse;
import com.loadtrack.entity.User;
import com.loadtrack.repository.UserRepository;
import com.loadtrack.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().getName());

        // Determine linked ID (driver or dealer)
        Integer linkedId = null;
        if (user.getLinkedDriver() != null) {
            linkedId = user.getLinkedDriver().getId();
        } else if (user.getLinkedDealer() != null) {
            linkedId = user.getLinkedDealer().getId();
        }

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole().getName(),
                user.getId(),
                linkedId
        );
    }
}
