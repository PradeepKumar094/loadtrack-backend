package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.SandTypeRequest;
import com.loadtrack.entity.SandType;
import com.loadtrack.service.SandTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sand-types")
public class SandTypeController {

    @Autowired
    private SandTypeService sandTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SandType>>> getAllSandTypes() {
        return ResponseEntity.ok(ApiResponse.success(sandTypeService.getAllSandTypes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SandType>> getSandTypeById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(sandTypeService.getSandTypeById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SandType>> addSandType(@Valid @RequestBody SandTypeRequest request) {
        SandType sandType = sandTypeService.addSandType(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Sand type added successfully", sandType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SandType>> updateSandType(
            @PathVariable Integer id,
            @Valid @RequestBody SandTypeRequest request) {
        SandType sandType = sandTypeService.updateSandType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Sand type updated successfully", sandType));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSandType(@PathVariable Integer id) {
        sandTypeService.deleteSandType(id);
        return ResponseEntity.ok(ApiResponse.success("Sand type deleted successfully", null));
    }
}
