package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.data.records.RegistrationRequest;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.services.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final UserRepository userRepository;

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService, UserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        registrationService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/register-social")
    public ResponseEntity<String> registerSocialUser(@RequestBody RegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        if (request.role() == null || request.role().isBlank()) {
            return ResponseEntity.badRequest().body("Role is required");
        }

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body("Invalid role");
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        if (request.authProvider() == null || request.authProvider().isBlank()) {
            return ResponseEntity.badRequest().body("OAuth provider is required");
        }
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(request.authProvider().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.badRequest().body("Invalid OAuth provider");
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

        registrationService.registerSocial(dto, oauthId, provider);

        return ResponseEntity.ok("User registered successfully");
    }
}
