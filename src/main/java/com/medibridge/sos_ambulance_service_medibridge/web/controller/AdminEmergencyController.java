package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.service.AmbulanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminEmergencyController {

    private final AmbulanceService ambulanceService;

    @PostMapping("/ambulances/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ambulance> register(@RequestBody RegisterAmbulanceRequest req) {
        return ResponseEntity.ok(
                ambulanceService.register(req.getRegistrationNumber(), req.getDriverUserId(), req.getContactPhone()));
    }

    @Data
    public static class RegisterAmbulanceRequest {
        private String registrationNumber;
        private String driverUserId;
        private String contactPhone;
    }
}
