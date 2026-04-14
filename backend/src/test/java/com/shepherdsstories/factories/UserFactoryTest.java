package com.shepherdsstories.factories;

import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    private final UserFactory userFactory = new UserFactory();

    @Test
    void createMissionary_GeneratesReferenceNumber() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");

        User user = new User();
        MissionaryProfile profile = userFactory.createMissionary(user, dto);

        assertNotNull(profile.getReferenceNumber(), "Reference number should be generated at creation time");
        assertEquals(16, profile.getReferenceNumber().length(), "Reference number should be 16 characters long");
    }

    @Test
    void createInviteCode_GeneratesCodeString() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");

        User user = new User();
        MissionaryProfile profile = userFactory.createMissionary(user, dto);

        var inviteCode = userFactory.createInviteCode(profile);

        assertNotNull(inviteCode.getCodeString(), "Invite code string should be generated");
        assertEquals(16, inviteCode.getCodeString().length(), "Invite code string should be 16 characters long");
        assertEquals(profile, inviteCode.getMissionary(), "Invite code should be linked to the missionary profile");
        assertTrue(inviteCode.getIsActive(), "Invite code should be active by default");
    }
}
