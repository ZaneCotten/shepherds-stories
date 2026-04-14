package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.records.RegistrationRequest;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private SecurityContextRepository securityContextRepository;

    private RegistrationController registrationController;

    @BeforeEach
    void setUp() {
        registrationController = new RegistrationController(registrationService, userRepository, securityContextRepository);
    }

    @Test
    void registerSocialUser_Success() {
        RegistrationRequest request = new RegistrationRequest(
                "social@example.com",
                "MISSIONARY",
                "GOOGLE",
                "Social User",
                "Social",
                "User"
        );

        when(userRepository.findByEmailIgnoreCase("social@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(registrationService).registerSocial(any(), eq("GOOGLE:social@example.com"), eq(AuthProvider.GOOGLE));
    }

    @Test
    void registerSocialUser_Supporter_Success() {
        RegistrationRequest request = new RegistrationRequest(
                "supporter_social@example.com",
                "SUPPORTER",
                "GOOGLE",
                "Supporter User",
                "Supporter",
                "User"
        );

        when(userRepository.findByEmailIgnoreCase("supporter_social@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(registrationService).registerSocial(any(), eq("GOOGLE:supporter_social@example.com"), eq(AuthProvider.GOOGLE));
    }

    @Test
    void registerSocialUser_MissingEmail_ReturnsBadRequest() {
        RegistrationRequest request = new RegistrationRequest(
                "",
                "MISSIONARY",
                "GOOGLE",
                "Social User",
                "Social",
                "User"
        );

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email is required", response.getBody());
    }

    @Test
    void registerSocialUser_UserAlreadyExists_ReturnsBadRequest() {
        RegistrationRequest request = new RegistrationRequest(
                "existing@example.com",
                "MISSIONARY",
                "GOOGLE",
                "Social User",
                "Social",
                "User"
        );

        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(new User()));

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exists", response.getBody());
    }

    @Test
    void registerSocialUser_InvalidRole_ReturnsBadRequest() {
        RegistrationRequest request = new RegistrationRequest(
                "social@example.com",
                "INVALID_ROLE",
                "GOOGLE",
                "Social User",
                "Social",
                "User"
        );

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid role", response.getBody());
    }

    @Test
    void registerSocialUser_InvalidProvider_ReturnsBadRequest() {
        RegistrationRequest request = new RegistrationRequest(
                "social@example.com",
                "MISSIONARY",
                "INVALID_PROVIDER",
                "Social User",
                "Social",
                "User"
        );

        when(userRepository.findByEmailIgnoreCase("social@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = registrationController.registerSocialUser(request, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid OAuth provider", response.getBody());
    }

    @Test
    void register_Success() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setRole(Role.MISSIONARY);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = registrationController.register(dto, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(registrationService).register(dto);
    }

    @Test
    void register_Supporter_Success() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("supporter@example.com");
        dto.setPassword("password123");
        dto.setRole(Role.SUPPORTER);

        when(userRepository.findByEmailIgnoreCase("supporter@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = registrationController.register(dto, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(registrationService).register(dto);
    }

    @Test
    void register_UserAlreadyExists_ReturnsBadRequest() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("existing@example.com");
        dto.setPassword("password123");
        dto.setRole(Role.MISSIONARY);

        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(new User()));

        ResponseEntity<String> response = registrationController.register(dto, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exists", response.getBody());
        verify(registrationService, never()).register(any());
    }
}
