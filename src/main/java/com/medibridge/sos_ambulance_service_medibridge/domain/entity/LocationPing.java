package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Location Ping Entity
 * Records ambulance location history for tracking and analytics
 * Useful for replay, debugging, and performance metrics
 */
@Entity
@Table(name = "location_pings", indexes = {
        @Index(name = "idx_ambulance_id", columnList = "ambulance_id"),
        @Index(name = "idx_incident_id", columnList = "incident_id"),
        @Index(name = "idx_driver_user_id", columnList = "driver_user_id"),
        @Index(name = "idx_pinged_at", columnList = "pinged_at"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationPing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ping_id", updatable = false, nullable = false)
    private UUID pingId;

    @Column(name = "ambulance_id", nullable = false)
    private UUID ambulanceId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "driver_user_id", nullable = false)
    private UUID driverUserId;

    // Location Data
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    // Accuracy Information
    @Column(name = "accuracy_meters")
    private Integer accuracyMeters;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    // Network Information
    @Column(name = "provider", length = 20)
    private String provider; // GPS, NETWORK, FUSED, etc.

    @Column(name = "signal_strength")
    private Integer signalStrength;

    // Timestamp
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "pinged_at", nullable = false)
    private LocalDateTime pinnedAt;

    // Organization
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    // Additional Context
    @Column(name = "on_duty_status", length = 20)
    private String onDutyStatus; // Snapshot of driver status at time of ping

    @Column(name = "notes", length = 255)
    private String notes;
}
