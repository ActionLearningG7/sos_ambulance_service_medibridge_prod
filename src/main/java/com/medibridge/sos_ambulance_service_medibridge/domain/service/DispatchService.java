package com.medibridge.sos_ambulance_service_medibridge.domain.service;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DispatchOffer;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.EmergencyRequest;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.SosIncident;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.DispatchOfferStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.IncidentStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.DispatchOfferRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.EmergencyRequestRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.SosIncidentRepository;
import com.medibridge.sos_ambulance_service_medibridge.service.NearestDispatchService;
import com.medibridge.sos_ambulance_service_medibridge.service.NotificationService;
import com.medibridge.sos_ambulance_service_medibridge.web.mapper.SosMapper;
import com.medibridge.sos_ambulance_service_medibridge.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final DispatchOfferRepository offerRepository;
    private final EmergencyRequestRepository requestRepository;
    private final AmbulanceService ambulanceService;
    private final WebSocketNotificationService notificationService;
    private final NotificationService trackingNotificationService;
    private final SosMapper sosMapper;

    // New dependencies
    private final SosIncidentRepository sosIncidentRepository;
    private final NearestDispatchService nearestDispatchService;

    @Transactional
    public void initiateDispatch(EmergencyRequest request) {
        // 1. Create linked SOS Incident (Bridge to NearestDispatchService)
        SosIncident incident = mapToSosIncident(request);

        // Default Org ID if null (Fallback for development/testing)
        if (incident.getOrganizationId() == null) {
            // Using the specific Organization ID from your existing ambulance data to
            // ensure matching
            incident.setOrganizationId(UUID.fromString("390fbfc0-3a00-4775-b258-d083175388ab"));
        }

        incident = sosIncidentRepository.save(incident);

        // 2. Try to assign nearest ambulance
        // This service uses Redis GEO to find and lock the ambulance
        nearestDispatchService.assignNearestAmbulance(incident);

        // 3. Sync result back to EmergencyRequest
        // Reload incident to get updates from assignNearestAmbulance
        incident = sosIncidentRepository.findById(incident.getIncidentId()).orElseThrow();

        if (incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.EN_ROUTE
                || incident.getStatus() == IncidentStatus.ARRIVED) {
            if (incident.getAmbulanceId() != null) {
                request.setAssignedAmbulanceId(incident.getAmbulanceId());
                request.setStatus(EmergencyStatus.ASSIGNED);
                requestRepository.save(request);

                // Notify User via WebSocket
                // NotificationService is handled inside NearestDispatchService for 'DRIVER',
                // but strict patient notification might need help
                // But NearestDispatchService notifies Patient too:
                // notificationService.notifyPatient(...) (Wait, looking at code:
                // notifyDriverAssignment, publishKafka)
                // It does NOT seem to notify Patient explicitly in assignNearestAmbulance
                // (lines 155 notifyDriverAssignment).
                // So we should notify patient here.

                var response = sosMapper.toResponse(request);
                notificationService.notifyPatient(request.getPatientId(), response);

                // Broadcast update to tracking topic so UI updates immediately
                trackingNotificationService.broadcastIncidentUpdate(incident, null, null);
            }
        } else if (incident.getStatus() == IncidentStatus.CANCELLED) {
            // Handle no ambulance found
            log.warn("Could not auto-assign ambulance: {}", incident.getCancellationReason());
            // Keep as CREATED or move to SEARCHING?
            // For now leave as CREATED so retries can happen or manual dispatch
        }
    }

    private SosIncident mapToSosIncident(EmergencyRequest req) {
        return SosIncident.builder()
                .patientId(UUID.fromString(req.getPatientId()))
                .patientName(req.getPatientName())
                .patientPhone(req.getContactPhone())
                .pickupLatitude(req.getPickupLat())
                .pickupLongitude(req.getPickupLng())
                .pickupAddress(req.getPickupLabel())
                .status(IncidentStatus.CREATED)
                .organizationId(req.getOrganizationId())
                .createdAt(req.getCreatedAt())
                .patientAge(req.getPatientAge())
                .bloodGroup(req.getBloodGroup())
                .patientNotes(req.getNotes())
                .build();
    }

    @Transactional
    public void acceptOffer(UUID offerId, String driverUserId) {
        DispatchOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (offer.getStatus() != DispatchOfferStatus.SENT) {
            throw new IllegalStateException("Offer is not pending anymore.");
        }

        Ambulance ambulance = ambulanceService.getById(offer.getAmbulanceId());
        if (!ambulance.getDriverUserId().equals(driverUserId)) {
            throw new SecurityException("Unauthorized accept attempt");
        }

        EmergencyRequest request = requestRepository.findById(offer.getEmergencyRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != EmergencyStatus.DISPATCHING && request.getStatus() != EmergencyStatus.CREATED) {
            offer.setStatus(DispatchOfferStatus.DECLINED);
            offerRepository.save(offer);
            throw new IllegalStateException("Request is no longer available.");
        }

        // Assign
        request.setAssignedAmbulanceId(ambulance.getId());
        request.setStatus(EmergencyStatus.ASSIGNED);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Update Offer
        offer.setStatus(DispatchOfferStatus.ACCEPTED);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);

        // Update Ambulance
        ambulanceService.updateStatus(driverUserId, AmbulanceStatus.BUSY);

        // Cancel others
        List<DispatchOffer> allOffers = offerRepository.findByEmergencyRequestId(request.getId());
        for (DispatchOffer o : allOffers) {
            if (!o.getId().equals(offerId) && o.getStatus() == DispatchOfferStatus.SENT) {
                o.setStatus(DispatchOfferStatus.CANCELLED);
            }
        }
        offerRepository.saveAll(allOffers);

        // Notify
        var response = sosMapper.toResponse(request);
        notificationService.notifyPatient(request.getPatientId(), response);
        notificationService.notifyAmbulanceAssignment(driverUserId, response);
    }

    public void declineOffer(UUID offerId, String userId) {
        DispatchOffer offer = offerRepository.findById(offerId).orElseThrow();
        offer.setStatus(DispatchOfferStatus.DECLINED);
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
    }
}
