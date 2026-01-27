package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin Controller for Ambulance Fleet Management
 */
@RestController
@RequestMapping("/api/v1/admin/ambulances")
@RequiredArgsConstructor
public class AdminAmbulanceController {

    private final AmbulanceRepository ambulanceRepository;
    private final com.medibridge.sos_ambulance_service_medibridge.client.feign.UserServiceClient userServiceClient;
    private final com.medibridge.sos_ambulance_service_medibridge.service.NearestDispatchService nearestDispatchService;

    /**
     * Create new ambulance
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceResponse> createAmbulance(@RequestBody CreateAmbulanceRequest request) {
        Double lat = request.getHomeBaseLat();
        Double lng = request.getHomeBaseLng();

        // If not provided in request, try to fetch from User Service
        if (lat == null || lng == null) {
            try {
                var addressResponse = userServiceClient.getOrganizationAddress();
                if (addressResponse != null) {
                    lat = addressResponse.getLatitude();
                    lng = addressResponse.getLongitude();
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch organization address: " + e.getMessage());
            }
        }

        // Fallback to New Delhi if still null (Crucial for demo/testing)
        if (lat == null || lng == null) {
            lat = 28.6139;
            lng = 77.2090;
        }

        Ambulance ambulance = Ambulance.builder()
                .registrationNumber(request.getRegistrationNumber())
                .driverUserId(request.getDriverUserId())
                .status(AmbulanceStatus.ON_DUTY)
                .organizationId(request.getOrganizationId() != null ? request.getOrganizationId()
                        : "00000000-0000-0000-0000-000000000000")
                .contactPhone(request.getContactPhone())
                .serviceRadiusKm(request.getServiceRadiusKm() != null ? request.getServiceRadiusKm() : 5.0)
                .lastLat(lat)
                .lastLng(lng)
                .lastLocationAt(LocalDateTime.now())
                .build();

        ambulance = ambulanceRepository.save(ambulance);

        // Sync with Redis for Auto-Dispatch
        if (ambulance.getStatus() == AmbulanceStatus.ON_DUTY && ambulance.getLastLat() != null
                && ambulance.getLastLng() != null) {
            try {
                // Ensure organization ID is a string if necessary, but method takes String
                // orgId
                nearestDispatchService.updateAmbulanceLocation(
                        ambulance.getId(),
                        ambulance.getLastLat(),
                        ambulance.getLastLng(),
                        ambulance.getOrganizationId() // This is String in Ambulance entity based on earlier context?
                                                      // Let's check getter return type if failed.
                );
            } catch (Exception e) {
                // Log error
                System.err.println("Failed to sync ambulance location to Redis: " + e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(ambulance));
    }

    /**
     * Get all ambulances
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AmbulanceResponse>> getAllAmbulances(Pageable pageable) {
        Page<Ambulance> ambulances = ambulanceRepository.findAll(pageable);
        return ResponseEntity.ok(ambulances.map(this::mapToResponse));
    }

    /**
     * Get ambulance by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceResponse> getAmbulance(@PathVariable UUID id) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));
        return ResponseEntity.ok(mapToResponse(ambulance));
    }

    /**
     * Update ambulance
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceResponse> updateAmbulance(
            @PathVariable UUID id,
            @RequestBody UpdateAmbulanceRequest request) {

        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

        if (request.getRegistrationNumber() != null) {
            ambulance.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getContactPhone() != null) {
            ambulance.setContactPhone(request.getContactPhone());
        }
        if (request.getServiceRadiusKm() != null) {
            ambulance.setServiceRadiusKm(request.getServiceRadiusKm());
        }
        if (request.getHomeBaseLat() != null && request.getHomeBaseLng() != null) {
            ambulance.setLastLat(request.getHomeBaseLat());
            ambulance.setLastLng(request.getHomeBaseLng());
            ambulance.setLastLocationAt(LocalDateTime.now());
        }

        ambulance = ambulanceRepository.save(ambulance);

        // Sync with Redis
        if (ambulance.getStatus() == AmbulanceStatus.ON_DUTY && ambulance.getLastLat() != null) {
            try {
                nearestDispatchService.updateAmbulanceLocation(
                        ambulance.getId(),
                        ambulance.getLastLat(),
                        ambulance.getLastLng(),
                        ambulance.getOrganizationId());
            } catch (Exception e) {
                System.err.println("Failed to sync ambulance location to Redis: " + e.getMessage());
            }
        }
        return ResponseEntity.ok(mapToResponse(ambulance));
    }

    /**
     * Assign driver to ambulance
     */
    @PatchMapping("/{id}/assign-driver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceResponse> assignDriver(
            @PathVariable UUID id,
            @RequestBody AssignDriverRequest request) {

        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

        ambulance.setDriverUserId(request.getDriverUserId());
        ambulance = ambulanceRepository.save(ambulance);

        return ResponseEntity.ok(mapToResponse(ambulance));
    }

    /**
     * Update ambulance status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {

        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

        ambulance.setStatus(AmbulanceStatus.valueOf(request.getStatus()));
        ambulance = ambulanceRepository.save(ambulance);

        if (ambulance.getLastLat() != null && ambulance.getLastLng() != null) {
            try {
                nearestDispatchService.updateAmbulanceLocation(
                        ambulance.getId(),
                        ambulance.getLastLat(),
                        ambulance.getLastLng(),
                        ambulance.getOrganizationId());
            } catch (Exception e) {
                System.err.println("Failed to sync ambulance location to Redis: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(mapToResponse(ambulance));
    }

    /**
     * Delete ambulance
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAmbulance(@PathVariable UUID id) {
        ambulanceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AmbulanceResponse mapToResponse(Ambulance ambulance) {
        return AmbulanceResponse.builder()
                .id(ambulance.getId())
                .registrationNumber(ambulance.getRegistrationNumber())
                .driverUserId(ambulance.getDriverUserId())
                .status(ambulance.getStatus().toString())
                .lastLat(ambulance.getLastLat())
                .lastLng(ambulance.getLastLng())
                .lastLocationAt(ambulance.getLastLocationAt())
                .serviceRadiusKm(ambulance.getServiceRadiusKm())
                .contactPhone(ambulance.getContactPhone())
                .organizationId(ambulance.getOrganizationId())
                .build();
    }

    // ===== DTOs =====

    @Data
    @AllArgsConstructor
    public static class CreateAmbulanceRequest {
        private String registrationNumber;
        private String driverUserId;
        private String organizationId;
        private String contactPhone;
        private Double homeBaseLat;
        private Double homeBaseLng;
        private Double serviceRadiusKm;
    }

    @Data
    @AllArgsConstructor
    public static class UpdateAmbulanceRequest {
        private String registrationNumber;
        private String contactPhone;
        private Double serviceRadiusKm;
        private Double homeBaseLat;
        private Double homeBaseLng;
    }

    @Data
    @AllArgsConstructor
    public static class AssignDriverRequest {
        private String driverUserId;
    }

    @Data
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class AmbulanceResponse {
        private UUID id;
        private String registrationNumber;
        private String driverUserId;
        private String status;
        private Double lastLat;
        private Double lastLng;
        private LocalDateTime lastLocationAt;
        private Double serviceRadiusKm;
        private String contactPhone;
        private String organizationId;
    }
}
