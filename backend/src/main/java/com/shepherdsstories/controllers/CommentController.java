package com.shepherdsstories.controllers;

import com.shepherdsstories.config.UserAuthConfig;
import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.CommentDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
import com.shepherdsstories.exceptions.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private static final String ERROR_KEY = "error";
    private static final String COMMENT_NOT_FOUND = "Comment not found";
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final ConnectionRepository connectionRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentController(CommentRepository commentRepository,
                             PostRepository postRepository,
                             UserRepository userRepository,
                             MissionaryProfileRepository missionaryProfileRepository,
                             SupporterProfileRepository supporterProfileRepository,
                             ConnectionRepository connectionRepository,
                             CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.connectionRepository = connectionRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> addComment(@PathVariable UUID postId,
                                             @RequestBody CommentDTO commentDTO,
                                             Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            // Access check: Supporters must be connected to the missionary who wrote the post
            if (user.getRole() == Role.SUPPORTER) {
                boolean connected = connectionRepository.existsByMissionaryIdAndSupporterIdAndStatus(post.getAuthor().getId(), user.getId(), RequestStatus.APPROVED);
                if (!connected) {
                    logger.warn("Supporter {} attempted to comment on post {} without connection", user.getEmail(), postId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of(ERROR_KEY, "You must be connected to this missionary to comment."));
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
                if (!parentComment.getPost().getId().equals(postId)) {
                    logger.warn("User {} attempted to reply to comment {} which belongs to a different post", user.getEmail(), commentDTO.getParentCommentId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(ERROR_KEY, "Parent comment does not belong to this post."));
                }
                comment.setParentComment(parentComment);
            }

            Comment savedComment = commentRepository.save(comment);
            return ResponseEntity.ok(convertToDTO(savedComment, user));
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
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable UUID postId,
                                                        Authentication authentication) {
        try {
            User currentUser = authentication != null && authentication.isAuthenticated() ? getCurrentUser(authentication) : null;
            List<Comment> comments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId);
            List<CommentDTO> commentDTOs = comments.stream()
                    .map(c -> convertToDTO(c, currentUser))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(commentDTOs);
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR fetching comments", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{commentId}")
    @Transactional
    public ResponseEntity<Object> updateComment(@PathVariable UUID postId,
                                                @PathVariable UUID commentId,
                                                @RequestBody CommentDTO commentDTO,
                                                Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND));

            if (!comment.getPost().getId().equals(postId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(ERROR_KEY, "Comment does not belong to this post."));
            }

            if (!comment.getUser().getId().equals(user.getId())) {
                logger.warn("User {} attempted to update comment {} owned by {}", user.getEmail(), commentId, comment.getUser().getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of(ERROR_KEY, "You can only edit your own comments."));
            }

            if (comment.getIsDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(ERROR_KEY, "Deleted comments cannot be edited."));
            }

            comment.setContent(commentDTO.getContent());
            comment.setEdited(true);
            comment.setUpdatedAt(OffsetDateTime.now());
            Comment savedComment = commentRepository.save(comment);
            return ResponseEntity.ok(convertToDTO(savedComment, user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR updating comment", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{commentId}")
    @Transactional
    public ResponseEntity<Object> deleteComment(@PathVariable UUID postId,
                                                @PathVariable UUID commentId,
                                                Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND));

            if (!comment.getPost().getId().equals(postId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(ERROR_KEY, "Comment does not belong to this post."));
            }

            if (!isUserAuthorizedToDelete(comment, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of(ERROR_KEY, "You can only delete your own comments or comments on your own post."));
            }

            comment.setContent("comment has been deleted");
            comment.setIsDeleted(true);
            commentRepository.saveAndFlush(comment);

            Set<UUID> usefulIds = pruneUselessComments(postId);

            if (!usefulIds.contains(commentId)) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(convertToDTO(comment, user));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR deleting comment", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isUserAuthorizedToDelete(Comment comment, User user) {
        boolean isCommentOwner = comment.getUser().getId().equals(user.getId());
        boolean isPostOwner = comment.getPost().getAuthor().getUser().getId().equals(user.getId());

        if (!isCommentOwner && !isPostOwner) {
            logger.warn("User {} attempted to delete comment {} owned by {} on post owned by {}",
                    user.getEmail(), comment.getId(), comment.getUser().getEmail(), comment.getPost().getAuthor().getUser().getEmail());
            return false;
        }
        return true;
    }

    private Set<UUID> pruneUselessComments(UUID postId) {
        List<Comment> allComments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId);
        Set<UUID> usefulIds = identifyUsefulComments(allComments);

        List<Comment> rootsToDelete = allComments.stream()
                .filter(c -> !usefulIds.contains(c.getId()))
                .filter(c -> c.getParentComment() == null || usefulIds.contains(c.getParentComment().getId()))
                .collect(Collectors.toList());

        if (!rootsToDelete.isEmpty()) {
            commentRepository.deleteAll(rootsToDelete);
            commentRepository.flush();
        }
        return usefulIds;
    }

    private Set<UUID> identifyUsefulComments(List<Comment> allComments) {
        Set<UUID> usefulIds = new HashSet<>();
        for (Comment c : allComments) {
            if (!c.getIsDeleted()) {
                usefulIds.add(c.getId());
            }
        }

        Map<UUID, Comment> commentMap = allComments.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        List<UUID> toProcess = new ArrayList<>(usefulIds);
        int index = 0;
        while (index < toProcess.size()) {
            Comment c = commentMap.get(toProcess.get(index++));
            if (c != null && c.getParentComment() != null) {
                UUID parentId = c.getParentComment().getId();
                if (usefulIds.add(parentId)) {
                    toProcess.add(parentId);
                }
            }
        }
        return usefulIds;
    }

    @PostMapping("/{commentId}/like")
    @Transactional
    public ResponseEntity<CommentDTO> toggleLike(@PathVariable UUID postId,
                                                 @PathVariable UUID commentId,
                                                 Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND));

            if (!comment.getPost().getId().equals(postId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            CommentLikeId likeId = new CommentLikeId();
            likeId.setCommentId(commentId);
            likeId.setUserId(currentUser.getId());

            if (commentLikeRepository.existsByCommentIdAndUserId(commentId, currentUser.getId())) {
                commentLikeRepository.findById(likeId).ifPresent(commentLikeRepository::delete);
            } else {
                CommentLike like = new CommentLike();
                like.setId(likeId);
                like.setComment(comment);
                like.setUser(currentUser);
                like.setCreatedAt(OffsetDateTime.now());
                commentLikeRepository.save(like);
            }

            return ResponseEntity.ok(convertToDTO(comment, currentUser));
        } catch (UnauthenticatedException _) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ResourceNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Throwable t) {
            logger.error("CRITICAL ERROR toggling comment like", t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private CommentDTO convertToDTO(Comment comment, User currentUser) {
        UUID commentId = comment.getId();
        long likeCount = commentLikeRepository.countByCommentId(commentId);
        boolean liked = currentUser != null && commentLikeRepository.existsByCommentIdAndUserId(commentId, currentUser.getId());
        String lastLikerName = resolveLastLikerName(commentId, currentUser);

        return CommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userName(getUserName(comment.getUser()))
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .edited(comment.getEdited())
                .isDeleted(comment.getIsDeleted())
                .likeCount(likeCount)
                .liked(liked)
                .lastLikerName(lastLikerName)
                .build();
    }

    private String resolveLastLikerName(UUID commentId, User currentUser) {
        List<CommentLike> latestLikes = commentLikeRepository.findLatestLikes(commentId, PageRequest.of(0, 1));
        if (latestLikes.isEmpty()) {
            return null;
        }

        CommentLike lastLike = latestLikes.getFirst();
        if (currentUser != null && lastLike.getUser().getId().equals(currentUser.getId())) {
            return "you";
        }

        return getUserDisplayName(lastLike.getUser());
    }

    private String getUserDisplayName(User user) {
        if (user.getRole() == Role.MISSIONARY) {
            return missionaryProfileRepository.findById(user.getId()).filter(mp -> (mp.getMissionaryName() != null && !mp.getMissionaryName().isEmpty())).map(MissionaryProfile::getMissionaryName).orElse(user.getEmail());
        } else if (user.getRole() == Role.SUPPORTER) {
            return supporterProfileRepository.findById(user.getId())
                    .map(sp -> {
                        String name = ((sp.getFirstName() != null ? sp.getFirstName() : "") + " " + (sp.getLastName() != null ? sp.getLastName() : "")).trim();
                        return name.isEmpty() ? user.getEmail() : name;
                    })
                    .orElse(user.getEmail());
        }
        return user.getEmail();
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
