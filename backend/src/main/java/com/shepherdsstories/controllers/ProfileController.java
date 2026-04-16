package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.dtos.SupporterProfileDTO;
import com.shepherdsstories.dtos.UserProfileDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.SupporterProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import com.shepherdsstories.services.ProfileService;
import com.shepherdsstories.services.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final S3Service s3Service;

    public ProfileController(UserRepository userRepository,
                             ProfileService profileService,
                             MissionaryProfileRepository missionaryProfileRepository,
                             SupporterProfileRepository supporterProfileRepository,
                             S3Service s3Service) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.s3Service = s3Service;
    }

    @GetMapping
    public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            return ResponseEntity.ok(convertToDTO(user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(Authentication authentication, @RequestParam String contentType) {
        try {
            User user = getCurrentUser(authentication);
            String key = "profiles/" + user.getId() + "/profile-picture-" + System.currentTimeMillis();
            String uploadUrl = s3Service.generateUploadUrl(key, contentType);
            return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "key", key));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/picture")
    @Transactional
    public ResponseEntity<UserProfileDTO> updateProfilePicture(Authentication authentication, @RequestBody Map<String, String> payload) {
        try {
            User user = getCurrentUser(authentication);
            String key = payload.get("key");

            if (user.getProfilePictureKey() != null) {
                s3Service.deleteObject(user.getProfilePictureKey());
            }

            user.setProfilePictureKey(key);
            userRepository.save(user);

            return ResponseEntity.ok(convertToDTO(user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException("Unauthenticated");
        }

        if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
            return userRepository.findById(details.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found by ID: " + details.getId()));
        }

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

        String principalName = authentication.getName();
        return userRepository.findByEmailIgnoreCase(principalName)
                .or(() -> userRepository.findByOauthId(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("User not found by principal: " + principalName));
    }

    private UserProfileDTO convertToDTO(User user) {
        String profilePictureUrl = s3Service.generatePresignedUrl(user.getProfilePictureKey());

        if (user.getRole() == Role.MISSIONARY) {
            MissionaryProfile profile = missionaryProfileRepository.findById(user.getId()).orElse(null);
            MissionaryProfileDTO dto = new MissionaryProfileDTO();
            populateCommonDTOFields(dto, user, profilePictureUrl);
            if (profile != null) {
                dto.setMissionaryName(profile.getMissionaryName());
                dto.setLocationRegion(profile.getLocationRegion());
                dto.setBiography(profile.getBiography());
                dto.setReferenceNumber(profile.getReferenceNumber());
                dto.setIsReferenceDisabled(profile.getIsReferenceDisabled());
                dto.setDisplayName(profileService.getUserDisplayName(user));
            }
            return dto;
        } else if (user.getRole() == Role.SUPPORTER) {
            SupporterProfile profile = supporterProfileRepository.findById(user.getId()).orElse(null);
            SupporterProfileDTO dto = new SupporterProfileDTO();
            populateCommonDTOFields(dto, user, profilePictureUrl);
            if (profile != null) {
                dto.setFirstName(profile.getFirstName());
                dto.setLastName(profile.getLastName());
                dto.setIsVerified(profile.getIsVerified());
                dto.setDisplayName(profileService.getUserDisplayName(user));
            }
            return dto;
        } else {
            UserProfileDTO dto = new UserProfileDTO();
            populateCommonDTOFields(dto, user, profilePictureUrl);
            dto.setDisplayName("Admin");
            return dto;
        }
    }

    private void populateCommonDTOFields(UserProfileDTO dto, User user, String profilePictureUrl) {
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setProfilePictureUrl(profilePictureUrl);
    }
}
