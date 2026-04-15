package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.ConnectionRepository;
import com.shepherdsstories.data.repositories.InviteCodeRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.ConnectionRequest;
import com.shepherdsstories.entities.InviteCode;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import com.shepherdsstories.utils.CodeGenerator;
import com.shepherdsstories.utils.ValidationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missionary")
public class MissionaryProfileController {
    private static final Logger logger = LoggerFactory.getLogger(MissionaryProfileController.class);
    private static final String MISSIONARY_PROFILE_NOT_FOUND = "Missionary profile not found";
    private static final String MESSAGE_KEY = "message";

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final InviteCodeRepository inviteCodeRepository;

    public MissionaryProfileController(MissionaryProfileRepository missionaryProfileRepository,
                                       UserRepository userRepository,
                                       ConnectionRepository connectionRepository,
                                       InviteCodeRepository inviteCodeRepository) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
        this.inviteCodeRepository = inviteCodeRepository;
    }

    @GetMapping("/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests() {
        try {
            User user = getCurrentUser();
            List<ConnectionRequest> requests = connectionRepository.findByMissionaryIdAndStatus(user.getId(), RequestStatus.PENDING);

            List<Map<String, Object>> response = requests.stream()
                    .map(req -> {
                        String supporterName = "Unknown Supporter";
                        if (req.getSupporter() != null) {
                            supporterName = req.getSupporter().getFirstName() + " " + req.getSupporter().getLastName();
                        }
                        return Map.of(
                                "id", req.getId(),
                                "supporterName", supporterName,
                                "createdAt", (Object) (req.getCreatedAt() != null ? req.getCreatedAt().toString() : "")
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching pending requests", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/requests/{requestId}/respond")
    @Transactional
    public ResponseEntity<Map<String, String>> respondToRequest(@PathVariable java.util.UUID requestId, @RequestParam boolean approve) {
        User user = getCurrentUser();
        ConnectionRequest request = connectionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getMissionary().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        request.setStatus(approve ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        request.setProcessedAt(OffsetDateTime.now());
        connectionRepository.save(request);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, approve ? "Approved" : "Denied"));
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

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<MissionaryProfileDTO> getProfile() {
        try {
            User user = getCurrentUser();

            MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(MISSIONARY_PROFILE_NOT_FOUND));

            MissionaryProfileDTO dto = new MissionaryProfileDTO();
            dto.setMissionaryName(profile.getMissionaryName());
            dto.setLocationRegion(profile.getLocationRegion());
            dto.setBiography(profile.getBiography());
            dto.setReferenceNumber(profile.getReferenceNumber());
            dto.setIsReferenceDisabled(profile.getIsReferenceDisabled());

            // Populate UserProfileDTO fields
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().name());
            dto.setDisplayName(profile.getMissionaryName());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching missionary profile", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/profile/toggle-reference")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleReferenceStatus() {
        try {
            User user = getCurrentUser();
            MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(MISSIONARY_PROFILE_NOT_FOUND));

            boolean currentStatus = profile.getIsReferenceDisabled() != null && profile.getIsReferenceDisabled();
            boolean newStatus = !currentStatus;
            profile.setIsReferenceDisabled(newStatus);

            // Update associated invite codes: if disabled, set isActive to false; if enabled, set isActive to true
            if (profile.getInviteCodes() != null) {
                for (InviteCode code : profile.getInviteCodes()) {
                    // We only want to enable the current reference code if it was the one disabled
                    // But the requirement says "when enabled/disabled", let's assume it applies to all or the current one.
                    // Usually, only one should be active anyway.
                    if (code.getCodeString().equalsIgnoreCase(profile.getReferenceNumber())) {
                        code.setIsActive(!newStatus);
                    }
                }
            }
            missionaryProfileRepository.save(profile);

            return ResponseEntity.ok(Map.of(
                    MESSAGE_KEY, profile.getIsReferenceDisabled() ? "Reference code disabled" : "Reference code enabled",
                    "isDisabled", profile.getIsReferenceDisabled()
            ));
        } catch (Exception e) {
            logger.error("Error toggling reference status", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @PostMapping("/profile/generate-code")
    @Transactional
    public ResponseEntity<Map<String, String>> generateNewInviteCode() {
        try {
            User user = getCurrentUser();
            MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(MISSIONARY_PROFILE_NOT_FOUND));

            // Generate new code
            String newCode = CodeGenerator.generateReference(ValidationConstants.REF_CODE_LENGTH);

            // Update profile's main reference number
            profile.setReferenceNumber(newCode);
            // Ensure the reference is enabled when a new code is generated
            profile.setIsReferenceDisabled(false);
            missionaryProfileRepository.save(profile);

            // Delete old invite codes and create new one
            inviteCodeRepository.deleteByMissionaryId(user.getId());

            InviteCode inviteCode = new InviteCode();
            inviteCode.setMissionary(profile);
            inviteCode.setCodeString(newCode);
            inviteCode.setIsActive(true);
            inviteCode.setCreatedAt(OffsetDateTime.now());
            inviteCodeRepository.save(inviteCode);

            return ResponseEntity.ok(Map.of(
                    MESSAGE_KEY, "New invite code generated successfully",
                    "newCode", newCode
            ));
        } catch (Exception e) {
            logger.error("Error generating new invite code", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
