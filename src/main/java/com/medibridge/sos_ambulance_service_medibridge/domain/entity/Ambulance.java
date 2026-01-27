package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ambulances", indexes = {
        @Index(name = "idx_driver_user_id", columnList = "driverUserId"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String driverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbulanceStatus status;

    private Double lastLat;
    private Double lastLng;
    private LocalDateTime lastLocationAt;

    private Double serviceRadiusKm; // configurable radius per ambulance or global defaults

    private String contactPhone;
    private String organizationId;
}
