package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.ProfileService;
import com.shepherdsstories.services.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private SupporterProfileRepository supporterProfileRepository;

    @Mock
    private S3Service s3Service;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(userRepository, profileService, missionaryProfileRepository, supporterProfileRepository, s3Service);
    }

    @Test
    void getUploadUrl_Authenticated_Success() {
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(Role.MISSIONARY);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(s3Service.generateUploadUrl(anyString(), anyString())).thenReturn("http://s3-upload-url");

        ResponseEntity<Map<String, String>> response = controller.getUploadUrl(auth, "image/jpeg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("http://s3-upload-url", response.getBody().get("uploadUrl"));
    }

    @Test
    void getUploadUrl_Unauthenticated_Returns401() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.getUploadUrl(auth, "image/jpeg");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
