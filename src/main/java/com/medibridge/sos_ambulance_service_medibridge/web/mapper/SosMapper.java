package com.medibridge.sos_ambulance_service_medibridge.web.mapper;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.EmergencyRequest;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceRepository;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.AmbulanceDto;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SosMapper {

    private final AmbulanceRepository ambulanceRepository;

    public SosResponse toResponse(EmergencyRequest req) {
        AmbulanceDto ambDto = null;
        if (req.getAssignedAmbulanceId() != null) {
            Optional<Ambulance> amb = ambulanceRepository.findById(req.getAssignedAmbulanceId());
            if (amb.isPresent()) {
                Ambulance a = amb.get();
                ambDto = AmbulanceDto.builder()
                        .id(a.getId())
                        .registrationNumber(a.getRegistrationNumber())
                        .driverUserId(a.getDriverUserId())
                        .status(a.getStatus())
                        .lastLat(a.getLastLat())
                        .lastLng(a.getLastLng())
                        .contactPhone(a.getContactPhone())
                        .build();
            }
        }

        return SosResponse.builder()
                .id(req.getId())
                .requestNumber(req.getRequestNumber())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .pickupLabel(req.getPickupLabel())
                .assignedAmbulance(ambDto)
                .build();
    }
}
