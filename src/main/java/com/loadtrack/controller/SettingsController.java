package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.entity.Settings;
import com.loadtrack.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsRepository settingsRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Settings>> getSettings() {
        Settings settings = settingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Settings not configured"));
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Settings>> updateSettings(@RequestBody Settings request) {
        Settings settings = settingsRepository.findAll().stream().findFirst()
                .orElse(new Settings());
        settings.setInterestRatePercent(request.getInterestRatePercent());
        settings.setAllowedDays(request.getAllowedDays());
        Settings saved = settingsRepository.save(settings);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", saved));
    }
}
