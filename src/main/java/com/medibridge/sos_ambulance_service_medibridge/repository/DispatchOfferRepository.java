package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DispatchOffer;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.DispatchOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DispatchOfferRepository extends JpaRepository<DispatchOffer, UUID> {

    List<DispatchOffer> findByEmergencyRequestId(UUID emergencyRequestId);

    // For pending offers for a driver
    List<DispatchOffer> findByAmbulanceIdAndStatus(UUID ambulanceId, DispatchOfferStatus status);
}
