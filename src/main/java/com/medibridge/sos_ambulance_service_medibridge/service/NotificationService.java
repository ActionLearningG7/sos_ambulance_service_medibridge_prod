package com.medibridge.sos_ambulance_service_medibridge.service;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DriverAssignment;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.SosIncident;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Notification Service for WebSocket communications
 * Sends real-time updates to drivers and patients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Notify driver of new assignment
     */
    public void notifyDriverAssignment(UUID driverId, SosIncident incident, DriverAssignment assignment) {
        try {
            DriverAssignmentNotification notification = DriverAssignmentNotification.builder()
                    .driverId(driverId.toString())
                    .incidentId(incident.getIncidentId().toString())
                    .ambulanceId(assignment.getAmbulanceId().toString())
                    .patientLocation(new Location(incident.getPickupLatitude(), incident.getPickupLongitude()))
                    .estimatedEtaMinutes(assignment.getEstimatedEtaMinutes())
                    .distanceKm(assignment.getDistanceKm())
                    .patientPhone(incident.getPatientPhone())
                    .medicalDescription(incident.getMedicalDescription())
                    .timestamp(System.currentTimeMillis())
                    .build();

            String destination = "/topic/sos/driver/" + driverId;
            messagingTemplate.convertAndSend(destination, notification);

            log.info("Sent assignment notification to driver {} for incident {}",
                    driverId, incident.getIncidentId());

        } catch (Exception e) {
            log.error("Error sending driver assignment notification", e);
        }
    }

    /**
     * Broadcast incident tracking update to all subscribers
     */
    public void broadcastIncidentUpdate(SosIncident incident, Double lat, Double lng) {
        try {
            Location ambLoc = (lat != null && lng != null) ? new Location(lat, lng) : null;

            IncidentTrackingUpdate update = IncidentTrackingUpdate.builder()
                    .incidentId(incident.getIncidentId().toString())
                    .status(incident.getStatus().toString())
                    .ambulanceLocation(ambLoc)
                    .pickupLocation(new Location(incident.getPickupLatitude(), incident.getPickupLongitude()))
                    .eta(null) // Could calculate if needed
                    .distance(null)
                    .timestamp(System.currentTimeMillis())
                    .build();

            String destination = "/topic/sos/incidents/" + incident.getIncidentId() + "/tracking";
            messagingTemplate.convertAndSend(destination, update);

            log.debug("Broadcast incident tracking update for incident {}", incident.getIncidentId());

        } catch (Exception e) {
            log.error("Error broadcasting incident update", e);
        }
    }

    /**
     * Notify patient of ambulance arrival
     */
    public void notifyPatientArrival(UUID patientId, SosIncident incident) {
        try {
            PatientNotification notification = PatientNotification.builder()
                    .patientId(patientId.toString())
                    .incidentId(incident.getIncidentId().toString())
                    .status("ARRIVED")
                    .message("Ambulance has arrived at your location")
                    .driverPhone(null) // Would be populated from ambulance data
                    .timestamp(System.currentTimeMillis())
                    .build();

            String destination = "/topic/sos/patient/" + patientId;
            messagingTemplate.convertAndSend(destination, notification);

            log.info("Sent arrival notification to patient {} for incident {}",
                    patientId, incident.getIncidentId());

        } catch (Exception e) {
            log.error("Error sending patient arrival notification", e);
        }
    }

    // ===== DTOs =====

    @Data
    @AllArgsConstructor
    public static class Location {
        private Double latitude;
        private Double longitude;
    }

    @Data
    @AllArgsConstructor
    public static class DriverAssignmentNotification {
        private String driverId;
        private String incidentId;
        private String ambulanceId;
        private Location patientLocation;
        private Integer estimatedEtaMinutes;
        private Double distanceKm;
        private String patientPhone;
        private String medicalDescription;
        private Long timestamp;

        public DriverAssignmentNotification() {
        }

        public static DriverAssignmentNotificationBuilder builder() {
            return new DriverAssignmentNotificationBuilder();
        }

        public static class DriverAssignmentNotificationBuilder {
            private String driverId;
            private String incidentId;
            private String ambulanceId;
            private Location patientLocation;
            private Integer estimatedEtaMinutes;
            private Double distanceKm;
            private String patientPhone;
            private String medicalDescription;
            private Long timestamp;

            public DriverAssignmentNotificationBuilder driverId(String driverId) {
                this.driverId = driverId;
                return this;
            }

            public DriverAssignmentNotificationBuilder incidentId(String incidentId) {
                this.incidentId = incidentId;
                return this;
            }

            public DriverAssignmentNotificationBuilder ambulanceId(String ambulanceId) {
                this.ambulanceId = ambulanceId;
                return this;
            }

            public DriverAssignmentNotificationBuilder patientLocation(Location patientLocation) {
                this.patientLocation = patientLocation;
                return this;
            }

            public DriverAssignmentNotificationBuilder estimatedEtaMinutes(Integer estimatedEtaMinutes) {
                this.estimatedEtaMinutes = estimatedEtaMinutes;
                return this;
            }

            public DriverAssignmentNotificationBuilder distanceKm(Double distanceKm) {
                this.distanceKm = distanceKm;
                return this;
            }

            public DriverAssignmentNotificationBuilder patientPhone(String patientPhone) {
                this.patientPhone = patientPhone;
                return this;
            }

            public DriverAssignmentNotificationBuilder medicalDescription(String medicalDescription) {
                this.medicalDescription = medicalDescription;
                return this;
            }

            public DriverAssignmentNotificationBuilder timestamp(Long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public DriverAssignmentNotification build() {
                return new DriverAssignmentNotification(driverId, incidentId, ambulanceId, patientLocation,
                        estimatedEtaMinutes, distanceKm, patientPhone, medicalDescription, timestamp);
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class IncidentTrackingUpdate {
        private String incidentId;
        private String status;
        private Location ambulanceLocation;
        private Location pickupLocation;
        private Integer eta;
        private Double distance;
        private Long timestamp;

        public IncidentTrackingUpdate() {
        }

        public static IncidentTrackingUpdateBuilder builder() {
            return new IncidentTrackingUpdateBuilder();
        }

        public static class IncidentTrackingUpdateBuilder {
            private String incidentId;
            private String status;
            private Location ambulanceLocation;
            private Location pickupLocation;
            private Integer eta;
            private Double distance;
            private Long timestamp;

            public IncidentTrackingUpdateBuilder incidentId(String incidentId) {
                this.incidentId = incidentId;
                return this;
            }

            public IncidentTrackingUpdateBuilder status(String status) {
                this.status = status;
                return this;
            }

            public IncidentTrackingUpdateBuilder ambulanceLocation(Location ambulanceLocation) {
                this.ambulanceLocation = ambulanceLocation;
                return this;
            }

            public IncidentTrackingUpdateBuilder pickupLocation(Location pickupLocation) {
                this.pickupLocation = pickupLocation;
                return this;
            }

            public IncidentTrackingUpdateBuilder eta(Integer eta) {
                this.eta = eta;
                return this;
            }

            public IncidentTrackingUpdateBuilder distance(Double distance) {
                this.distance = distance;
                return this;
            }

            public IncidentTrackingUpdateBuilder timestamp(Long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public IncidentTrackingUpdate build() {
                return new IncidentTrackingUpdate(incidentId, status, ambulanceLocation, pickupLocation,
                        eta, distance, timestamp);
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class PatientNotification {
        private String patientId;
        private String incidentId;
        private String status;
        private String message;
        private String driverPhone;
        private Long timestamp;

        public PatientNotification() {
        }

        public static PatientNotificationBuilder builder() {
            return new PatientNotificationBuilder();
        }

        public static class PatientNotificationBuilder {
            private String patientId;
            private String incidentId;
            private String status;
            private String message;
            private String driverPhone;
            private Long timestamp;

            public PatientNotificationBuilder patientId(String patientId) {
                this.patientId = patientId;
                return this;
            }

            public PatientNotificationBuilder incidentId(String incidentId) {
                this.incidentId = incidentId;
                return this;
            }

            public PatientNotificationBuilder status(String status) {
                this.status = status;
                return this;
            }

            public PatientNotificationBuilder message(String message) {
                this.message = message;
                return this;
            }

            public PatientNotificationBuilder driverPhone(String driverPhone) {
                this.driverPhone = driverPhone;
                return this;
            }

            public PatientNotificationBuilder timestamp(Long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public PatientNotification build() {
                return new PatientNotification(patientId, incidentId, status, message, driverPhone, timestamp);
            }
        }
    }
}
