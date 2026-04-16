package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.PrayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrayerRequestRepository extends JpaRepository<PrayerRequest, UUID> {

    // Get all unanswered prayer requests for a missionary
    List<PrayerRequest> findAllByMissionaryIdAndIsAnsweredFalseOrderByCreatedAtDesc(UUID missionaryId);

    // Get all requests for a missionary, newest first
    List<PrayerRequest> findAllByMissionaryIdOrderByCreatedAtDesc(UUID missionaryId);

    // Get all answered prayer requests
    List<PrayerRequest> findAllByMissionaryIdAndIsAnsweredTrueOrderByCreatedAtDesc(UUID missionaryId);
}