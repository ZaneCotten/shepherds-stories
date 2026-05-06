package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.PasswordChangeRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    private UserController userController;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userController = new UserController(userRepository, passwordEncoder, auditLogService);
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedOldPassword");

        UserAuthConfig.AppUserDetails details = new UserAuthConfig.AppUserDetails(
                userId,
                "test@example.com",
                "password",
                true,
                Collections.singletonList(new SimpleGrantedAuthority("MISSIONARY"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void changePassword_Success() {
        PasswordChangeRequestDTO request = PasswordChangeRequestDTO.builder()
                .currentPassword("oldPassword")
                .newPassword("NewPass123!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashedNewPassword");

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ResponseEntity<Map<String, String>> response = userController.changePassword(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", Objects.requireNonNull(response.getBody()).get("message"));

        verify(userRepository).save(testUser);
        verify(auditLogService).log(eq("PASSWORD_CHANGE"), eq("test@example.com"), eq(userId), anyString(), any());
    }

    @Test
    void changePassword_InvalidCurrentPassword() {
        PasswordChangeRequestDTO request = PasswordChangeRequestDTO.builder()
                .currentPassword("wrongPassword")
                .newPassword("NewPass123!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedOldPassword")).thenReturn(false);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ResponseEntity<Map<String, String>> response = userController.changePassword(request, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid current password", Objects.requireNonNull(response.getBody()).get("error"));

        verify(userRepository, never()).save(any());
        verify(auditLogService).log(eq("PASSWORD_CHANGE_FAILED"), eq("test@example.com"), eq(userId), anyString(), any());
    }

    @Test
    void changePassword_UserNotFound() {
        PasswordChangeRequestDTO request = PasswordChangeRequestDTO.builder()
                .currentPassword("oldPassword")
                .newPassword("NewPass123!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        // This should throw ResourceNotFoundException
        try {
            userController.changePassword(request, httpRequest);
        } catch (com.shepherdsstories.exceptions.ResourceNotFoundException e) {
            assertEquals("User not found by ID: " + userId, e.getMessage());
        }

        verify(userRepository, never()).save(any());
    }
}
