package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.factories.UserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/supporter")
public class SupporterController {

    private static final String MESSAGE_KEY = "message";

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final UserFactory userFactory;

    public SupporterController(MissionaryProfileRepository missionaryProfileRepository,
                               InviteCodeRepository inviteCodeRepository,
                               SupporterProfileRepository supporterProfileRepository,
                               ConnectionRepository connectionRepository,
                               UserRepository userRepository,
                               UserFactory userFactory) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.userFactory = userFactory;
    }

    @PostMapping("/send-request")
    public ResponseEntity<Map<String, String>> sendRequest(@RequestParam String code, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = getCurrentUser(authentication);
        Optional<SupporterProfile> supporterOpt = supporterProfileRepository.findById(user.getId());

        SupporterProfile supporter;
        if (supporterOpt.isEmpty()) {
            // Missionary acting as a supporter, or any user without a supporter profile
            supporter = userFactory.createDefaultSupporter(user);
            supporter = supporterProfileRepository.save(supporter);
        } else {
            supporter = supporterOpt.get();
        }

        // Find missionary
        String trimmedCode = code.trim();
        Optional<MissionaryProfile> profileOpt = missionaryProfileRepository.findByReferenceNumberIgnoreCase(trimmedCode);
        if (profileOpt.isEmpty()) {
            profileOpt = inviteCodeRepository.findByCodeStringIgnoreCase(trimmedCode)
                    .filter(InviteCode::getIsActive)
                    .map(InviteCode::getMissionary);
        }

        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, "Missionary not found with given invite code."));
        }

        MissionaryProfile missionary = profileOpt.get();
        if (missionary.getIsReferenceDisabled() != null && missionary.getIsReferenceDisabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, "Missionary has disabled invitations."));
        }

        // Check if user is following themselves
        if (missionary.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "You cannot follow yourself."));
        }

        // Check for existing request
        if (connectionRepository.existsByMissionaryIdAndSupporterId(missionary.getId(), supporter.getId())) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Request already exists or already connected"));
        }

        // Create connection request
        ConnectionRequest request = new ConnectionRequest();
        request.setMissionary(missionary);
        request.setSupporter(supporter);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(OffsetDateTime.now());

        connectionRepository.save(request);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Request sent!"));
    }

    private User getCurrentUser(org.springframework.security.core.Authentication authentication) {
        String principalName = authentication.getName();
        String email = null;

        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken) {
            org.springframework.security.oauth2.core.user.OAuth2User principal = authToken.getPrincipal();
            if (principal != null) {
                Object emailAttr = principal.getAttribute("email");
                email = emailAttr != null ? emailAttr.toString() : null;
            }
        } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
            Object emailAttr = oauthUser.getAttribute("email");
            email = emailAttr != null ? emailAttr.toString() : null;
        }

        if (email != null) {
            String finalEmail = email.trim().toLowerCase();
            return userRepository.findByEmailIgnoreCase(finalEmail)
                    .or(() -> userRepository.findByOauthId("GOOGLE:" + finalEmail))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found by email: " + finalEmail));
        }

        return userRepository.findByEmailIgnoreCase(principalName)
                .or(() -> userRepository.findByOauthId(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("User not found by principal: " + principalName));
    }
}
