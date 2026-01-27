package com.medibridge.sos_ambulance_service_medibridge.domain.service;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.EmergencyRequest;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.EmergencyRequestRepository;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosRequest;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosResponse;
import com.medibridge.sos_ambulance_service_medibridge.web.mapper.SosMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyService {

        private final EmergencyRequestRepository requestRepository;
        private final DispatchService dispatchService;
        private final AuditService auditService;
        private final SosMapper sosMapper;
        private final AmbulanceService ambulanceService; // Inject for updates

        @Transactional
        public SosResponse createSos(String patientId, SosRequest req) {
                if (requestRepository.findByPatientIdAndStatusIn(patientId,
                                List.of(EmergencyStatus.CREATED, EmergencyStatus.DISPATCHING, EmergencyStatus.ASSIGNED,
                                                EmergencyStatus.EN_ROUTE, EmergencyStatus.ARRIVED))
                                .isPresent()) {
                        throw new IllegalStateException("Active SOS already exists");
                }

                EmergencyRequest entity = EmergencyRequest.builder()
                                .patientId(patientId)
                                .requestNumber("SOS-" + System.currentTimeMillis())
                                .patientName("Patient " + patientId.substring(0, 5))
                                .patientAge(30)
                                .bloodGroup("O+")
                                .contactPhone(req.getEmergencyContact())
                                .pickupLat(req.getPickupLat())
                                .pickupLng(req.getPickupLng())
                                .notes(req.getNotes())
                                .organizationId(req.getOrganizationId() != null
                                                ? UUID.fromString(req.getOrganizationId())
                                                : null)
                                .status(EmergencyStatus.CREATED)
                                .createdAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusHours(1))
                                .build();

                entity = requestRepository.save(entity);
                auditService.log(patientId, "PATIENT", "CREATE_SOS", "REQUEST", entity.getId().toString(),
                                "Created SOS");

                dispatchService.initiateDispatch(entity);

                return sosMapper.toResponse(entity);
        }

        public SosResponse getActive(String patientId) {
                return requestRepository.findByPatientIdAndStatusIn(patientId,
                                List.of(EmergencyStatus.CREATED, EmergencyStatus.DISPATCHING, EmergencyStatus.ASSIGNED,
                                                EmergencyStatus.EN_ROUTE, EmergencyStatus.ARRIVED))
                                .map(sosMapper::toResponse)
                                .orElse(null);
        }

        public List<SosResponse> getPatientRequests(String patientId) {
                return requestRepository.findByPatientId(patientId).stream()
                                .map(sosMapper::toResponse)
                                .toList();
        }

        public SosResponse getSosById(UUID id) {
                return requestRepository.findById(id)
                                .map(sosMapper::toResponse)
                                .orElseThrow(() -> new RuntimeException("SOS request not found: " + id));
        }

        @Transactional
        public void cancelSos(String patientId, UUID id, String reason) {
                EmergencyRequest req = requestRepository.findById(id).orElseThrow();
                if (!req.getPatientId().equals(patientId))
                        throw new SecurityException("Not your request");

                req.setStatus(EmergencyStatus.CANCELLED);
                req.setCancelReason(reason);
                req.setUpdatedAt(LocalDateTime.now());

                if (req.getAssignedAmbulanceId() != null) {
                        Ambulance amb = ambulanceService.getById(req.getAssignedAmbulanceId());
                        ambulanceService.updateStatus(amb.getDriverUserId(), AmbulanceStatus.ON_DUTY);
                }

                requestRepository.save(req);
                auditService.log(patientId, "PATIENT", "CANCEL_SOS", "REQUEST", id.toString(), reason);
        }

        @Transactional
        public void markArrived(String driverId, UUID requestId) {
                EmergencyRequest req = requestRepository.findById(requestId).orElseThrow();
                Ambulance amb = ambulanceService.getByDriverId(driverId);

                if (!req.getAssignedAmbulanceId().equals(amb.getId()))
                        throw new SecurityException("Not assigned");

                req.setStatus(EmergencyStatus.ARRIVED);
                req.setUpdatedAt(LocalDateTime.now());
                requestRepository.save(req);
                auditService.log(driverId, "AMBULANCE", "ARRIVED", "REQUEST", requestId.toString(),
                                "Ambulance Arrived");
        }

        @Transactional
        public void markCompleted(String driverId, UUID requestId) {
                EmergencyRequest req = requestRepository.findById(requestId).orElseThrow();
                Ambulance amb = ambulanceService.getByDriverId(driverId);

                if (!req.getAssignedAmbulanceId().equals(amb.getId()))
                        throw new SecurityException("Not assigned");

                req.setStatus(EmergencyStatus.COMPLETED);
                req.setUpdatedAt(LocalDateTime.now());
                requestRepository.save(req);

                ambulanceService.updateStatus(driverId, AmbulanceStatus.ON_DUTY);

                auditService.log(driverId, "AMBULANCE", "COMPLETED", "REQUEST", requestId.toString(), "Job Done");
        }

        @Transactional
        public void forceCancel(String adminId, UUID id) {
                EmergencyRequest req = requestRepository.findById(id).orElseThrow();
                req.setStatus(EmergencyStatus.CANCELLED);
                req.setCancelReason("Admin Force Cancel");
                if (req.getAssignedAmbulanceId() != null) {
                        Ambulance amb = ambulanceService.getById(req.getAssignedAmbulanceId());
                        ambulanceService.updateStatus(amb.getDriverUserId(), AmbulanceStatus.ON_DUTY);
                }
                requestRepository.save(req);
                auditService.log(adminId, "ADMIN", "FORCE_CANCEL", "REQUEST", id.toString(), "Admin cancelled");
        }
}
