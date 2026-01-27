package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SosRequest {
    @NotNull(message = "Latitude is required")
    private Double pickupLat;

    @NotNull(message = "Longitude is required")
    private Double pickupLng;

    private String notes;
    private String emergencyContact; // Optional override
    private String organizationId;
}
