package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.SupporterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupporterProfileRepository extends JpaRepository<SupporterProfile, UUID> {
}
