package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DispatchOffer;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.DispatchOfferStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.service.AmbulanceService;
import com.medibridge.sos_ambulance_service_medibridge.domain.service.DispatchService;
import com.medibridge.sos_ambulance_service_medibridge.domain.service.EmergencyService;
import com.medibridge.sos_ambulance_service_medibridge.repository.DispatchOfferRepository;
import com.medibridge.sos_ambulance_service_medibridge.util.SecurityUtils;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.AmbulanceStatusUpdate;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.DispatchOfferDto;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.LocationUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ambulance")
@RequiredArgsConstructor
public class AmbulanceController {

    private final AmbulanceService ambulanceService;
    private final DispatchService dispatchService;
    private final EmergencyService emergencyService;
    private final DispatchOfferRepository offerRepository; // Direct access for list

    @PostMapping("/status")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> updateStatus(@RequestBody AmbulanceStatusUpdate update) {
        String userId = SecurityUtils.getCurrentUserId();
        ambulanceService.updateStatus(userId, update.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> updateLocation(@RequestBody LocationUpdate location) {
        String userId = SecurityUtils.getCurrentUserId();
        ambulanceService.updateLocation(userId, location.getLatitude(), location.getLongitude());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/offers")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<List<DispatchOfferDto>> getOffers() {
        String userId = SecurityUtils.getCurrentUserId();
        Ambulance amb = ambulanceService.getByDriverId(userId);

        List<DispatchOffer> offers = offerRepository.findByAmbulanceIdAndStatus(amb.getId(), DispatchOfferStatus.SENT);

        return ResponseEntity.ok(offers.stream().map(o -> DispatchOfferDto.builder()
                .offerId(o.getId())
                .requestId(o.getEmergencyRequestId())
                .status(o.getStatus())
                // In real app, we would fetch request details to populate DTO
                // Here leaving null for brevity or fetching if needed
                .build()).collect(Collectors.toList()));
    }

    @PostMapping("/offers/{offerId}/accept")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> acceptOffer(@PathVariable UUID offerId) {
        String userId = SecurityUtils.getCurrentUserId();
        dispatchService.acceptOffer(offerId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/offers/{offerId}/decline")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> declineOffer(@PathVariable UUID offerId) {
        String userId = SecurityUtils.getCurrentUserId();
        dispatchService.declineOffer(offerId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/arrived")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> arrived(@PathVariable UUID requestId) {
        String userId = SecurityUtils.getCurrentUserId();
        emergencyService.markArrived(userId, requestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/complete")
    @PreAuthorize("hasRole('AMBULANCE')")
    public ResponseEntity<Void> complete(@PathVariable UUID requestId) {
        String userId = SecurityUtils.getCurrentUserId();
        emergencyService.markCompleted(userId, requestId);
        return ResponseEntity.ok().build();
    }
}
