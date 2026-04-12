package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find all logs for a specific user to see their history
    List<AuditLog> findAllByUserIdOrderByTimestampDesc(UUID userId);

    // Find specific types of actions (e.g., all security alerts)
    List<AuditLog> findAllByActionOrderByTimestampDesc(String action);
}