package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.PostLikeRepository;
import com.shepherdsstories.data.repositories.PostRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.PostDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostRepository postRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    public PostController(PostRepository postRepository,
                          MissionaryProfileRepository missionaryProfileRepository,
                          UserRepository userRepository,
                          PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
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

            Post post = new Post();
            post.setTitle(postDTO.getTitle());
            post.setContent(postDTO.getContent());
            post.setAuthor(missionary);
            post.setCreatedAt(OffsetDateTime.now());
            post.setUpdatedAt(OffsetDateTime.now());

            Post savedPost = postRepository.save(post);
            return ResponseEntity.ok(convertToDTO(savedPost, user));
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
    public ResponseEntity<List<PostDTO>> getFeed(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            logger.info("Fetching feed for user: {} (ID: {}) with role: {}", user.getEmail(), user.getId(), user.getRole());

            // Supporter feed: posts from missionaries the user is connected to.
            // We assume any authenticated user can have a supporter feed if they have connections.
            List<Post> posts = postRepository.findAllForSupporter(user.getId(), RequestStatus.APPROVED);
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
    public ResponseEntity<PostDTO> updatePost(@PathVariable UUID id, @RequestBody PostDTO postDTO, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            if (!post.getAuthor().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            post.setTitle(postDTO.getTitle());
            post.setContent(postDTO.getContent());
            post.setUpdatedAt(OffsetDateTime.now());

            Post updatedPost = postRepository.save(post);
            return ResponseEntity.ok(convertToDTO(updatedPost, user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR updating post", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    @Transactional
    public ResponseEntity<PostDTO> toggleLike(@PathVariable UUID id, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

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

    private PostDTO convertToDTO(Post post, User currentUser) {
        long likeCount = postLikeRepository.countByPostId(post.getId());
        boolean liked = currentUser != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());

        return PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getMissionaryName())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .liked(liked)
                .build();
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
