package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String actorId;
    private String actorRole;
    private String actionType; // e.g., SOS_CREATED
    private String targetType; // e.g., REQUEST
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
