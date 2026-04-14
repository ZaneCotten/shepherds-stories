package com.shepherdsstories.factories;

import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    private final UserFactory userFactory = new UserFactory();

    @Test
    void createMissionary_SetsProvidedReferenceNumber() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        String referenceNumber = "SYNC_CODE_123456";

        User user = new User();
        MissionaryProfile profile = userFactory.createMissionary(user, dto, referenceNumber);

        assertEquals(referenceNumber, profile.getReferenceNumber(), "Reference number should be the provided one");
    }

    @Test
    void createInviteCode_UsesProvidedCodeString() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        String referenceNumber = "TESTCODE123";

        User user = new User();
        MissionaryProfile profile = userFactory.createMissionary(user, dto, referenceNumber);

        var inviteCode = userFactory.createInviteCode(profile, referenceNumber);

        assertEquals(referenceNumber, inviteCode.getCodeString(), "Invite code string should be the provided one");
        assertEquals(profile, inviteCode.getMissionary(), "Invite code should be linked to the missionary profile");
        assertTrue(inviteCode.getIsActive(), "Invite code should be active by default");
    }
}
