package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Driver Assignment Entity
 * Tracks assignment of drivers to SOS incidents
 */
@Entity
@Table(name = "driver_assignments", indexes = {
        @Index(name = "idx_driver_user_id", columnList = "driver_user_id"),
        @Index(name = "idx_incident_id", columnList = "incident_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_assigned_at", columnList = "assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    private UUID assignmentId;

    @Column(name = "driver_user_id", nullable = false)
    private UUID driverUserId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "ambulance_id", nullable = false)
    private UUID ambulanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

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

    // Assignment Quality Metrics
    @Column(name = "estimated_eta_minutes")
    private Integer estimatedEtaMinutes;

    @Column(name = "actual_eta_minutes")
    private Integer actualEtaMinutes;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
