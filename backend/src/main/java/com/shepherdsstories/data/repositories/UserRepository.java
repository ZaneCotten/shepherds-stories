package com.shepherdsstories.data.repositories;

import com.shepherdsstories.data.entities.User;
import com.shepherdsstories.data.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Find a user by email
    Optional<User> findByEmail(String email);

    // Find all users with a specific role
    List<User> findAllByRole(Role role);

    // Check if an email already exists during registration
    boolean existsByEmail(String email);

    // Find users who aren't locked out
    List<User> findAllByIsLockedFalse();

    // Find users who are locked out
    List<User> findAllByIsLockedTrue();

}
