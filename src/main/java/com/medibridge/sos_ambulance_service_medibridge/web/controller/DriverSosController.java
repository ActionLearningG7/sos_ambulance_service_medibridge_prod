package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DriverAssignment;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.LocationPing;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.SosIncident;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AssignmentStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.IncidentStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.DriverAssignmentRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.LocationPingRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.SosIncidentRepository;
import com.medibridge.sos_ambulance_service_medibridge.service.NearestDispatchService;
import com.medibridge.sos_ambulance_service_medibridge.service.NotificationService;
import com.medibridge.sos_ambulance_service_medibridge.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Driver Controller for SOS Incident Management
 * Handles driver assignments, status updates, and location tracking
 */
@RestController
@RequestMapping("/api/v1/driver/incidents")
@RequiredArgsConstructor
public class DriverSosController {

    private final DriverAssignmentRepository assignmentRepository;
    private final SosIncidentRepository incidentRepository;
    private final LocationPingRepository locationPingRepository;
    private final NearestDispatchService dispatchService;
    private final NotificationService notificationService;

    /**
     * Get active incident assigned to driver
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<?> getActiveIncident() {
        UUID driverId = UUID.fromString(SecurityUtils.getCurrentUserId());

        Optional<DriverAssignment> assignment = assignmentRepository.findByDriverUserId(driverId)
                .stream()
                .filter(a -> a.getStatus() != AssignmentStatus.COMPLETED &&
                        a.getStatus() != AssignmentStatus.CANCELLED)
                .findFirst();

        if (assignment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        SosIncident incident = incidentRepository.findById(assignment.get().getIncidentId())
                .orElse(null);

        return ResponseEntity.ok(new ActiveIncidentResponse(
                assignment.get().getAssignmentId(),
                incident,
                assignment.get()));
    }

    /**
     * Accept incident assignment
     */
    @PostMapping("/{incidentId}/accept")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> acceptIncident(@PathVariable UUID incidentId) {
        DriverAssignment assignment = assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        SosIncident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setStatus(IncidentStatus.ASSIGNED);
            incidentRepository.save(incident);
            notificationService.broadcastIncidentUpdate(incident, null, null);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Mark incident as en-route
     */
    @PostMapping("/{incidentId}/en-route")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> markEnRoute(@PathVariable UUID incidentId) {
        DriverAssignment assignment = assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setStatus(AssignmentStatus.EN_ROUTE);
        assignment.setStartedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        SosIncident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setStatus(IncidentStatus.EN_ROUTE);
            incident.setEnRouteAt(LocalDateTime.now());
            incidentRepository.save(incident);
            notificationService.broadcastIncidentUpdate(incident, null, null);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Mark incident as arrived
     */
    @PostMapping("/{incidentId}/arrived")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> markArrived(@PathVariable UUID incidentId) {
        DriverAssignment assignment = assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setStatus(AssignmentStatus.ARRIVED);
        assignment.setArrivedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        SosIncident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setStatus(IncidentStatus.ARRIVED);
            incident.setArrivedAt(LocalDateTime.now());
            incidentRepository.save(incident);
            notificationService.notifyPatientArrival(incident.getPatientId(), incident);
            notificationService.broadcastIncidentUpdate(incident, null, null);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Confirm patient pickup
     */
    @PostMapping("/{incidentId}/pickup")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> confirmPickup(@PathVariable UUID incidentId) {
        DriverAssignment assignment = assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setStatus(AssignmentStatus.PICKED_UP);
        assignment.setPickupAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        SosIncident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setStatus(IncidentStatus.PICKUP);
            incident.setPickupAt(LocalDateTime.now());
            incidentRepository.save(incident);
            notificationService.broadcastIncidentUpdate(incident, null, null);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Complete incident
     */
    @PostMapping("/{incidentId}/complete")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> completeIncident(
            @PathVariable UUID incidentId,
            @RequestBody(required = false) CompleteIncidentRequest request) {

        DriverAssignment assignment = assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        if (request != null && request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }
        assignmentRepository.save(assignment);

        SosIncident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setStatus(IncidentStatus.COMPLETED);
            incident.setCompletedAt(LocalDateTime.now());
            if (request != null && request.getNotes() != null) {
                incident.setDriverNotes(request.getNotes());
            }
            incidentRepository.save(incident);
            notificationService.broadcastIncidentUpdate(incident, null, null);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Location ping endpoint - updates driver location in Redis
     * Called every 5 seconds by driver mobile app
     */
    @PostMapping("/location-ping")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<Void> ping(@RequestBody LocationPingRequest request) {
        UUID driverId = UUID.fromString(SecurityUtils.getCurrentUserId());

        try {
            // Record location ping in database
            LocationPing ping = LocationPing.builder()
                    .driverUserId(driverId)
                    .ambulanceId(request.getAmbulanceId())
                    .incidentId(request.getIncidentId())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .accuracyMeters(request.getAccuracyMeters())
                    .speedKmh(request.getSpeedKmh())
                    .provider(request.getProvider())
                    .pinnedAt(LocalDateTime.now())
                    .organizationId(request.getOrganizationId())
                    .build();

            locationPingRepository.save(ping);

            // Update Redis live state and GEO index
            if (request.getAmbulanceId() != null) {
                dispatchService.updateAmbulanceLocation(
                        request.getAmbulanceId(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getOrganizationId().toString());
            }

            // Broadcast location update to subscribers
            if (request.getIncidentId() != null) {
                SosIncident incident = incidentRepository.findById(request.getIncidentId()).orElse(null);
                if (incident != null) {
                    notificationService.broadcastIncidentUpdate(incident, request.getLatitude(),
                            request.getLongitude());
                }
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Log error but don't fail - location pings are not critical
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== DTOs =====

    @Data
    @AllArgsConstructor
    public static class LocationPingRequest {
        private UUID ambulanceId;
        private UUID incidentId;
        private Double latitude;
        private Double longitude;
        private Integer accuracyMeters;
        private Double speedKmh;
        private String provider;
        private UUID organizationId;
    }

    @Data
    @AllArgsConstructor
    public static class CompleteIncidentRequest {
        private String notes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ActiveIncidentResponse {
        private UUID assignmentId;
        private SosIncident incident;
        private DriverAssignment assignment;
    }
}
