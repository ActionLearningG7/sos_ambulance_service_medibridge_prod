package com.medibridge.sos_ambulance_service_medibridge.domain.entity;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.DispatchOfferStatus;
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
@Table(name = "dispatch_offers", indexes = {
        @Index(name = "idx_request", columnList = "emergencyRequestId"),
        @Index(name = "idx_ambulance", columnList = "ambulanceId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DispatchOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID emergencyRequestId;

    @Column(nullable = false)
    private UUID ambulanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DispatchOfferStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime respondedAt;

    // Add optimistic locking or version if needed, but transactional service logic
    // usually sufficient for first-to-claim
}
