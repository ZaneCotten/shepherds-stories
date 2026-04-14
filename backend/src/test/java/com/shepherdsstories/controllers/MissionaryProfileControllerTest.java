package com.shepherdsstories.controllers;

import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionaryProfileControllerTest {

    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private UserRepository userRepository;

    private MissionaryProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new MissionaryProfileController(missionaryProfileRepository, userRepository);
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
    void getProfile_Unauthenticated_ReturnsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseEntity<MissionaryProfileDTO> response = controller.getProfile();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
