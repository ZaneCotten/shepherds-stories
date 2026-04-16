package com.shepherdsstories.services;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.InviteCodeRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.InviteCode;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SynchronizationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;
    @Mock
    private SupporterProfileRepository supporterProfileRepository;
    @Mock
    private InviteCodeRepository inviteCodeRepository;
    @Mock
    private UserFactory userFactory;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                missionaryProfileRepository,
                supporterProfileRepository,
                inviteCodeRepository,
                userFactory,
                passwordEncoder
        );
    }

    @Test
    void register_EnsuresSameCodeUsedForProfileAndInviteCode() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("m@example.com");
        dto.setRole(Role.MISSIONARY);

        User user = new User();
        MissionaryProfile profile = new MissionaryProfile();
        String expectedCode = "SYNC_CODE_123456";
        profile.setReferenceNumber(expectedCode);

        InviteCode inviteCode = new InviteCode();

        when(userFactory.createBaseUser(dto)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);

        // Capture the code string passed to createMissionary and createInviteCode
        ArgumentCaptor<String> profileCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inviteCodeCaptor = ArgumentCaptor.forClass(String.class);

        when(userFactory.createMissionary(eq(user), eq(dto), profileCodeCaptor.capture())).thenReturn(profile);
        when(userFactory.createInviteCode(eq(profile), inviteCodeCaptor.capture())).thenReturn(inviteCode);

        registrationService.registerSocial(dto, "ID", AuthProvider.GOOGLE);

        assertEquals(profileCodeCaptor.getValue(), inviteCodeCaptor.getValue(), "The code string passed to both MissionaryProfile and InviteCode must be identical");
        assertEquals(16, profileCodeCaptor.getValue().length(), "The generated code should be 16 characters long");
    }
}
