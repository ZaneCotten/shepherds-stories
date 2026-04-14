package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.*;
import com.shepherdsstories.dtos.CommentDTO;
import com.shepherdsstories.entities.*;
import com.shepherdsstories.exceptions.ResourceNotFoundException;
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
    private SupporterProfile supporterProfile;
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

        supporterProfile = new SupporterProfile();
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
        savedComment.setCreatedAt(OffsetDateTime.now());

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        ResponseEntity<CommentDTO> response = controller.addComment(post.getId(), commentDTO, missionaryAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Missionary comment", response.getBody().getContent());
        assertEquals("Test Missionary", response.getBody().getUserName());
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
        when(connectionRepository.existsByMissionaryIdAndSupporterId(missionaryProfile.getId(), supporterUser.getId())).thenReturn(true);

        ResponseEntity<CommentDTO> response = controller.addComment(post.getId(), commentDTO, supporterAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Supporter comment", response.getBody().getContent());
        assertEquals("John Doe", response.getBody().getUserName());
    }

    @Test
    void addComment_PostNotFound() {
        CommentDTO commentDTO = CommentDTO.builder().content("Comment").build();
        UUID fakePostId = UUID.randomUUID();
        when(postRepository.findById(fakePostId)).thenReturn(Optional.empty());

        ResponseEntity<CommentDTO> response = controller.addComment(fakePostId, commentDTO, supporterAuth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void addComment_SupporterForbidden() {
        CommentDTO commentDTO = CommentDTO.builder().content("No permission").build();
        when(connectionRepository.existsByMissionaryIdAndSupporterId(missionaryProfile.getId(), supporterUser.getId())).thenReturn(false);

        ResponseEntity<CommentDTO> response = controller.addComment(post.getId(), commentDTO, supporterAuth);

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
        assertEquals(1, response.getBody().size());
        assertEquals("Hello", response.getBody().get(0).getContent());
        assertEquals("John Doe", response.getBody().get(0).getUserName());
    }
}
