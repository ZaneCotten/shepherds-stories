package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.PostLikeRepository;
import com.shepherdsstories.data.repositories.PostRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.MediaDTO;
import com.shepherdsstories.dtos.PostDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.entities.Post;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.PostLike;
import com.shepherdsstories.entities.PostLikeId;
import com.shepherdsstories.entities.Media;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import com.shepherdsstories.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);
    private static final String POST_NOT_FOUND = "Post not found";

    private static final String MESSAGE_KEY = "message";
    private final PostRepository postRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final S3Service s3Service;

    public PostController(PostRepository postRepository,
                          MissionaryProfileRepository missionaryProfileRepository,
                          SupporterProfileRepository supporterProfileRepository,
                          UserRepository userRepository,
                          PostLikeRepository postLikeRepository,
                          S3Service s3Service) {
        this.postRepository = postRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
        this.s3Service = s3Service;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            if (user.getRole() != Role.MISSIONARY) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            MissionaryProfile missionary = missionaryProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Missionary profile not found"));

            validatePostRequirements(postDTO);

            Post post = new Post();
            post.setTitle(postDTO.getTitle());
            post.setContent(postDTO.getContent());
            post.setAuthor(missionary);
            post.setCreatedAt(OffsetDateTime.now());
            post.setUpdatedAt(OffsetDateTime.now());

            Post savedPost = postRepository.save(post);
            savePostMedia(savedPost, postDTO.getMedia());

            return ResponseEntity.ok(convertToDTO(savedPost, user));
        } catch (IllegalArgumentException _) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR creating post", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PostDTO>> getMyPosts(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            if (user.getRole() != Role.MISSIONARY) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Post> posts = postRepository.findAllByAuthorIdWithAuthor(user.getId());
            List<PostDTO> postDTOs = posts.stream().map(p -> convertToDTO(p, user)).collect(Collectors.toList());
            return ResponseEntity.ok(postDTOs);
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR fetching my posts", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/feed")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PostDTO>> getFeed(@RequestParam(required = false) UUID missionaryId, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            logger.info("Fetching feed for user: {} (ID: {}) with role: {}, missionaryId: {}", user.getEmail(), user.getId(), user.getRole(), missionaryId);

            List<Post> posts = postRepository.findAllForSupporter(user.getId(), RequestStatus.APPROVED, missionaryId);
            logger.info("Found {} posts for user {}", posts.size(), user.getEmail());

            List<PostDTO> postDTOs = posts.stream().map(p -> convertToDTO(p, user)).collect(Collectors.toList());
            return ResponseEntity.ok(postDTOs);
        } catch (UnauthenticatedException _) {
            logger.warn("Unauthenticated access to /feed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            logger.warn("User not found during /feed access");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR fetching feed", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> updatePost(@PathVariable UUID id, @RequestBody PostDTO postDTO, Authentication authentication) {
        try {
            logger.info("Updating post {} with data: {}", id, postDTO);
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

            if (!post.getAuthor().getId().equals(user.getId())) {
                logger.warn("User {} unauthorized to update post {}", user.getId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(MESSAGE_KEY, "You are not authorized to edit this post"));
            }

            validatePostUpdate(postDTO);

            post.setTitle(postDTO.getTitle());
            post.setContent(postDTO.getContent());
            post.setUpdatedAt(OffsetDateTime.now());

            updateMedia(post, postDTO.getMedia());

            Post updatedPost = postRepository.save(post);
            logger.info("Successfully updated post {}", id);
            return ResponseEntity.ok(convertToDTO(updatedPost, user));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating post {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(MESSAGE_KEY, e.getMessage()));
        } catch (UnauthenticatedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(MESSAGE_KEY, e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, e.getMessage()));
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR updating post", t);
            String message = t.getMessage() != null ? t.getMessage() : "An unexpected error occurred";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(MESSAGE_KEY, "An internal error occurred: " + message));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> deletePost(@PathVariable UUID id, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

            if (!post.getAuthor().getId().equals(user.getId())) {
                logger.warn("User {} unauthorized to delete post {}", user.getId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(MESSAGE_KEY, "You are not authorized to delete this post"));
            }

            post.getMedia().forEach(media -> s3Service.deleteObject(media.getS3Key()));

            postRepository.delete(post);

            return ResponseEntity.noContent().build();
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR deleting post", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    @Transactional
    public ResponseEntity<PostDTO> toggleLike(@PathVariable UUID id, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

            PostLikeId likeId = new PostLikeId();
            likeId.setPostId(post.getId());
            likeId.setUserId(user.getId());

            if (postLikeRepository.existsById(likeId)) {
                postLikeRepository.deleteById(likeId);
            } else {
                PostLike like = new PostLike();
                like.setId(likeId);
                like.setPost(post);
                like.setUser(user);
                like.setCreatedAt(OffsetDateTime.now());
                postLikeRepository.save(like);
            }

            return ResponseEntity.ok(convertToDTO(post, user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR toggling like", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/upload-url")
    public ResponseEntity<MediaDTO> getUploadUrl(@RequestParam String fileName, @RequestParam String contentType, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            if (user.getRole() != Role.MISSIONARY) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String s3Key = "posts/" + user.getId() + "/" + UUID.randomUUID() + "-" + fileName;
            String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);

            MediaDTO response = MediaDTO.builder()
                    .s3Key(s3Key)
                    .url(uploadUrl)
                    .fileName(fileName)
                    .build();

            return ResponseEntity.ok(response);
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Throwable t) {
            logger.error("Error generating upload URL", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void validatePostRequirements(PostDTO postDTO) {
        if (postDTO.getTitle() == null || postDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is mandatory");
        }
        boolean hasContent = postDTO.getContent() != null && !postDTO.getContent().isBlank();
        boolean hasMedia = postDTO.getMedia() != null && !postDTO.getMedia().isEmpty();
        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Title must be paired with either content or a media file");
        }
    }

    private void validatePostUpdate(PostDTO postDTO) {
        if (postDTO.getTitle() == null || postDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is mandatory");
        }
        boolean hasContent = postDTO.getContent() != null && !postDTO.getContent().isBlank();
        boolean hasMedia = postDTO.getMedia() != null && !postDTO.getMedia().isEmpty();
        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Title must be paired with either content or a media file");
        }
    }

    private void updateMedia(Post post, List<MediaDTO> mediaDTOs) {
        if (mediaDTOs == null) {
            return;
        }

        Map<UUID, MediaDTO> dtoMap = mediaDTOs.stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(MediaDTO::getId, m -> m));

        // 1. Identify and remove media not in DTO list
        List<Media> toRemove = post.getMedia().stream()
                .filter(m -> !dtoMap.containsKey(m.getId()))
                .toList();

        for (Media m : toRemove) {
            logger.info("Removing media {} from post {}", m.getId(), post.getId());
            try {
                s3Service.deleteObject(m.getS3Key());
            } catch (Exception e) {
                logger.warn("Failed to delete S3 object {}: {}", m.getS3Key(), e.getMessage());
            }
            post.getMedia().remove(m);
        }

        // 2. Update existing and add new media
        for (MediaDTO mDto : mediaDTOs) {
            if (mDto.getId() != null) {
                post.getMedia().stream()
                        .filter(m -> m.getId().equals(mDto.getId()))
                        .findFirst()
                        .ifPresent(m -> m.setOrderNumber(mDto.getOrderNumber() != null ? mDto.getOrderNumber() : 0));
            } else {
                Media media = new Media();
                media.setPost(post);
                media.setS3Key(mDto.getS3Key());
                media.setBucketName(s3Service.getBucketName());
                media.setFileName(mDto.getFileName());
                media.setMediaType(mDto.getMediaType());
                media.setOrderNumber(mDto.getOrderNumber() != null ? mDto.getOrderNumber() : 0);
                post.getMedia().add(media);
            }
        }
    }

    private void savePostMedia(Post post, List<MediaDTO> mediaDTOs) {
        if (mediaDTOs == null) return;
        for (MediaDTO mDto : mediaDTOs) {
            Media media = new Media();
            media.setPost(post);
            media.setS3Key(mDto.getS3Key());
            media.setBucketName(s3Service.getBucketName());
            media.setFileName(mDto.getFileName());
            media.setMediaType(mDto.getMediaType());
            media.setOrderNumber(mDto.getOrderNumber() != null ? mDto.getOrderNumber() : 0);
            post.getMedia().add(media);
        }
    }

    private PostDTO convertToDTO(Post post, User currentUser) {
        long likeCount = postLikeRepository.countByPostId(post.getId());
        boolean liked = currentUser != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());

        String lastLikerName = resolveLastLikerName(post.getId(), currentUser);

        List<MediaDTO> mediaDTOs = post.getMedia().stream()
                .map(m -> MediaDTO.builder()
                        .id(m.getId())
                        .fileName(m.getFileName())
                        .mediaType(m.getMediaType())
                        .orderNumber(m.getOrderNumber())
                        .s3Key(m.getS3Key())
                        .url(s3Service.generatePresignedUrl(m.getS3Key()))
                        .build())
                .collect(Collectors.toList());

        return PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getMissionaryName())
                .authorProfilePictureUrl(s3Service.generatePresignedUrl(post.getAuthor().getUser().getProfilePictureKey()))
                .authorRole(Role.MISSIONARY.name())
                .authorBiography(post.getAuthor().getBiography())
                .authorLocationRegion(post.getAuthor().getLocationRegion())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .liked(liked)
                .lastLikerName(lastLikerName)
                .media(mediaDTOs)
                .build();
    }

    private String resolveLastLikerName(UUID postId, User currentUser) {
        List<PostLike> latestLikes = postLikeRepository.findLatestLikes(postId, PageRequest.of(0, 5));
        if (latestLikes.isEmpty()) {
            return null;
        }

        User lastLiker = latestLikes.getFirst().getUser();
        if (currentUser != null && lastLiker.getId().equals(currentUser.getId())) {
            return "you";
        }

        return getUserDisplayName(lastLiker);
    }

    private String getUserDisplayName(User user) {
        if (user.getRole() == Role.MISSIONARY) {
            return missionaryProfileRepository.findById(user.getId())
                    .map(mp -> {
                        String name = mp.getMissionaryName().trim();
                        return name.isEmpty() ? user.getEmail() : name;
                    })
                    .orElse(user.getEmail());
        } else if (user.getRole() == Role.SUPPORTER) {
            return supporterProfileRepository.findById(user.getId())
                    .map(sp -> {
                        String name = (sp.getFirstName() + " " + sp.getLastName()).trim();
                        return name.isEmpty() ? user.getEmail() : name;
                    })
                    .orElse(user.getEmail());
        }
        return user.getEmail();
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
}
