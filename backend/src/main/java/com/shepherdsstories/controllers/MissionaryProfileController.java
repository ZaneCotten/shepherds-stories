package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.ConnectionRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.ConnectionRequest;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
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

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;

    public MissionaryProfileController(MissionaryProfileRepository missionaryProfileRepository,
                                       UserRepository userRepository,
                                       ConnectionRepository connectionRepository) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
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
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/requests/{requestId}/respond")
    @Transactional
    public ResponseEntity<?> respondToRequest(@PathVariable java.util.UUID requestId, @RequestParam boolean approve) {
        User user = getCurrentUser();
        ConnectionRequest request = connectionRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getMissionary().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        request.setStatus(approve ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        request.setProcessedAt(OffsetDateTime.now());
        connectionRepository.save(request);

        return ResponseEntity.ok(Map.of("message", approve ? "Approved" : "Denied"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated");
        }

        String principalName = authentication.getName();
        String email = null;

        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken) {
            email = authToken.getPrincipal().getAttribute("email");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
            email = oauthUser.getAttribute("email");
        }

        if (email != null) {
            String finalEmail = email.trim().toLowerCase();
            return userRepository.findByEmailIgnoreCase(finalEmail)
                    .or(() -> userRepository.findByOauthId("GOOGLE:" + finalEmail))
                    .orElseThrow(() -> new RuntimeException("User not found by email: " + finalEmail));
        }

        return userRepository.findByEmailIgnoreCase(principalName)
                .or(() -> userRepository.findByOauthId(principalName))
                .orElseThrow(() -> new RuntimeException("User not found by principal: " + principalName));
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<MissionaryProfileDTO> getProfile() {
        User user = getCurrentUser();

        MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Missionary profile not found"));

        MissionaryProfileDTO dto = new MissionaryProfileDTO();
        dto.setMissionaryName(profile.getMissionaryName());
        dto.setLocationRegion(profile.getLocationRegion());
        dto.setBiography(profile.getBiography());
        dto.setReferenceNumber(profile.getReferenceNumber());

        return ResponseEntity.ok(dto);
    }
}
