package com.shepherdsstories.controllers;

import com.shepherdsstories.data.repositories.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupporterControllerTest {

    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @Mock
    private SupporterProfileRepository supporterProfileRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFactory userFactory;

    private SupporterController controller;

    @BeforeEach
    void setUp() {
        controller = new SupporterController(missionaryProfileRepository, inviteCodeRepository, supporterProfileRepository, connectionRepository, userRepository, userFactory);
    }

    @Test
    void sendRequest_ByReferenceNumber_Success() {
        String code = "REF1234567890ABC";
        String email = "supporter@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        User missionaryUser = new User();
        missionaryUser.setId(UUID.randomUUID());

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(missionaryUser);

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.of(profile));
        when(connectionRepository.existsByMissionaryIdAndSupporterId(profile.getId(), supporter.getId())).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Request sent!", body.get("message"));
        verify(connectionRepository).save(any());
    }

    @Test
    void sendRequest_ByInviteCode_Success() {
        String code = "INVITE123";
        String email = "supporter@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        User missionaryUser = new User();
        missionaryUser.setId(UUID.randomUUID());

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(missionaryUser);

        InviteCode inviteCode = new InviteCode();
        inviteCode.setCodeString(code);
        inviteCode.setIsActive(true);
        inviteCode.setMissionary(profile);

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.empty());
        when(inviteCodeRepository.findByCodeStringIgnoreCase(code)).thenReturn(Optional.of(inviteCode));
        when(connectionRepository.existsByMissionaryIdAndSupporterId(profile.getId(), supporter.getId())).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Request sent!", body.get("message"));
    }

    @Test
    void sendRequest_MissionaryRequester_CreatesDefaultProfile() {
        String code = "REF123";
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        User missionaryUser = new User();
        missionaryUser.setId(UUID.randomUUID());

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(missionaryUser);

        SupporterProfile newSupporter = new SupporterProfile();
        newSupporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        // No existing supporter profile
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.empty());
        when(userFactory.createDefaultSupporter(user)).thenReturn(newSupporter);
        when(supporterProfileRepository.save(newSupporter)).thenReturn(newSupporter);

        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.of(profile));
        when(connectionRepository.existsByMissionaryIdAndSupporterId(profile.getId(), newSupporter.getId())).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(supporterProfileRepository).save(newSupporter);
        verify(connectionRepository).save(any());
    }

    @Test
    void sendRequest_SelfFollow_ReturnsBadRequest() {
        String code = "SELF";
        String email = "user@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user); // Same user

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.of(profile));

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("You cannot follow yourself.", body.get("message"));
    }

    @Test
    void sendRequest_NotFound_Returns404() {
        String code = "INVALID";
        String email = "supporter@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.empty());
        when(inviteCodeRepository.findByCodeStringIgnoreCase(code)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void sendRequest_CaseInsensitive_Success() {
        String code = "ref123";
        String email = "supporter@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        User missionaryUser = new User();
        missionaryUser.setId(UUID.randomUUID());

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(missionaryUser);

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.of(profile));
        when(connectionRepository.existsByMissionaryIdAndSupporterId(profile.getId(), supporter.getId())).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(missionaryProfileRepository).findByReferenceNumberIgnoreCase(code);
    }

    @Test
    void sendRequest_ReferenceDisabled_Returns404() {
        String code = "DISABLED";
        String email = "supporter@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(UUID.randomUUID());
        profile.setIsReferenceDisabled(true);
        profile.setUser(new User());

        SupporterProfile supporter = new SupporterProfile();
        supporter.setId(userId);

        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findById(userId)).thenReturn(Optional.of(supporter));
        when(missionaryProfileRepository.findByReferenceNumberIgnoreCase(code)).thenReturn(Optional.of(profile));

        ResponseEntity<Map<String, String>> response = controller.sendRequest(code, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Missionary has disabled invitations.", body.get("message"));
    }
}
