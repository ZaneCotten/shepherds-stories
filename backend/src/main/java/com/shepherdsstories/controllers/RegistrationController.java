package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.data.records.RegistrationRequest;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private static final String ERROR_KEY = "error";
    private final UserRepository userRepository;

    private final RegistrationService registrationService;
    private final SecurityContextRepository securityContextRepository;

    public RegistrationController(RegistrationService registrationService, UserRepository userRepository, SecurityContextRepository securityContextRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
        this.securityContextRepository = securityContextRepository;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String firstToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().split("\\s+")[0];
    }

    private static String secondToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<java.util.Map<String, Object>> register(@Valid @RequestBody RegistrationRequestDTO request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "User already exists"));
        }

        registrationService.register(request);
        authenticateUser(request.getEmail(), request.getRole().name(), httpRequest, httpResponse);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow();
        return ResponseEntity.ok(java.util.Map.of(
                "message", "User registered successfully",
                "id", user.getId(),
                "username", user.getEmail(),
                "role", user.getRole().name()
        ));
    }

    private void authenticateUser(String email, String role, HttpServletRequest request, HttpServletResponse response) {
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken token =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        email, null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    @PostMapping("/register-social")
    @Transactional
    public ResponseEntity<java.util.Map<String, Object>> registerSocialUser(@RequestBody RegistrationRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = normalizeEmail(request.email());
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "Email is required"));
        }

        if (request.role() == null || request.role().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "Role is required"));
        }

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "Invalid role"));
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "User already exists"));
        }

        if (request.authProvider() == null || request.authProvider().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "OAuth provider is required"));
        }
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(request.authProvider().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body(java.util.Map.of(ERROR_KEY, "Invalid OAuth provider"));
        }
        String oauthId = provider.name() + ":" + email;

        String fullName = firstNonBlank(
                request.displayName(),
                email.contains("@") ? email.substring(0, email.indexOf('@')) : "Missionary"
        );

        String givenName = firstNonBlank(request.firstName(), firstToken(fullName), "Supporter");
        String familyName = firstNonBlank(request.lastName(), secondToken(fullName), "Account");

        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail(email);
        dto.setRole(role);
        dto.setDisplayName(fullName);
        dto.setFirstName(givenName);
        dto.setLastName(familyName);
        dto.setProfilePictureUrl(request.profilePictureUrl());

        registrationService.registerSocial(dto, oauthId, provider);
        authenticateUser(email, role.name(), httpRequest, httpResponse);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return ResponseEntity.ok(java.util.Map.of(
                "message", "User registered successfully",
                "id", user.getId(),
                "username", user.getEmail(),
                "role", user.getRole().name()
        ));
    }
}
