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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        String email = "social@example.com";
        RegistrationRequest request = new RegistrationRequest(
                email,
                "MISSIONARY",
                "GOOGLE",
                "Social User",
                "Social",
                "User"
        );

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(email);
        savedUser.setRole(Role.MISSIONARY);

        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.empty()) // Before register
                .thenReturn(Optional.of(savedUser)); // After register

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User registered successfully", body.get("message"));
        assertEquals(savedUser.getId(), body.get("id"));
        verify(registrationService).registerSocial(any(), eq("GOOGLE:social@example.com"), eq(AuthProvider.GOOGLE));
    }

    @Test
    void registerSocialUser_Supporter_Success() {
        String email = "supporter_social@example.com";
        RegistrationRequest request = new RegistrationRequest(
                email,
                "SUPPORTER",
                "GOOGLE",
                "Supporter User",
                "Supporter",
                "User"
        );

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(email);
        savedUser.setRole(Role.SUPPORTER);

        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedUser));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User registered successfully", body.get("message"));
        assertEquals(savedUser.getId(), body.get("id"));
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

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("Email is required", body.get("error"));
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

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User already exists", body.get("error"));
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

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("Invalid role", body.get("error"));
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

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.registerSocialUser(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("Invalid OAuth provider", body.get("error"));
    }

    @Test
    void register_Success() {
        String email = "test@example.com";
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail(email);
        dto.setPassword("password123");
        dto.setRole(Role.MISSIONARY);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(email);
        savedUser.setRole(Role.MISSIONARY);

        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedUser));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.register(dto, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User registered successfully", body.get("message"));
        assertEquals(savedUser.getId(), body.get("id"));
        verify(registrationService).register(dto);
    }

    @Test
    void register_Supporter_Success() {
        String email = "supporter@example.com";
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail(email);
        dto.setPassword("password123");
        dto.setRole(Role.SUPPORTER);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(email);
        savedUser.setRole(Role.SUPPORTER);

        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedUser));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.register(dto, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User registered successfully", body.get("message"));
        assertEquals(savedUser.getId(), body.get("id"));
        verify(registrationService).register(dto);
    }

    @Test
    void register_UserAlreadyExists_ReturnsBadRequest() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("existing@example.com");
        dto.setPassword("password123");
        dto.setRole(Role.MISSIONARY);

        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(new User()));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<Map<String, Object>> response = registrationController.register(dto, httpRequest, httpResponse);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assert body != null;
        assertEquals("User already exists", body.get("error"));
        verify(registrationService, never()).register(any());
    }
}
