package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.entities.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionRequest, UUID> {
    @Query("SELECT cr FROM ConnectionRequest cr JOIN FETCH cr.supporter WHERE cr.missionary.id = :missionaryId AND cr.status = :status")
    List<ConnectionRequest> findByMissionaryIdAndStatus(@Param("missionaryId") UUID missionaryId, @Param("status") RequestStatus status);

    Optional<ConnectionRequest> findByMissionaryIdAndSupporterId(UUID missionaryId, UUID supporterId);

    boolean existsByMissionaryIdAndSupporterIdAndStatus(UUID missionaryId, UUID supporterId, RequestStatus status);
}
