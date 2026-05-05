package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.repositories.ConnectionRepository;
import com.shepherdsstories.data.repositories.InviteCodeRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.PrayerRequestRepository;
import com.shepherdsstories.data.repositories.PostRepository;
import com.shepherdsstories.data.repositories.CommentRepository;
import com.shepherdsstories.data.repositories.PostLikeRepository;
import com.shepherdsstories.data.repositories.CommentLikeRepository;
import com.shepherdsstories.dtos.BannedSupporterDTO;
import com.shepherdsstories.dtos.MissionaryProfileDTO;
import com.shepherdsstories.dtos.PrayerRequestDTO;
import com.shepherdsstories.dtos.SupporterProfileDTO;
import com.shepherdsstories.entities.ConnectionRequest;
import com.shepherdsstories.entities.Comment;
import com.shepherdsstories.entities.InviteCode;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.PrayerRequest;
import com.shepherdsstories.entities.Post;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import com.shepherdsstories.services.S3Service;
import com.shepherdsstories.utils.CodeGenerator;
import com.shepherdsstories.utils.ValidationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missionary")
public class MissionaryProfileController {
    private static final Logger logger = LoggerFactory.getLogger(MissionaryProfileController.class);
    private static final String MISSIONARY_PROFILE_NOT_FOUND = "Missionary profile not found";
    private static final String MESSAGE_KEY = "message";

    private static final String CONNECTION_NOT_FOUND = "Connection not found";

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final PrayerRequestRepository prayerRequestRepository;
    private final PostRepository postRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private CommentRepository commentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PostLikeRepository postLikeRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private CommentLikeRepository commentLikeRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private S3Service s3Service;

    public MissionaryProfileController(MissionaryProfileRepository missionaryProfileRepository,
                                       UserRepository userRepository,
                                       ConnectionRepository connectionRepository,
                                       InviteCodeRepository inviteCodeRepository,
                                       SupporterProfileRepository supporterProfileRepository,
                                       PrayerRequestRepository prayerRequestRepository,
                                       PostRepository postRepository) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.prayerRequestRepository = prayerRequestRepository;
        this.postRepository = postRepository;
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
                                "profilePictureUrl", (req.getSupporter() != null && req.getSupporter().getUser() != null) ? s3Service.generatePresignedUrl(req.getSupporter().getUser().getProfilePictureKey()) : "",
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

