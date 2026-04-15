package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.CommentDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.services.ProfileService;
import com.shepherdsstories.services.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private CommentController controller;

    private User missionaryUser;
    private MissionaryProfile missionaryProfile;
    private User supporterUser;
    private Post post;
    private Authentication missionaryAuth;
    private Authentication supporterAuth;

    @BeforeEach
    void setUp() {
        missionaryUser = new User();
        missionaryUser.setId(UUID.randomUUID());
        missionaryUser.setEmail("missionary@test.com");
        missionaryUser.setRole(Role.MISSIONARY);

        missionaryProfile = new MissionaryProfile();
        missionaryProfile.setId(missionaryUser.getId());
        missionaryProfile.setUser(missionaryUser);
        missionaryProfile.setMissionaryName("Test Missionary");

        supporterUser = new User();
        supporterUser.setId(UUID.randomUUID());
        supporterUser.setEmail("supporter@test.com");
        supporterUser.setRole(Role.SUPPORTER);

        SupporterProfile supporterProfile = new SupporterProfile();
        supporterProfile.setId(supporterUser.getId());
        supporterProfile.setUser(supporterUser);
        supporterProfile.setFirstName("John");
        supporterProfile.setLastName("Doe");

        post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(missionaryProfile);

        missionaryAuth = mock(Authentication.class);
        lenient().when(missionaryAuth.isAuthenticated()).thenReturn(true);
        lenient().when(missionaryAuth.getName()).thenReturn("missionary@test.com");

        supporterAuth = mock(Authentication.class);
        lenient().when(supporterAuth.isAuthenticated()).thenReturn(true);
        lenient().when(supporterAuth.getName()).thenReturn("supporter@test.com");

        lenient().when(userRepository.findByEmailIgnoreCase("missionary@test.com")).thenReturn(Optional.of(missionaryUser));
        lenient().when(userRepository.findByEmailIgnoreCase("supporter@test.com")).thenReturn(Optional.of(supporterUser));
        lenient().when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        lenient().when(profileService.getUserName(missionaryUser)).thenReturn("Test Missionary");
        lenient().when(profileService.getUserName(supporterUser)).thenReturn("John Doe");
        lenient().when(profileService.getUserDisplayName(missionaryUser)).thenReturn("Test Missionary");
        lenient().when(profileService.getUserDisplayName(supporterUser)).thenReturn("John Doe");
    }

    @Test
    void addComment_MissionarySuccess() {
        CommentDTO commentDTO = CommentDTO.builder()
                .content("Missionary comment")
                .build();

        Comment savedComment = new Comment();
        savedComment.setId(UUID.randomUUID());
        savedComment.setPost(post);
        savedComment.setUser(missionaryUser);
        savedComment.setContent(commentDTO.getContent());

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        ResponseEntity<?> response = controller.addComment(post.getId(), commentDTO, missionaryAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseBody = (CommentDTO) response.getBody();
        assert responseBody != null;
        assertEquals("Missionary comment", responseBody.getContent());
        assertEquals("Test Missionary", responseBody.getUserName());
    }

    @Test
    void addComment_SupporterSuccess() {
        CommentDTO commentDTO = CommentDTO.builder()
                .content("Supporter comment")
                .build();

        Comment savedComment = new Comment();
        savedComment.setId(UUID.randomUUID());
        savedComment.setPost(post);
        savedComment.setUser(supporterUser);
        savedComment.setContent(commentDTO.getContent());
        savedComment.setCreatedAt(OffsetDateTime.now());

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(connectionRepository.existsByMissionaryIdAndSupporterIdAndStatus(missionaryProfile.getId(), supporterUser.getId(), RequestStatus.APPROVED)).thenReturn(true);

        ResponseEntity<?> response = controller.addComment(post.getId(), commentDTO, supporterAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseBody = (CommentDTO) response.getBody();
        assert responseBody != null;
        assertEquals("Supporter comment", responseBody.getContent());
        assertEquals("John Doe", responseBody.getUserName());
    }

    @Test
    void addComment_PostNotFound() {
        CommentDTO commentDTO = CommentDTO.builder().content("Comment").build();
        UUID fakePostId = UUID.randomUUID();
        when(postRepository.findById(fakePostId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.addComment(fakePostId, commentDTO, supporterAuth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void addComment_SupporterForbidden() {
        CommentDTO commentDTO = CommentDTO.builder().content("No permission").build();
        when(connectionRepository.existsByMissionaryIdAndSupporterIdAndStatus(missionaryProfile.getId(), supporterUser.getId(), RequestStatus.APPROVED)).thenReturn(false);

        ResponseEntity<?> response = controller.addComment(post.getId(), commentDTO, supporterAuth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void addComment_ReplySuccess() {
        UUID parentCommentId = UUID.randomUUID();
        Comment parentComment = new Comment();
        parentComment.setId(parentCommentId);
        parentComment.setPost(post);
        parentComment.setUser(missionaryUser);
        parentComment.setContent("Parent content");

        CommentDTO replyDTO = CommentDTO.builder()
                .content("Reply content")
                .parentCommentId(parentCommentId)
                .build();

        Comment savedReply = new Comment();
        savedReply.setId(UUID.randomUUID());
        savedReply.setPost(post);
        savedReply.setUser(supporterUser);
        savedReply.setContent(replyDTO.getContent());
        savedReply.setParentComment(parentComment);

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);
        when(connectionRepository.existsByMissionaryIdAndSupporterIdAndStatus(missionaryProfile.getId(), supporterUser.getId(), RequestStatus.APPROVED)).thenReturn(true);

        ResponseEntity<?> response = controller.addComment(post.getId(), replyDTO, supporterAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseBody = (CommentDTO) response.getBody();
        assert responseBody != null;
        assertEquals("Reply content", responseBody.getContent());
        assertEquals(parentCommentId, responseBody.getParentCommentId());
    }

    @Test
    void addComment_ParentCommentNotFound() {
        UUID fakeParentId = UUID.randomUUID();
        CommentDTO replyDTO = CommentDTO.builder()
                .content("Reply")
                .parentCommentId(fakeParentId)
                .build();

        when(commentRepository.findById(fakeParentId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.addComment(post.getId(), replyDTO, missionaryAuth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void addComment_ParentCommentPostMismatch() {
        UUID parentCommentId = UUID.randomUUID();
        Post differentPost = new Post();
        differentPost.setId(UUID.randomUUID());

        Comment parentComment = new Comment();
        parentComment.setId(parentCommentId);
        parentComment.setPost(differentPost); // Different post!

        CommentDTO replyDTO = CommentDTO.builder()
                .content("Reply")
                .parentCommentId(parentCommentId)
                .build();

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));

        ResponseEntity<?> response = controller.addComment(post.getId(), replyDTO, missionaryAuth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getComments_Success() {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPost(post);
        comment.setUser(supporterUser);
        comment.setContent("Hello");
        comment.setCreatedAt(OffsetDateTime.now());

        when(commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(comment));
        when(commentLikeRepository.countByCommentId(any(UUID.class))).thenReturn(0L);

        ResponseEntity<List<CommentDTO>> response = controller.getComments(post.getId(), supporterAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<CommentDTO> responseBody = response.getBody();
        assert responseBody != null;
        assertEquals(1, responseBody.size());
        assertEquals("Hello", responseBody.getFirst().getContent());
        assertEquals("John Doe", responseBody.getFirst().getUserName());
    }

    @Test
    void updateComment_Success() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);
        comment.setContent("Old content");

        CommentDTO updateDTO = CommentDTO.builder().content("New content").build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updateComment(post.getId(), commentId, updateDTO, missionaryAuth);
        CommentDTO responseDTO = (CommentDTO) response.getBody();
        assert responseDTO != null;

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New content", responseDTO.getContent());
        assertTrue(responseDTO.getEdited());
        org.junit.jupiter.api.Assertions.assertNotNull(responseDTO.getUpdatedAt());
    }

    @Test
    void updateComment_Forbidden() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);
        comment.setContent("Old content");

        CommentDTO updateDTO = CommentDTO.builder().content("New content").build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Attempting to update missionary's comment as a supporter
        ResponseEntity<?> response = controller.updateComment(post.getId(), commentId, updateDTO, supporterAuth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateComment_PostIdMismatch() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        CommentDTO updateDTO = CommentDTO.builder().content("New content").build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.updateComment(UUID.randomUUID(), commentId, updateDTO, missionaryAuth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateComment_DeletedComment() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);
        comment.setIsDeleted(true);

        CommentDTO updateDTO = CommentDTO.builder().content("New content").build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.updateComment(post.getId(), commentId, updateDTO, missionaryAuth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteComment_LeafSuccess() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);
        comment.setIsDeleted(false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(comment));

        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commentRepository).deleteAll(anyList());
    }

    @Test
    void deleteComment_ParentSuccess() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);
        comment.setIsDeleted(false);

        Comment child = new Comment();
        child.setId(UUID.randomUUID());
        child.setPost(post);
        child.setUser(supporterUser);
        child.setParentComment(comment);
        child.setIsDeleted(false); // Active child

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(comment, child));

        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseDTO = (CommentDTO) response.getBody();
        assert responseDTO != null;
        assertEquals("comment has been deleted", responseDTO.getContent());
        assertTrue(responseDTO.getIsDeleted());
        verify(commentRepository, never()).deleteAll(anyList());
    }

    @Test
    void deleteComment_Forbidden() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Attempting to delete missionary's comment as a supporter who is not post author
        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, supporterAuth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_PostAuthorSuccess() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(supporterUser);
        comment.setIsDeleted(false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(comment));

        // Post author (missionaryAuth) deleting a supporter's comment on their post
        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commentRepository).deleteAll(anyList());
    }

    @Test
    void deleteComment_RecursiveCleanupSuccess() {
        // P (deleted)
        //   C1 (active)
        UUID parentId = UUID.randomUUID();
        Comment parent = new Comment();
        parent.setId(parentId);
        parent.setPost(post);
        parent.setUser(missionaryUser);
        parent.setIsDeleted(true);
        parent.setContent("comment has been deleted");

        UUID childId = UUID.randomUUID();
        Comment child = new Comment();
        child.setId(childId);
        child.setPost(post);
        child.setUser(supporterUser);
        child.setParentComment(parent);
        child.setIsDeleted(false);

        when(commentRepository.findById(childId)).thenReturn(Optional.of(child));
        when(commentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(parent, child));

        // Deleting the last active child of a soft-deleted parent
        ResponseEntity<?> response = controller.deleteComment(post.getId(), childId, supporterAuth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        // Both parent and child should be deleted
        verify(commentRepository).deleteAll(anyList());
    }

    @Test
    void toggleLike_Success() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, missionaryUser.getId())).thenReturn(false, true);
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(1L);
        when(commentLikeRepository.findLatestLikes(eq(commentId), any(PageRequest.class))).thenReturn(List.of());

        ResponseEntity<?> response = controller.toggleLike(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseDTO = (CommentDTO) response.getBody();
        assert responseDTO != null;
        assertTrue(responseDTO.getLiked());
        assertEquals(1, responseDTO.getLikeCount());
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    @Test
    void toggleLike_UnlikeSuccess() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        CommentLike commentLike = new CommentLike();
        CommentLikeId commentLikeId = new CommentLikeId();
        commentLikeId.setCommentId(commentId);
        commentLikeId.setUserId(missionaryUser.getId());
        commentLike.setId(commentLikeId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, missionaryUser.getId())).thenReturn(true, false);
        when(commentLikeRepository.findById(commentLikeId)).thenReturn(Optional.of(commentLike));
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);
        when(commentLikeRepository.findLatestLikes(eq(commentId), any(PageRequest.class))).thenReturn(List.of());

        ResponseEntity<?> response = controller.toggleLike(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentDTO responseDTO = (CommentDTO) response.getBody();
        assert responseDTO != null;
        assertFalse(responseDTO.getLiked());
        assertEquals(0, responseDTO.getLikeCount());
        verify(commentLikeRepository).delete(commentLike);
    }
}
