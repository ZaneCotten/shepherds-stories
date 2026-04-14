package com.shepherdsstories.factories;

import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.InviteCode;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.SupporterProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.utils.CodeGenerator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static com.shepherdsstories.utils.ValidationConstants.REF_CODE_LENGTH;

@Component
public class UserFactory {

    private static String defaultMissionaryName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        return fallbackNameFromEmail(email, "Missionary");
    }

    private static String defaultFirstName(String firstName, String email) {
        if (firstName != null && !firstName.isBlank()) {
            return firstName.trim();
        }
        return fallbackNameFromEmail(email, "Supporter");
    }

    private static String defaultLastName(String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            return lastName.trim();
        }
        return "Account";
    }

    private static String fallbackNameFromEmail(String email, String fallback) {
        if (email == null || email.isBlank()) {
            return fallback;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return fallback;
        }

        String localPart = email.substring(0, atIndex).trim();
        return localPart.isEmpty() ? fallback : localPart;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    public User createBaseUser(RegistrationRequestDTO dto) {
        User user = new User();
        user.setEmail(normalizeEmail(dto.getEmail()));
        user.setRole(dto.getRole());
        user.setIsLocked(false);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    public MissionaryProfile createMissionary(User user, RegistrationRequestDTO dto) {
        MissionaryProfile profile = new MissionaryProfile();
        profile.setUser(user); // Ties the UUIDs together via @MapsId
        profile.setMissionaryName(defaultMissionaryName(dto.getDisplayName(), dto.getEmail()));
        profile.setLocationRegion(dto.getRegion());
        profile.setBiography(dto.getBiography());
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setIsReferenceDisabled(false);
        profile.setReferenceNumber(CodeGenerator.generateReference(REF_CODE_LENGTH));
        // The unique reference number is also stored in a separate InviteCode entity.
        return profile;
    }

    public InviteCode createInviteCode(MissionaryProfile profile) {
        InviteCode inviteCode = new InviteCode();
        inviteCode.setMissionary(profile);
        inviteCode.setCodeString(CodeGenerator.generateReference(REF_CODE_LENGTH));
        inviteCode.setIsActive(true);
        inviteCode.setCreatedAt(OffsetDateTime.now());
        return inviteCode;
    }

    public SupporterProfile createSupporter(User user, RegistrationRequestDTO dto) {
        SupporterProfile profile = new SupporterProfile();
        profile.setUser(user);
        profile.setFirstName(defaultFirstName(dto.getFirstName(), dto.getEmail()));
        profile.setLastName(defaultLastName(dto.getLastName()));
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setIsVerified(false);
        return profile;
    }
}
