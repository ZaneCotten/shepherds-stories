package com.shepherdsstories.factories;

import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.SupporterProfile;
import com.shepherdsstories.entities.User;
import org.springframework.stereotype.Component;


@Component
public class UserFactory {

    public User createBaseUser(RegistrationRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        // ... set defaults like isLocked = false
        return user;
    }

    public MissionaryProfile createMissionary(User user, RegistrationRequestDTO dto) {
        MissionaryProfile profile = new MissionaryProfile();
        profile.setUser(user); // Ties the UUIDs together via @MapsId
        profile.setMissionaryName(dto.getDisplayName());
        profile.setLocationRegion(dto.getRegion());
        profile.setBiography(dto.getBiography());
        // Reference number is handled by @PrePersist in your entity
        return profile;
    }

    public SupporterProfile createSupporter(User user, RegistrationRequestDTO dto) {
        SupporterProfile profile = new SupporterProfile();
        profile.setUser(user);
        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        return profile;
    }
}