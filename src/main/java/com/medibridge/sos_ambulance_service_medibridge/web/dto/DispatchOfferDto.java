package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.DispatchOfferStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DispatchOfferDto {
    private UUID offerId;
    private UUID requestId;
    private DispatchOfferStatus status;

    // Minimal Request Info for Decision
    private String pickupLabel;
    private Double pickupLat;
    private Double pickupLng;
    private Double distanceMeters;

    // Patient Snapshot (Minimal)
    private String patientFirstName;
    private Integer patientAge;
    // Hide full name/phone until accepted?
}
