package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.PasswordChangeRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import com.shepherdsstories.services.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody PasswordChangeRequestDTO request, HttpServletRequest httpRequest) {
        User user = getCurrentUser();

        if (user.getPasswordHash() == null) {
            // User likely registered via OAuth2 and doesn't have a password set yet
            // Or we could allow setting a password if they don't have one
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot change password for social login users without a local password."));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditLogService.log("PASSWORD_CHANGE_FAILED", user.getEmail(), user.getId(), "Invalid current password", httpRequest.getRemoteAddr());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid current password"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log("PASSWORD_CHANGE", user.getEmail(), user.getId(), "Success", httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException("Unauthenticated");
        }

        if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
            return userRepository.findById(details.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found by ID: " + details.getId()));
        }

        // Handle OAuth2 users if necessary, though they might not have a password to change
        String principalName = authentication.getName();
        return userRepository.findByEmailIgnoreCase(principalName)
                .or(() -> userRepository.findByOauthId(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("User not found by principal: " + principalName));
    }
}
