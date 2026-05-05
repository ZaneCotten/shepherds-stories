package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.services.S3Service;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionaryProfileControllerTest {

    @Mock
    private com.shepherdsstories.data.repositories.MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private com.shepherdsstories.data.repositories.UserRepository userRepository;

    @Mock
    private com.shepherdsstories.data.repositories.ConnectionRepository connectionRepository;

    @Mock
    private com.shepherdsstories.data.repositories.InviteCodeRepository inviteCodeRepository;

    @Mock
    private com.shepherdsstories.data.repositories.SupporterProfileRepository supporterProfileRepository;

    @Mock
    private com.shepherdsstories.data.repositories.PrayerRequestRepository prayerRequestRepository;

    @Mock
    private com.shepherdsstories.data.repositories.PostRepository postRepository;

    @Mock
    private com.shepherdsstories.data.repositories.CommentRepository commentRepository;

    @Mock
    private com.shepherdsstories.data.repositories.PostLikeRepository postLikeRepository;

    @Mock
    private com.shepherdsstories.data.repositories.CommentLikeRepository commentLikeRepository;

    @Mock
    private S3Service s3Service;

    @org.mockito.InjectMocks
    private MissionaryProfileController controller;

    @BeforeEach
    void setUp() {
        // Mockito handles constructor injection, but we need to manually set field-injected dependencies
        ReflectionTestUtils.setField(controller, "commentRepository", commentRepository);
        ReflectionTestUtils.setField(controller, "postLikeRepository", postLikeRepository);
        ReflectionTestUtils.setField(controller, "commentLikeRepository", commentLikeRepository);
        ReflectionTestUtils.setField(controller, "s3Service", s3Service);
    }

    @Test
    void getProfile_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
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

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
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

        com.shepherdsstories.entities.SupporterProfile supporter = new com.shepherdsstories.entities.SupporterProfile();
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

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
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

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
        profile.setId(userId);
        profile.setIsReferenceDisabled(false);
        profile.setReferenceNumber(code);

        com.shepherdsstories.entities.InviteCode inviteCode = new com.shepherdsstories.entities.InviteCode();
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

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
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

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
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
        verify(inviteCodeRepository).save(any(com.shepherdsstories.entities.InviteCode.class));
    }

    @Test
    void updateProfile_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();
        String newName = "New Missionary Name";

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        com.shepherdsstories.entities.MissionaryProfile profile = new com.shepherdsstories.entities.MissionaryProfile();
        profile.setId(userId);
        profile.setMissionaryName("Old Name");

        MissionaryProfileDTO updateDto = new MissionaryProfileDTO();
        updateDto.setMissionaryName(newName);

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(missionaryProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<MissionaryProfileDTO> response = controller.updateProfile(updateDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(newName, response.getBody().getMissionaryName());
        assertEquals(newName, profile.getMissionaryName());
        verify(missionaryProfileRepository).save(profile);
    }

    @Test
    void exportCsv_Success() throws Exception {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);
        java.io.PrintWriter writer = mock(java.io.PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        controller.exportCsv(null, null, true, true, true, true, true, false, true, true, response);

        verify(response).setContentType("text/csv");
        verify(response).setHeader(eq("Content-Disposition"), contains("attachment; filename=missionary_data_export.csv"));
        verify(writer).println(contains("Type,Date,Title/Name,Content/Email,Status/Other,Likes"));

        verify(connectionRepository).findByMissionaryIdAndStatusAndCreatedAtBetween(eq(userId), eq(RequestStatus.APPROVED), any(), any());
        verify(prayerRequestRepository).findAllByMissionaryIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(userId), any(), any());
        verify(postRepository).findAllByAuthorIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(userId), any(), any());

        verify(connectionRepository).countByMissionaryIdAndStatusAndCreatedAtBetween(eq(userId), eq(RequestStatus.APPROVED), any(), any());
        verify(prayerRequestRepository).countByMissionaryIdAndCreatedAtBetween(eq(userId), any(), any());
        verify(prayerRequestRepository).countByMissionaryIdAndAnsweredTrueAndCreatedAtBetween(eq(userId), any(), any());
    }

    @Test
    void banAndUnbanSupporter_Success() {
        UUID missionaryId = UUID.randomUUID();
        UUID supporterId = UUID.randomUUID();
        String email = "missionary@example.com";
        setupAuth(email);

        User missionary = new User();
        missionary.setId(missionaryId);
        missionary.setEmail(email);

        User supporterUser = new User();
        supporterUser.setId(supporterId);

        SupporterProfile supporterProfile = new SupporterProfile();
        supporterProfile.setId(supporterId);
        supporterProfile.setUser(supporterUser);
        supporterProfile.setFirstName("John");
        supporterProfile.setLastName("Doe");

        ConnectionRequest connection = new ConnectionRequest();
        connection.setMissionary(new MissionaryProfile());
        connection.getMissionary().setId(missionaryId);
        connection.setSupporter(supporterProfile);
        connection.setStatus(RequestStatus.APPROVED);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(missionary));
        when(connectionRepository.findByMissionaryIdAndSupporterId(missionaryId, supporterId)).thenReturn(Optional.of(connection));
        when(connectionRepository.findByMissionaryIdAndStatus(missionaryId, RequestStatus.BANNED)).thenReturn(List.of(connection));

        // Ban
        ResponseEntity<Map<String, String>> banResponse = controller.banSupporter(supporterId);
        assertEquals(HttpStatus.OK, banResponse.getStatusCode());
        assertEquals("Supporter banned", banResponse.getBody().get("message"));
        assertEquals(RequestStatus.BANNED, connection.getStatus());

        // Get Banned
        ResponseEntity<List<com.shepherdsstories.dtos.BannedSupporterDTO>> bannedListResponse = controller.getBannedSupporters();
        assertEquals(HttpStatus.OK, bannedListResponse.getStatusCode());
        assertEquals(1, bannedListResponse.getBody().size());
        assertEquals("John", bannedListResponse.getBody().get(0).getFirstName());

        // Unban
        ResponseEntity<Map<String, String>> unbanResponse = controller.unbanSupporter(supporterId);
        assertEquals(HttpStatus.OK, unbanResponse.getStatusCode());
        assertEquals("Supporter unbanned", unbanResponse.getBody().get("message"));
        assertEquals(RequestStatus.REJECTED, connection.getStatus());
    }

    @Test
    void unbanSupporter_NotBanned_Error() {
        UUID missionaryId = UUID.randomUUID();
        UUID supporterId = UUID.randomUUID();
        String email = "missionary@example.com";
        setupAuth(email);

        User missionary = new User();
        missionary.setId(missionaryId);
        missionary.setEmail(email);

        ConnectionRequest connection = new ConnectionRequest();
        connection.setMissionary(new MissionaryProfile());
        connection.getMissionary().setId(missionaryId);
        connection.setStatus(RequestStatus.APPROVED);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(missionary));
        when(connectionRepository.findByMissionaryIdAndSupporterId(missionaryId, supporterId)).thenReturn(Optional.of(connection));

        ResponseEntity<Map<String, String>> response = controller.unbanSupporter(supporterId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Supporter is not banned.", response.getBody().get("message"));
    }

    @Test
    void removeSupporter_Success() {
        String email = "missionary@example.com";
        UUID userId = UUID.randomUUID();
        UUID supporterId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        ConnectionRequest connection = new ConnectionRequest();
        connection.setStatus(RequestStatus.APPROVED);

        setupAuth(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(connectionRepository.findByMissionaryIdAndSupporterId(userId, supporterId)).thenReturn(Optional.of(connection));

        ResponseEntity<Map<String, String>> response = controller.removeSupporter(supporterId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Supporter removed", response.getBody().get("message"));
        assertEquals(RequestStatus.REJECTED, connection.getStatus());
        assertNotNull(connection.getProcessedAt());
        assertTrue(connection.getProcessedAt().isBefore(OffsetDateTime.now().minusMinutes(1)));
        verify(connectionRepository).save(connection);
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
