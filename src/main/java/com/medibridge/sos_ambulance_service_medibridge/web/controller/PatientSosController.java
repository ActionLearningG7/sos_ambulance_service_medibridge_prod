package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.service.EmergencyService;
import com.medibridge.sos_ambulance_service_medibridge.util.SecurityUtils;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.LocationUpdate;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosRequest;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sos")
@RequiredArgsConstructor
public class PatientSosController {

    private final EmergencyService emergencyService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<SosResponse> createSos(@Valid @RequestBody SosRequest req) {
        String patientId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(emergencyService.createSos(patientId, req));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<SosResponse> createSosAlternate(@Valid @RequestBody SosRequest req) {
        String patientId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(emergencyService.createSos(patientId, req));
    }

    @GetMapping("/me/active")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<SosResponse> getActive() {
        String patientId = SecurityUtils.getCurrentUserId();
        SosResponse res = emergencyService.getActive(patientId);
        if (res == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<SosResponse>> getMyRequests() {
        String patientId = SecurityUtils.getCurrentUserId();
        List<SosResponse> requests = emergencyService.getPatientRequests(patientId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<SosResponse> getSosById(@PathVariable UUID id) {
        SosResponse response = emergencyService.getSosById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, @RequestBody(required = false) String reason) {
        String patientId = SecurityUtils.getCurrentUserId();
        emergencyService.cancelSos(patientId, id, reason == null ? "User Cancelled" : reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> updateLocation(@PathVariable UUID id, @RequestBody LocationUpdate loc) {
        // In real app, validating this is their active request
        // Then pushing to assigned ambulance via WebSocket (broadcasting)
        // Ignoring for basic MVP flow, relying on initial pickup lat/lng
        return ResponseEntity.ok().build();
    }
}
