package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.MissionaryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MissionaryProfileRepository extends JpaRepository<MissionaryProfile, UUID> {
}
