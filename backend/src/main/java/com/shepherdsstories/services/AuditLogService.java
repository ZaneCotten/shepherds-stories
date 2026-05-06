package com.shepherdsstories.services;

import com.shepherdsstories.data.repositories.AuditLogRepository;
import com.shepherdsstories.entities.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String email, UUID userId, String details, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEmail(email != null ? email : "unknown");
        log.setUserId(userId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setTimestamp(OffsetDateTime.now());
        auditLogRepository.save(log);
    }
}