    @GetMapping("/supporters")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SupporterProfileDTO>> getSupporters() {
        User user = getCurrentUser();
        List<ConnectionRequest> connections = connectionRepository.findByMissionaryIdAndStatus(user.getId(), RequestStatus.APPROVED);

        List<SupporterProfileDTO> supporters = connections.stream()
                .map(req -> {
                    var s = req.getSupporter();
                    return SupporterProfileDTO.builder()
                            .id(s.getId())
                            .firstName(s.getFirstName())
                            .lastName(s.getLastName())
                            .profilePictureUrl((s.getUser() != null) ? s3Service.generatePresignedUrl(s.getUser().getProfilePictureKey()) : "")
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(supporters);
    }

    @PostMapping("/supporters/{supporterId}/remove")
    @Transactional
    public ResponseEntity<Map<String, String>> removeSupporter(@PathVariable java.util.UUID supporterId) {
        User user = getCurrentUser();
        ConnectionRequest connection = connectionRepository.findByMissionaryIdAndSupporterId(user.getId(), supporterId)
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

        if (connection.getStatus() != RequestStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Supporter is not currently connected."));
        }

        connection.setStatus(RequestStatus.REJECTED);
        connection.setProcessedAt(OffsetDateTime.now().minusMinutes(2)); // Allow immediate reconnection
        connectionRepository.save(connection);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Supporter removed"));
    }

    @PostMapping("/supporters/{supporterId}/ban")
    @Transactional
    public ResponseEntity<Map<String, String>> banSupporter(@PathVariable java.util.UUID supporterId) {
        User user = getCurrentUser();
        ConnectionRequest connection = connectionRepository.findByMissionaryIdAndSupporterId(user.getId(), supporterId)
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

        connection.setStatus(RequestStatus.BANNED);
        connection.setProcessedAt(OffsetDateTime.now());
        connectionRepository.save(connection);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Supporter banned"));
    }

    @GetMapping("/banned-supporters")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BannedSupporterDTO>> getBannedSupporters() {
        User user = getCurrentUser();
        List<ConnectionRequest> connections = connectionRepository.findByMissionaryIdAndStatus(user.getId(), RequestStatus.BANNED);

        List<BannedSupporterDTO> bannedSupporters = connections.stream()
                .map(req -> {
                    var s = req.getSupporter();
                    return BannedSupporterDTO.builder()
                            .id(s.getId())
                            .firstName(s.getFirstName())
                            .lastName(s.getLastName())
                            .profilePictureUrl((s.getUser() != null) ? s3Service.generatePresignedUrl(s.getUser().getProfilePictureKey()) : "")
                            .bannedAt(req.getProcessedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(bannedSupporters);
    }

    @PostMapping("/supporters/{supporterId}/unban")
    @Transactional
    public ResponseEntity<Map<String, String>> unbanSupporter(@PathVariable java.util.UUID supporterId) {
        User user = getCurrentUser();
        ConnectionRequest connection = connectionRepository.findByMissionaryIdAndSupporterId(user.getId(), supporterId)
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

        if (connection.getStatus() != RequestStatus.BANNED) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, "Supporter is not banned."));
        }

        connection.setStatus(RequestStatus.REJECTED);
        connection.setProcessedAt(OffsetDateTime.now().minusMinutes(2)); // Allow immediate re-request
        connectionRepository.save(connection);

        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Supporter unbanned"));
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

            MissionaryProfileDTO dto = MissionaryProfileDTO.builder()
                    .id(profile.getId())
                    .missionaryName(profile.getMissionaryName())
                    .locationRegion(profile.getLocationRegion())
                    .biography(profile.getBiography())
                    .referenceNumber(profile.getReferenceNumber())
                    .isReferenceDisabled(profile.getIsReferenceDisabled())
                    .profilePictureUrl(s3Service.generatePresignedUrl(user.getProfilePictureKey()))
                    .build();

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching missionary profile", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<MissionaryProfileDTO> updateProfile(@RequestBody MissionaryProfileDTO updateDto) {
        try {
            User user = getCurrentUser();
            MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(MISSIONARY_PROFILE_NOT_FOUND));

            if (updateDto.getMissionaryName() != null && !updateDto.getMissionaryName().isBlank()) {
                profile.setMissionaryName(updateDto.getMissionaryName());
            }

            if (updateDto.getLocationRegion() != null) {
                profile.setLocationRegion(updateDto.getLocationRegion());
            }

            if (updateDto.getBiography() != null) {
                profile.setBiography(updateDto.getBiography());
            }

            if (updateDto.getProfilePictureUrl() != null) {
                user.setProfilePictureKey(updateDto.getProfilePictureUrl());
                userRepository.save(user);
            }

            missionaryProfileRepository.save(profile);

            return ResponseEntity.ok(MissionaryProfileDTO.builder()
                    .id(profile.getId())
                    .missionaryName(profile.getMissionaryName())
                    .locationRegion(profile.getLocationRegion())
                    .biography(profile.getBiography())
                    .referenceNumber(profile.getReferenceNumber())
                    .isReferenceDisabled(profile.getIsReferenceDisabled())
                    .profilePictureUrl(s3Service.generatePresignedUrl(user.getProfilePictureKey()))
                    .build());
        } catch (Exception e) {
            logger.error("Error updating missionary profile", e);
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

    @PostMapping("/profile/picture/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(@RequestParam String contentType) {
        User user = getCurrentUser();
        String s3Key = "profiles/" + user.getId() + "/" + java.util.UUID.randomUUID();
        String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);
        return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "s3Key", s3Key));
    }

    @GetMapping("/prayer-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PrayerRequestDTO>> getMyPrayerRequests() {
        User user = getCurrentUser();
        List<PrayerRequest> requests = prayerRequestRepository.findAllByMissionaryIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(requests.stream().map(this::toPrayerRequestDTO).collect(Collectors.toList()));
    }

    @PostMapping("/prayer-requests")
    @Transactional
    public ResponseEntity<PrayerRequestDTO> createPrayerRequest(@RequestBody PrayerRequestDTO dto) {
        User user = getCurrentUser();
        MissionaryProfile profile = missionaryProfileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(MISSIONARY_PROFILE_NOT_FOUND));

