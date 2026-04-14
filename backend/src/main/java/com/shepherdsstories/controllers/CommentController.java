package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.CommentDTO;
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
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final ConnectionRepository connectionRepository;

    public CommentController(CommentRepository commentRepository,
                             PostRepository postRepository,
                             UserRepository userRepository,
                             MissionaryProfileRepository missionaryProfileRepository,
                             SupporterProfileRepository supporterProfileRepository,
                             ConnectionRepository connectionRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.connectionRepository = connectionRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CommentDTO> addComment(@PathVariable UUID postId,
                                                 @RequestBody CommentDTO commentDTO,
                                                 Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            // Access check: Supporters must be connected to the missionary who wrote the post
            if (user.getRole() == Role.SUPPORTER) {
                boolean connected = connectionRepository.existsByMissionaryIdAndSupporterId(post.getAuthor().getId(), user.getId());
                if (!connected) {
                    logger.warn("Supporter {} attempted to comment on post {} without connection", user.getEmail(), postId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            Comment comment = new Comment();
            comment.setPost(post);
            comment.setUser(user);
            comment.setContent(commentDTO.getContent());
            comment.setCreatedAt(OffsetDateTime.now());

            if (commentDTO.getParentCommentId() != null) {
                Comment parentComment = commentRepository.findById(commentDTO.getParentCommentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
                comment.setParentComment(parentComment);
            }

            Comment savedComment = commentRepository.save(comment);
            return ResponseEntity.ok(convertToDTO(savedComment));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR adding comment", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable UUID postId) {
        try {
            List<Comment> comments = commentRepository.findAllByPostId(postId);
            List<CommentDTO> commentDTOs = comments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(commentDTOs);
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR fetching comments", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private CommentDTO convertToDTO(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userName(getUserName(comment.getUser()))
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private String getUserName(User user) {
        if (user.getRole() == Role.MISSIONARY) {
            return missionaryProfileRepository.findById(user.getId())
                    .map(MissionaryProfile::getMissionaryName)
                    .orElse("Unknown Missionary");
        } else if (user.getRole() == Role.SUPPORTER) {
            return supporterProfileRepository.findById(user.getId())
                    .map(sp -> sp.getFirstName() + " " + sp.getLastName())
                    .orElse("Unknown Supporter");
        }
        return "Unknown User";
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException("Unauthenticated");
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
