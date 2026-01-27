package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SosResponse {
    private UUID id;
    private String requestNumber;
    private EmergencyStatus status;
    private LocalDateTime createdAt;

    // Request Details
    private Double pickupLat;
    private Double pickupLng;
    private String pickupLabel;

    // Assigned Ambulance (Null if not assigned)
    private AmbulanceDto assignedAmbulance;

    // ETA or Distance? (Optional)
    private Double estimatedDistanceMeters;
}
