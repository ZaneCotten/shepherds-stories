package com.shepherdsstories.data.repositories;

import com.shepherdsstories.entities.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {
    Optional<InviteCode> findByCodeString(String codeString);

    Optional<InviteCode> findByCodeStringIgnoreCase(String codeString);
}
