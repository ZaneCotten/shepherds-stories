package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.MissionaryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MissionaryProfileRepository extends JpaRepository<MissionaryProfile, UUID> {
}
