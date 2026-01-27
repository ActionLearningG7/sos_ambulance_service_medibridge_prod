package com.medibridge.sos_ambulance_service_medibridge.websocket;

import com.medibridge.sos_ambulance_service_medibridge.web.dto.DispatchOfferDto;
import com.medibridge.sos_ambulance_service_medibridge.web.dto.SosResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyPatient(String patientId, SosResponse response) {
        messagingTemplate.convertAndSendToUser(
                patientId,
                "/sos/updates",
                response);
    }

    public void notifyAmbulanceOffer(String driverUserId, DispatchOfferDto offer) {
        messagingTemplate.convertAndSendToUser(
                driverUserId,
                "/ambulance/offers",
                offer);
    }

    public void notifyAmbulanceAssignment(String driverUserId, SosResponse response) {
        messagingTemplate.convertAndSendToUser(
                driverUserId,
                "/ambulance/offers", // Or logic to show 'My Active Assignment'
                response);
    }

    public void broadcastToAdmin(Object payload) {
        messagingTemplate.convertAndSend("/topic/admin/sos", payload);
    }
}
