package com.shepherdsstories.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
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
