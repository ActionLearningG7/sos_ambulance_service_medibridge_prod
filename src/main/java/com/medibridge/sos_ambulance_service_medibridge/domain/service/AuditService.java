package com.medibridge.sos_ambulance_service_medibridge.domain.service;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.AuditLog;
import com.medibridge.sos_ambulance_service_medibridge.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(String actorId, String role, String action, String targetType, String targetId, String summary) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .actorId(actorId)
                    .actorRole(role)
                    .actionType(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .summary(summary)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to audit log: {}", e.getMessage());
        }
    }
}
