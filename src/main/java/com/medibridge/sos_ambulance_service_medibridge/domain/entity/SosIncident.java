package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SOS Incident Entity
 * Represents an emergency ambulance request from a patient
 * Tracks the entire lifecycle of an incident from creation to completion
 */
@Entity
@Table(name = "sos_incidents", indexes = {
        @Index(name = "idx_patient_id", columnList = "patient_id"),
        @Index(name = "idx_ambulance_id", columnList = "ambulance_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_organization_id", columnList = "organization_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "incident_id", updatable = false, nullable = false)
    private UUID incidentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "patient_name", length = 100)
    private String patientName;

    @Column(name = "patient_phone", length = 20)
    private String patientPhone;

    // Location Information
    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    // Status & Assignment
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.CREATED;

    @Column(name = "ambulance_id")
    private UUID ambulanceId;

    @Column(name = "driver_user_id")
    private UUID driverUserId;

    // Medical Information
    @Column(name = "medical_description", columnDefinition = "TEXT")
    private String medicalDescription;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    // Organization & Audit
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Timeline
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "en_route_at")
    private LocalDateTime enRouteAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    // Metrics
    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    @Column(name = "actual_distance_km")
    private Double actualDistanceKm;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    @Column(name = "actual_time_minutes")
    private Integer actualTimeMinutes;

    @Column(name = "response_time_seconds")
    private Integer responseTimeSeconds;

    // Notes & Comments
    @Column(name = "patient_notes", columnDefinition = "TEXT")
    private String patientNotes;

    @Column(name = "driver_notes", columnDefinition = "TEXT")
    private String driverNotes;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;
}
