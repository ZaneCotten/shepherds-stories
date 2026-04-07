package com.shepherdsstories.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.shepherdsstories.utils.ValidationConstants.PRAYER_CONTENT_MAX_LENGTH;
import static com.shepherdsstories.utils.ValidationConstants.TITLE_MAX_LENGTH;

@Getter
@Setter
@Entity
@Table(name = "prayer_requests")
public class PrayerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false, length = PRAYER_CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "is_answered")
    private boolean isAnswered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // The missionary who created the request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missionary_id", nullable = false)
    private MissionaryProfile missionary;
}
