package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AmbulanceStatusUpdate {
    @NotNull
    private AmbulanceStatus status;
}
