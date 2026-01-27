package com.medibridge.sos_ambulance_service_medibridge.domain.service;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;
    private final AuditService auditService;

    @Transactional
    public Ambulance register(String registrationNumber, String driverUserId, String contactPhone) {
        if (ambulanceRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new IllegalArgumentException("Registration number already exists");
        }

        Ambulance ambulance = Ambulance.builder()
                .registrationNumber(registrationNumber)
                .driverUserId(driverUserId)
                .status(AmbulanceStatus.OFF_DUTY)
                .contactPhone(contactPhone)
                .serviceRadiusKm(15.0) // Default 15km
                .build();

        Ambulance saved = ambulanceRepository.save(ambulance);
        auditService.log(driverUserId, "ADMIN", "REGISTER_AMBULANCE", "AMBULANCE", saved.getId().toString(),
                "Registered " + registrationNumber);
        return saved;
    }

    @Transactional
    public void updateStatus(String driverUserId, AmbulanceStatus status) {
        Ambulance ambulance = ambulanceRepository.findByDriverUserId(driverUserId)
                .orElseThrow(() -> new EntityNotFoundException("Ambulance not found for user"));

        ambulance.setStatus(status);
        ambulanceRepository.save(ambulance);
        auditService.log(driverUserId, "AMBULANCE", "UPDATE_STATUS", "AMBULANCE", ambulance.getId().toString(),
                "Status: " + status);
    }

    @Transactional
    public void updateLocation(String driverUserId, Double lat, Double lng) {
        Ambulance ambulance = ambulanceRepository.findByDriverUserId(driverUserId)
                .orElseThrow(() -> new EntityNotFoundException("Ambulance not found for user"));

        ambulance.setLastLat(lat);
        ambulance.setLastLng(lng);
        ambulance.setLastLocationAt(LocalDateTime.now());
        ambulanceRepository.save(ambulance);
        // We typically do NOT audit log every location update to DB to avoid
        // spam/bloat.
    }

    public List<Ambulance> findOnDutyNearby(Double lat, Double lng, Double radiusMeters) {
        return ambulanceRepository.findNearbyAmbulances(lat, lng, radiusMeters, AmbulanceStatus.ON_DUTY, 5);
    }

    public Ambulance getByDriverId(String driverUserId) {
        return ambulanceRepository.findByDriverUserId(driverUserId)
                .orElseThrow(() -> new EntityNotFoundException("Ambulance not found"));
    }

    public Ambulance getById(UUID id) {
        return ambulanceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ambulance not found"));
    }
}
