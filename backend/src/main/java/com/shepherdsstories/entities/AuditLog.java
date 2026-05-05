package com.shepherdsstories.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"timestamp\"", nullable = false)
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "\"action\"", nullable = false)
    private String action; // e.g., "LOGIN_ATTEMPT", "STORY_VIEW", "PROFILE_UPDATE"

    @Column(name = "user_id")
    private UUID userId; // The person performing the action (can be null for guest/failed login)

    @Column(nullable = false)
    private String email; // Recorded for convenience or failed login tracking

    @Column(columnDefinition = "TEXT")
    private String details; // Extra context (e.g., "Viewed Story ID: 456")

    @Column(name = "ip_address")
    private String ipAddress;
}