        PrayerRequest request = new PrayerRequest();
        request.setTitle(dto.getTitle());
        request.setContent(dto.getContent());
        request.setAnswered(dto.isAnswered());
        request.setMissionary(profile);
        request.setCreatedAt(java.time.LocalDateTime.now());

        PrayerRequest saved = prayerRequestRepository.save(request);
        return ResponseEntity.ok(toPrayerRequestDTO(saved));
    }

    @PutMapping("/prayer-requests/{requestId}")
    @Transactional
    public ResponseEntity<PrayerRequestDTO> updatePrayerRequest(@PathVariable UUID requestId, @RequestBody PrayerRequestDTO dto) {
        User user = getCurrentUser();
        PrayerRequest request = prayerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Prayer request not found"));

        if (!request.getMissionary().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        request.setTitle(dto.getTitle());
        request.setContent(dto.getContent());
        request.setAnswered(dto.isAnswered());

        PrayerRequest saved = prayerRequestRepository.save(request);
        return ResponseEntity.ok(toPrayerRequestDTO(saved));
    }

    @DeleteMapping("/prayer-requests/{requestId}")
    @Transactional
    public ResponseEntity<Void> deletePrayerRequest(@PathVariable UUID requestId) {
        User user = getCurrentUser();
        PrayerRequest request = prayerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Prayer request not found"));

        if (!request.getMissionary().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        prayerRequestRepository.delete(request);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/export/csv")
    @Transactional(readOnly = true)
    public void exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean includeSupporters,
            @RequestParam(defaultValue = "true") boolean includeRequests,
            @RequestParam(defaultValue = "true") boolean includePrayers,
            @RequestParam(defaultValue = "true") boolean includePosts,
            @RequestParam(defaultValue = "false") boolean includeComments,
            @RequestParam(defaultValue = "false") boolean includeAllComments,
            @RequestParam(defaultValue = "false") boolean includeLikes,
            @RequestParam(defaultValue = "true") boolean includeSupporterCount,
            HttpServletResponse response) throws IOException {

        User user = getCurrentUser();
        OffsetDateTime start = startDate != null ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC) : OffsetDateTime.parse("1970-01-01T00:00:00Z");
        OffsetDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC) : OffsetDateTime.now().plusYears(100);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=missionary_data_export.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Type,Date,Title/Name,Content/Email,Status/Other,Likes");

            if (includeSupporters) writeSupporters(writer, user.getId(), start, end);
            if (includeRequests) writeRequests(writer, user.getId(), start, end, includeSupporters);
            if (includePrayers) writePrayers(writer, user.getId(), start, end);
            if (includePosts) writePosts(writer, user.getId(), start, end, includeComments, includeLikes);
            if (includeAllComments)
                writeAllComments(writer, user.getId(), start, end, includePosts && includeComments, includeLikes);

            writeSummary(writer, user.getId(), start, end, includeSupporterCount, includePrayers);
        }
    }

    private void writeSupporters(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end) {
        List<ConnectionRequest> connections = connectionRepository.findByMissionaryIdAndStatusAndCreatedAtBetween(missionaryId, RequestStatus.APPROVED, start, end);
        for (ConnectionRequest cr : connections) {
            String name = cr.getSupporter().getFirstName() + " " + cr.getSupporter().getLastName();
            writer.printf("Supporter,%s,\"%s\",%s,Joined,%n", cr.getCreatedAt(), escapeCsv(name), cr.getSupporter().getUser().getEmail());
        }
    }

    private void writeRequests(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end, boolean includeSupporters) {
        List<ConnectionRequest> requests = connectionRepository.findByMissionaryIdAndCreatedAtBetween(missionaryId, start, end);
        for (ConnectionRequest cr : requests) {
            if (cr.getStatus() == RequestStatus.APPROVED && includeSupporters) continue;
            String name = cr.getSupporter().getFirstName() + " " + cr.getSupporter().getLastName();
            writer.printf("Connection Request,%s,\"%s\",%s,%s,%n", cr.getCreatedAt(), escapeCsv(name), cr.getSupporter().getUser().getEmail(), cr.getStatus());
        }
    }

    private void writePrayers(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end) {
        List<PrayerRequest> prayers = prayerRequestRepository.findAllByMissionaryIdAndCreatedAtBetweenOrderByCreatedAtDesc(missionaryId, start.toLocalDateTime(), end.toLocalDateTime());
        for (PrayerRequest p : prayers) {
            writer.printf("Prayer Request,%s,\"%s\",\"%s\",%s,%n", p.getCreatedAt(), escapeCsv(p.getTitle()), escapeCsv(p.getContent()), p.isAnswered() ? "Answered" : "Unanswered");
        }
    }

    private void writePosts(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end, boolean includeComments, boolean includeLikes) {
        List<Post> posts = postRepository.findAllByAuthorIdAndCreatedAtBetweenOrderByCreatedAtDesc(missionaryId, start, end);
        for (Post p : posts) {
            String likes = includeLikes ? String.valueOf(postLikeRepository.countByPostId(p.getId())) : "";
            writer.printf("Post,%s,\"%s\",\"%s\",Author: %s,%s%n", p.getCreatedAt(), escapeCsv(p.getTitle()), escapeCsv(p.getContent()), escapeCsv(p.getAuthor().getMissionaryName()), likes);
            if (includeComments) writeCommentsForPost(writer, p, start, end, includeLikes);
        }
    }

    private void writeCommentsForPost(PrintWriter writer, Post p, OffsetDateTime start, OffsetDateTime end, boolean includeLikes) {
        List<Comment> comments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(p.getId());
        for (Comment c : comments) {
            if (c.getCreatedAt().isBefore(start) || c.getCreatedAt().isAfter(end)) continue;
            String cLikes = includeLikes ? String.valueOf(commentLikeRepository.countByCommentId(c.getId())) : "";
            writer.printf("Comment,%s,\"Post: %s\",\"%s\",By: %s,%s%n", c.getCreatedAt(), escapeCsv(p.getTitle()), escapeCsv(c.getContent()), escapeCsv(getUserDisplayName(c.getUser())), cLikes);
        }
    }

    private void writeAllComments(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end, boolean skipDuplicate, boolean includeLikes) {
        List<Comment> comments = commentRepository.findAllByPostAuthorIdAndCreatedAtBetweenOrderByCreatedAtDesc(missionaryId, start, end);
        for (Comment c : comments) {
            if (skipDuplicate && c.getPost().getCreatedAt().isAfter(start) && c.getPost().getCreatedAt().isBefore(end))
                continue;
            String cLikes = includeLikes ? String.valueOf(commentLikeRepository.countByCommentId(c.getId())) : "";
            writer.printf("Comment,%s,\"Post: %s\",\"%s\",By: %s,%s%n", c.getCreatedAt(), escapeCsv(c.getPost().getTitle()), escapeCsv(c.getContent()), escapeCsv(getUserDisplayName(c.getUser())), cLikes);
        }
    }

    private void writeSummary(PrintWriter writer, UUID missionaryId, OffsetDateTime start, OffsetDateTime end, boolean includeSupporterCount, boolean includePrayers) {
        writer.println();
        writer.println("Summary Metrics");
        if (includeSupporterCount) {
            writer.printf("\"Supporter Count (for range)\",,%d,,, %n", connectionRepository.countByMissionaryIdAndStatusAndCreatedAtBetween(missionaryId, RequestStatus.APPROVED, start, end));
        }
        if (includePrayers) {
            writer.printf("\"Total Prayer Requests Created (for range)\",,%d,,, %n", prayerRequestRepository.countByMissionaryIdAndCreatedAtBetween(missionaryId, start.toLocalDateTime(), end.toLocalDateTime()));
            writer.printf("\"Total Prayer Requests Answered (for range)\",,%d,,, %n", prayerRequestRepository.countByMissionaryIdAndAnsweredTrueAndCreatedAtBetween(missionaryId, start.toLocalDateTime(), end.toLocalDateTime()));
        }
    }

    private String getUserDisplayName(User user) {
        if (user == null) return "Unknown";
        return supporterProfileRepository.findById(user.getId())
                .map(sp -> sp.getFirstName() + " " + sp.getLastName())
                .orElseGet(() -> missionaryProfileRepository.findById(user.getId())
                        .map(MissionaryProfile::getMissionaryName)
                        .orElse(user.getEmail()));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
