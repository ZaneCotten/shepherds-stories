package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.ConnectionRepository;
import com.shepherdsstories.data.repositories.InviteCodeRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionaryProfileControllerTest {

    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    private MissionaryProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new MissionaryProfileController(missionaryProfileRepository, userRepository, connectionRepository, inviteCodeRepository);
    }

    @Test
    void getProfile_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);
        profile.setMissionaryName("Test Missionary");
        profile.setReferenceNumber("REF1234567890ABC");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<MissionaryProfileDTO> response = controller.getProfile();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Missionary", response.getBody().getMissionaryName());
        assertEquals("REF1234567890ABC", response.getBody().getReferenceNumber());
    }

    @Test
    void getProfile_OAuth2_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);
        profile.setMissionaryName("OAuth Missionary");

        org.springframework.security.oauth2.core.user.OAuth2User oauthUser = mock(org.springframework.security.oauth2.core.user.OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(email);

        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken auth = mock(org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(oauthUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<MissionaryProfileDTO> response = controller.getProfile();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MissionaryProfileDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("OAuth Missionary", body.getMissionaryName());
    }

    @Test
    void getPendingRequests_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        SupporterProfile supporter = new SupporterProfile();
        supporter.setFirstName("John");
        supporter.setLastName("Doe");

        ConnectionRequest request = new ConnectionRequest();
        request.setId(UUID.randomUUID());
        request.setSupporter(supporter);
        request.setCreatedAt(OffsetDateTime.now());

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(connectionRepository.findByMissionaryIdAndStatus(userId, RequestStatus.PENDING)).thenReturn(List.of(request));

        ResponseEntity<List<Map<String, Object>>> response = controller.getPendingRequests();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("John Doe", body.getFirst().get("supporterName"));
    }

    @Test
    void respondToRequest_Approve_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);

        ConnectionRequest request = new ConnectionRequest();
        request.setId(requestId);
        request.setMissionary(profile);

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(connectionRepository.findById(requestId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.respondToRequest(requestId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(RequestStatus.APPROVED, request.getStatus());
        verify(connectionRepository).save(request);
    }

    @Test
    void toggleReferenceStatus_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();
        String code = "ABCDEF1234567890";

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);
        profile.setIsReferenceDisabled(false);
        profile.setReferenceNumber(code);

        InviteCode inviteCode = new InviteCode();
        inviteCode.setMissionary(profile);
        inviteCode.setCodeString(code);
        inviteCode.setIsActive(true);
        profile.setInviteCodes(List.of(inviteCode));

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        // Disable
        ResponseEntity<?> response = controller.toggleReferenceStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(profile.getIsReferenceDisabled());
        assertFalse(inviteCode.getIsActive());
        verify(missionaryProfileRepository, times(1)).save(profile);

        // Toggle back (Enable)
        controller.toggleReferenceStatus();
        assertFalse(profile.getIsReferenceDisabled());
        assertTrue(inviteCode.getIsActive());
        verify(missionaryProfileRepository, times(2)).save(profile);
    }

    @Test
    void toggleReferenceStatus_NullInitialValue_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);
        profile.setIsReferenceDisabled(null); // Explicitly null

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<?> response = controller.toggleReferenceStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, profile.getIsReferenceDisabled()); // Should become true because !null -> !false (auto-unboxed or handled)
        verify(missionaryProfileRepository).save(profile);
    }

    @Test
    void generateNewInviteCode_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        MissionaryProfile profile = new MissionaryProfile();
        profile.setId(userId);
        profile.setReferenceNumber("OLD_CODE");
        profile.setIsReferenceDisabled(true); // Start as disabled
        profile.setInviteCodes(new ArrayList<>());

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<Map<String, String>> response = controller.generateNewInviteCode();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        String newCode = body.get("newCode");
        assertNotNull(newCode);
        assertEquals(16, newCode.length());
        assertNotEquals("OLD_CODE", newCode);
        assertEquals(newCode, profile.getReferenceNumber());
        assertFalse(profile.getIsReferenceDisabled()); // Should be false (enabled) after generation

        verify(inviteCodeRepository).deleteByMissionaryId(userId);
        verify(missionaryProfileRepository).save(profile);
        verify(inviteCodeRepository).save(any(InviteCode.class));
    }

    private void setupAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}
