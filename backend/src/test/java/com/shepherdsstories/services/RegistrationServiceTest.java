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
import com.shepherdsstories.entities.SupporterProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

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
    void register_Missionary_SavesUserAndMissionaryProfile() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("missionary@example.com");
        dto.setPassword("password");
        dto.setRole(Role.MISSIONARY);

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(Role.MISSIONARY);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setUser(user);

        InviteCode inviteCode = new InviteCode();
        inviteCode.setMissionary(profile);

        when(userFactory.createBaseUser(dto)).thenReturn(user);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userFactory.createMissionary(eq(user), eq(dto), anyString())).thenReturn(profile);
        when(userFactory.createInviteCode(eq(profile), anyString())).thenReturn(inviteCode);

        registrationService.register(dto);

        verify(userRepository).save(user);
        verify(missionaryProfileRepository).save(profile);
        verify(inviteCodeRepository).save(inviteCode);
        verify(supporterProfileRepository, never()).save(any());
    }

    @Test
    void register_Supporter_SavesUserAndSupporterProfile() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("supporter@example.com");
        dto.setPassword("password");
        dto.setRole(Role.SUPPORTER);

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(Role.SUPPORTER);

        SupporterProfile profile = new SupporterProfile();
        profile.setUser(user);

        when(userFactory.createBaseUser(dto)).thenReturn(user);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userFactory.createSupporter(user, dto)).thenReturn(profile);

        registrationService.register(dto);

        verify(userRepository).save(user);
        verify(supporterProfileRepository).save(profile);
        verify(missionaryProfileRepository, never()).save(any());
    }

    @Test
    void registerSocial_Missionary_SavesUserAndMissionaryProfile() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("social_m@example.com");
        dto.setRole(Role.MISSIONARY);
        String oauthId = "GOOGLE:social_m@example.com";

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(Role.MISSIONARY);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setUser(user);

        InviteCode inviteCode = new InviteCode();
        inviteCode.setMissionary(profile);

        when(userFactory.createBaseUser(dto)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userFactory.createMissionary(eq(user), eq(dto), anyString())).thenReturn(profile);
        when(userFactory.createInviteCode(eq(profile), anyString())).thenReturn(inviteCode);

        registrationService.registerSocial(dto, oauthId, AuthProvider.GOOGLE);

        verify(userRepository).save(user);
        verify(missionaryProfileRepository).save(profile);
        verify(inviteCodeRepository).save(inviteCode);
        verify(supporterProfileRepository, never()).save(any());
    }

    @Test
    void registerSocial_Supporter_SavesUserAndSupporterProfile() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("social_s@example.com");
        dto.setRole(Role.SUPPORTER);
        String oauthId = "GOOGLE:social_s@example.com";

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(Role.SUPPORTER);

        SupporterProfile profile = new SupporterProfile();
        profile.setUser(user);

        when(userFactory.createBaseUser(dto)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userFactory.createSupporter(user, dto)).thenReturn(profile);

        registrationService.registerSocial(dto, oauthId, AuthProvider.GOOGLE);

        verify(userRepository).save(user);
        verify(supporterProfileRepository).save(profile);
        verify(missionaryProfileRepository, never()).save(any());
    }
}
