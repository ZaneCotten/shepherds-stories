package com.shepherdsstories.controllers;

import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missionary")
public class MissionaryProfileController {

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final UserRepository userRepository;

    public MissionaryProfileController(MissionaryProfileRepository missionaryProfileRepository, UserRepository userRepository) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<MissionaryProfileDTO> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    // Fallback to searching by OauthId if email doesn't match directly
                    // This can happen if the authentication principal name is provider:email
                    return userRepository.findByOauthId(email)
                            .orElseThrow(() -> new RuntimeException("User not found: " + email));
                });

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
