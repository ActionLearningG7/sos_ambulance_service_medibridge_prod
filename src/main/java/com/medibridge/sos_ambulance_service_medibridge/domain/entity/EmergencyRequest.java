package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_requests", indexes = {
        @Index(name = "idx_request_number", columnList = "requestNumber", unique = true),
        @Index(name = "idx_patient_id", columnList = "patientId"),
        @Index(name = "idx_assigned_ambulance", columnList = "assignedAmbulanceId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String requestNumber; // Human readable ID

    @Column(nullable = false)
    private String patientId;

    @Column(name = "organization_id")
    private UUID organizationId;

    // Snapshot Data
    private String patientName;
    private Integer patientAge;
    private String bloodGroup;
    private String contactPhone;

    // Location
    @Column(nullable = false)
    private Double pickupLat;

    @Column(nullable = false)
    private Double pickupLng;

    private String pickupLabel;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyStatus status;

    private UUID assignedAmbulanceId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;

    private String cancelReason;
}
