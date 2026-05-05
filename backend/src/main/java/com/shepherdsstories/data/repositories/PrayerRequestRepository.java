package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.PrayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrayerRequestRepository extends JpaRepository<PrayerRequest, UUID> {

    // Get all unanswered prayer requests for a missionary
    List<PrayerRequest> findAllByMissionaryIdAndAnsweredFalseOrderByCreatedAtDesc(UUID missionaryId);

    // Get all requests for a missionary, newest first
    List<PrayerRequest> findAllByMissionaryIdOrderByCreatedAtDesc(UUID missionaryId);

    // Get all answered prayer requests
    List<PrayerRequest> findAllByMissionaryIdAndAnsweredTrueOrderByCreatedAtDesc(UUID missionaryId);

    // Get all unanswered prayer requests for multiple missionaries
    List<PrayerRequest> findAllByMissionaryIdInAndAnsweredFalseOrderByCreatedAtDesc(List<UUID> missionaryIds);

    List<PrayerRequest> findAllByMissionaryIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID missionaryId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countByMissionaryId(UUID missionaryId);

    long countByMissionaryIdAndAnsweredTrue(UUID missionaryId);

    long countByMissionaryIdAndCreatedAtBetween(UUID missionaryId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countByMissionaryIdAndAnsweredTrueAndCreatedAtBetween(UUID missionaryId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}