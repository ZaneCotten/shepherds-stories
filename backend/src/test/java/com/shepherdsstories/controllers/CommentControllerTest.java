package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.CommentDTO;
import com.shepherdsstories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private SupporterProfileRepository supporterProfileRepository;

    @Mock
    private ConnectionRepository connectionRepository;

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
        lenient().when(missionaryProfileRepository.findById(missionaryUser.getId())).thenReturn(Optional.of(missionaryProfile));
        lenient().when(supporterProfileRepository.findById(supporterUser.getId())).thenReturn(Optional.of(supporterProfile));
        lenient().when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
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
    void getComments_Success() {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPost(post);
        comment.setUser(supporterUser);
        comment.setContent("Hello");
        comment.setCreatedAt(OffsetDateTime.now());

        when(commentRepository.findAllByPostId(post.getId())).thenReturn(List.of(comment));

        ResponseEntity<List<CommentDTO>> response = controller.getComments(post.getId());

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
    void deleteComment_Success() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, missionaryAuth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_Forbidden() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPost(post);
        comment.setUser(missionaryUser);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Attempting to delete missionary's comment as a supporter
        ResponseEntity<?> response = controller.deleteComment(post.getId(), commentId, supporterAuth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commentRepository, never()).delete(any());
    }
}
