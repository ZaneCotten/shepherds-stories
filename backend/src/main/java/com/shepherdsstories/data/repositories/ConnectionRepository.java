package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<ConnectionRequest, UUID> {
    List<ConnectionRequest> findAllByIdAndStatus(UUID id, Object status);

    // Check if a connection already exists
    boolean existsByMissionaryIdAndSupporterId(UUID missionaryId, UUID supporterId);
}
