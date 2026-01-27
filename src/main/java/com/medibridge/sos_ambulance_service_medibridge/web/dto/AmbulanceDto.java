package com.medibridge.sos_ambulance_service_medibridge.web.dto;

import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AmbulanceDto {
    private UUID id;
    private String registrationNumber;
    private String driverUserId;
    private AmbulanceStatus status;
    private Double lastLat;
    private Double lastLng;
    private String contactPhone;
}
