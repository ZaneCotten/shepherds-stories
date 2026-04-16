package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.factories.UserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    @Transactional
    public ResponseEntity<Map<String, String>> sendRequest(@RequestParam String code, org.springframework.security.core.Authentication authentication) {
        try {
            return processSendRequest(code, authentication);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> processSendRequest(String code, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = getCurrentUser(authentication);
        SupporterProfile supporter = getOrCreateSupporterProfile(user);

        MissionaryProfile missionary = findMissionaryByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Missionary not found with given invite code."));

        if (Boolean.TRUE.equals(missionary.getIsReferenceDisabled())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, "Missionary has disabled invitations."));
        }

        if (missionary.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "You cannot follow yourself."));
        }

        return handleExistingOrNewRequest(missionary, supporter);
    }

    private SupporterProfile getOrCreateSupporterProfile(User user) {
        return supporterProfileRepository.findById(user.getId())
                .orElseGet(() -> {
                    SupporterProfile newSupporter = userFactory.createDefaultSupporter(user);
                    return supporterProfileRepository.save(newSupporter);
                });
    }

    private Optional<MissionaryProfile> findMissionaryByCode(String code) {
        String trimmedCode = code.trim();
        Optional<MissionaryProfile> profileOpt = missionaryProfileRepository.findByReferenceNumberIgnoreCase(trimmedCode);
        if (profileOpt.isEmpty()) {
            profileOpt = inviteCodeRepository.findByCodeStringIgnoreCase(trimmedCode)
                    .filter(InviteCode::getIsActive)
                    .map(InviteCode::getMissionary);
        }
        return profileOpt;
    }

    private ResponseEntity<Map<String, String>> handleExistingOrNewRequest(MissionaryProfile missionary, SupporterProfile supporter) {
        Optional<ConnectionRequest> existingRequestOpt = connectionRepository.findByMissionaryIdAndSupporterId(missionary.getId(), supporter.getId());
        if (existingRequestOpt.isPresent()) {
            return processExistingRequest(existingRequestOpt.get());
        }

        ConnectionRequest request = new ConnectionRequest();
        request.setMissionary(missionary);
        request.setSupporter(supporter);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(OffsetDateTime.now());
        connectionRepository.save(request);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Request sent!"));
    }

    private ResponseEntity<Map<String, String>> processExistingRequest(ConnectionRequest existingRequest) {
        RequestStatus status = existingRequest.getStatus();
        if (status == RequestStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Already connected"));
        } else if (status == RequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Request already pending"));
        } else if (status == RequestStatus.REJECTED) {
            return handleRejectedRequest(existingRequest);
        } else {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Cannot send request at this time."));
        }
    }

    private ResponseEntity<Map<String, String>> handleRejectedRequest(ConnectionRequest existingRequest) {
        OffsetDateTime processedAt = existingRequest.getProcessedAt();
        if (processedAt != null && processedAt.isAfter(OffsetDateTime.now().minusMinutes(1))) {
            long secondsLeft = 60 - java.time.Duration.between(processedAt, OffsetDateTime.now()).getSeconds();
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Request was recently rejected. Please wait " + secondsLeft + " seconds before trying again."));
        }
        existingRequest.setStatus(RequestStatus.PENDING);
        existingRequest.setCreatedAt(OffsetDateTime.now());
        existingRequest.setProcessedAt(null);
        connectionRepository.save(existingRequest);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Request sent!"));
    }

    private User getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.shepherdsstories.exceptions.UnauthenticatedException("Unauthenticated");
        }

        if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
            return userRepository.findById(details.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found by ID: " + details.getId()));
        }

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
