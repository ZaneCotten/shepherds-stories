package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.data.records.RegistrationRequest;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        registrationService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/register-social")
    public ResponseEntity<?> registerSocialUser(@RequestBody RegistrationRequest request) {
        // Check if user already exists (extra safety)
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        // Map the DTO to your User Entity
        User newUser = new User();
        newUser.setEmail(request.email());
        newUser.setRole(Role.valueOf(request.role()));
        newUser.setAuthProvider(AuthProvider.valueOf(request.authProvider()));
        newUser.setIsLocked(false);
        // Password hash remains null for social users

        // Save to PostgreSQL
        userRepository.save(newUser);

        return ResponseEntity.ok("User registered successfully");
    }
}
