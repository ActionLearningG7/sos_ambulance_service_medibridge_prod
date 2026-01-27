package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationUpdate {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
