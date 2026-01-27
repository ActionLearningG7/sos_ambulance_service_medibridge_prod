package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.EmergencyRequest;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.EmergencyRequestRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/sos-incidents")
@RequiredArgsConstructor
public class AdminSosIncidentController {

    private final EmergencyRequestRepository requestRepository;
    private final AmbulanceRepository ambulanceRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SosIncidentResponse>> getIncidents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {

        if (search != null && search.trim().isEmpty()) {
            search = null;
        }

        EmergencyStatus emergencyStatus = null;
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                emergencyStatus = EmergencyStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // ignore invalid status
            }
        }

        Page<EmergencyRequest> requests = requestRepository.findAllBySearch(search, emergencyStatus, pageable);
        return ResponseEntity.ok(requests.map(this::mapToResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SosIncidentResponse> getIncident(@PathVariable UUID id) {
        return requestRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private SosIncidentResponse mapToResponse(EmergencyRequest req) {
        String ambulanceReg = null;
        String driverName = null;
        String driverId = null;

        if (req.getAssignedAmbulanceId() != null) {
            Ambulance amb = ambulanceRepository.findById(req.getAssignedAmbulanceId()).orElse(null);
            if (amb != null) {
                ambulanceReg = amb.getRegistrationNumber();
                driverId = amb.getDriverUserId();
                // We'd ideally need to fetch driver name from User Service, but for now we'll
                // send ID or Placeholder
                driverName = "Driver "
                        + (amb.getDriverUserId() != null ? amb.getDriverUserId().substring(0, 5) : "Unknown");
            }
        }

        return SosIncidentResponse.builder()
                .id(req.getId())
                .requestNumber(req.getRequestNumber())
                .patientId(req.getPatientId())
                .patientName(req.getPatientName())
                .patientPhone(req.getContactPhone())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .status(req.getStatus().toString())
                .assignedAmbulanceReg(ambulanceReg)
                .assignedDriverName(driverName)
                .assignedDriverId(driverId)
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    public static class SosIncidentResponse {
        private UUID id;
        private String requestNumber;
        private String patientId;
        private String patientName;
        private String patientPhone;
        private Double pickupLat;
        private Double pickupLng;
        private String status;
        private String assignedAmbulanceReg;
        private String assignedDriverName; // Placeholder or fetched
        private String assignedDriverId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
