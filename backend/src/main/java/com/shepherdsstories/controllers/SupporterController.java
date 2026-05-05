package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.dtos.PrayerRequestDTO;
import com.shepherdsstories.dtos.SupporterProfileDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.factories.UserFactory;
import com.shepherdsstories.services.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supporter")
public class SupporterController {

    private static final String MESSAGE_KEY = "message";

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final ConnectionRepository connectionRepository;
    private final PrayerRequestRepository prayerRequestRepository;
    private final UserRepository userRepository;
    private final UserFactory userFactory;

    @org.springframework.beans.factory.annotation.Autowired
    private S3Service s3Service;

    public SupporterController(MissionaryProfileRepository missionaryProfileRepository,
                               InviteCodeRepository inviteCodeRepository,
                               SupporterProfileRepository supporterProfileRepository,
                               ConnectionRepository connectionRepository,
                               PrayerRequestRepository prayerRequestRepository,
                               UserRepository userRepository,
                               UserFactory userFactory) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.connectionRepository = connectionRepository;
        this.prayerRequestRepository = prayerRequestRepository;
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

    @GetMapping("/missionaries")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MissionaryProfileDTO>> getConnectedMissionaries(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = getCurrentUser(authentication);
        List<ConnectionRequest> connections = connectionRepository.findBySupporterIdAndStatus(user.getId(), RequestStatus.APPROVED);

        List<MissionaryProfileDTO> missionaryDTOs = connections.stream()
                .map(ConnectionRequest::getMissionary)
                .map(m -> MissionaryProfileDTO.builder()
                        .id(m.getId())
                        .missionaryName(m.getMissionaryName())
                        .locationRegion(m.getLocationRegion())
                        .biography(m.getBiography())
                        .profilePictureUrl(s3Service.generatePresignedUrl(m.getUser().getProfilePictureKey()))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(missionaryDTOs);
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<SupporterProfileDTO> getProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = getCurrentUser(authentication);
        SupporterProfile supporter = getOrCreateSupporterProfile(user);

        SupporterProfileDTO dto = SupporterProfileDTO.builder()
                .id(supporter.getId())
                .firstName(supporter.getFirstName())
                .lastName(supporter.getLastName())
                .profilePictureUrl(s3Service.generatePresignedUrl(user.getProfilePictureKey()))
                .build();

        return ResponseEntity.ok(dto);
    }

    private ResponseEntity<Map<String, String>> processExistingRequest(ConnectionRequest existingRequest) {
        RequestStatus status = existingRequest.getStatus();
        if (status == RequestStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Already connected"));
        } else if (status == RequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Request already pending"));
        } else if (status == RequestStatus.REJECTED) {
            return handleRejectedRequest(existingRequest);
        } else if (status == RequestStatus.BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(MESSAGE_KEY, "You are banned from connecting with this missionary."));
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

    @PostMapping("/missionaries/{missionaryId}/remove")
    @Transactional
    public ResponseEntity<Map<String, String>> removeMissionary(@PathVariable java.util.UUID missionaryId, org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        ConnectionRequest connection = connectionRepository.findByMissionaryIdAndSupporterId(missionaryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        if (connection.getStatus() != RequestStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "You are not currently connected to this missionary."));
        }

        connection.setStatus(RequestStatus.REJECTED);
        connection.setProcessedAt(OffsetDateTime.now().minusMinutes(2)); // Allow immediate reconnection
        connectionRepository.save(connection);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Missionary removed"));
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

    @PostMapping("/profile/picture/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(@RequestParam String contentType, org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        String s3Key = "profiles/" + user.getId() + "/" + java.util.UUID.randomUUID();
        String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);
        return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "s3Key", s3Key));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<SupporterProfileDTO> updateProfile(@RequestBody SupporterProfileDTO updateDto, org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        SupporterProfile profile = supporterProfileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supporter profile not found"));

        if (updateDto.getFirstName() != null && !updateDto.getFirstName().isBlank()) {
            profile.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null && !updateDto.getLastName().isBlank()) {
            profile.setLastName(updateDto.getLastName());
        }

        if (updateDto.getProfilePictureUrl() != null) {
            user.setProfilePictureKey(updateDto.getProfilePictureUrl());
            userRepository.save(user);
        }

        supporterProfileRepository.save(profile);

        return ResponseEntity.ok(SupporterProfileDTO.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .profilePictureUrl(s3Service.generatePresignedUrl(user.getProfilePictureKey()))
                .build());
    }

    @GetMapping("/prayer-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PrayerRequestDTO>> getPrayerRequests(org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<ConnectionRequest> connections = connectionRepository.findBySupporterIdAndStatus(user.getId(), RequestStatus.APPROVED);

        List<java.util.UUID> missionaryIds = connections.stream()
                .map(req -> req.getMissionary().getId())
                .collect(Collectors.toList());

        List<PrayerRequest> requests = prayerRequestRepository.findAllByMissionaryIdInAndAnsweredFalseOrderByCreatedAtDesc(missionaryIds);

        return ResponseEntity.ok(requests.stream().map(this::toPrayerRequestDTO).collect(Collectors.toList()));
    }

    private PrayerRequestDTO toPrayerRequestDTO(PrayerRequest request) {
        return PrayerRequestDTO.builder()
                .id(request.getId())
                .title(request.getTitle())
                .content(request.getContent())
                .answered(request.isAnswered())
                .createdAt(request.getCreatedAt())
                .missionaryId(request.getMissionary().getId())
                .missionaryName(request.getMissionary().getMissionaryName())
                .build();
    }
}
