package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.SupporterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupporterProfileRepository extends JpaRepository<SupporterProfile, UUID> {
}
