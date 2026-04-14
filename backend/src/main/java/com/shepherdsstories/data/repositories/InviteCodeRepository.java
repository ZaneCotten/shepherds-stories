package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {
}
