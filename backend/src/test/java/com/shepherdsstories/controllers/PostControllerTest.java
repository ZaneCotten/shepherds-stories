package com.shepherdsstories.controllers;

import com.shepherdsstories.data.enums.RequestStatus;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.PostLikeRepository;
import com.shepherdsstories.data.repositories.PostRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.PostDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.Post;
import com.shepherdsstories.entities.PostLike;
import com.shepherdsstories.entities.SupporterProfile;
import com.shepherdsstories.entities.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MissionaryProfileRepository missionaryProfileRepository;

    @Mock
    private SupporterProfileRepository supporterProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostController controller;

    private User missionaryUser;
    private MissionaryProfile missionaryProfile;
    private Authentication auth;

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

        auth = mock(Authentication.class);
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("missionary@test.com");

        lenient().when(userRepository.findByEmailIgnoreCase("missionary@test.com")).thenReturn(Optional.of(missionaryUser));
    }

    @Test
    void createPost_Success() {
        when(missionaryProfileRepository.findById(missionaryUser.getId())).thenReturn(Optional.of(missionaryProfile));

        PostDTO postDTO = PostDTO.builder()
                .title("Test Title")
                .content("Test Content")
                .build();

        Post savedPost = new Post();
        savedPost.setId(UUID.randomUUID());
        savedPost.setTitle(postDTO.getTitle());
        savedPost.setContent(postDTO.getContent());
        savedPost.setAuthor(missionaryProfile);
        savedPost.setCreatedAt(OffsetDateTime.now());

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        ResponseEntity<PostDTO> response = controller.createPost(postDTO, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Title", response.getBody().getTitle());
        assertEquals("Test Missionary", response.getBody().getAuthorName());
    }

    @Test
    void getMyPosts_Success() {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setTitle("Title");
        post.setContent("Content");
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        when(postRepository.findAllByAuthorIdWithAuthor(missionaryUser.getId())).thenReturn(List.of(post));

        ResponseEntity<List<PostDTO>> response = controller.getMyPosts(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Title", response.getBody().get(0).getTitle());
    }

    @Test
    void getFeed_Success() {
        User supporterUser = new User();
        supporterUser.setId(UUID.randomUUID());
        supporterUser.setEmail("supporter@test.com");
        supporterUser.setRole(Role.SUPPORTER);

        Authentication supporterAuth = mock(Authentication.class);
        when(supporterAuth.isAuthenticated()).thenReturn(true);
        when(supporterAuth.getName()).thenReturn("supporter@test.com");
        when(userRepository.findByEmailIgnoreCase("supporter@test.com")).thenReturn(Optional.of(supporterUser));

        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setTitle("Missionary Update");
        post.setContent("This is an update");
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        when(postRepository.findAllForSupporter(supporterUser.getId(), RequestStatus.APPROVED)).thenReturn(List.of(post));

        ResponseEntity<List<PostDTO>> response = controller.getFeed(supporterAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Missionary Update", response.getBody().get(0).getTitle());
        assertEquals("Test Missionary", response.getBody().get(0).getAuthorName());
    }

    @Test
    void updatePost_Success() {
        UUID postId = UUID.randomUUID();
        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setTitle("Old Title");
        existingPost.setContent("Old Content");
        existingPost.setAuthor(missionaryProfile);
        existingPost.setCreatedAt(OffsetDateTime.now().minusDays(1));
        existingPost.setUpdatedAt(existingPost.getCreatedAt());

        PostDTO updateDTO = PostDTO.builder()
                .title("New Title")
                .content("New Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<PostDTO> response = controller.updatePost(postId, updateDTO, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New Title", response.getBody().getTitle());
        assertEquals("New Content", response.getBody().getContent());
        // Verify creation time is preserved (within some tolerance or just not null)
        assertEquals(existingPost.getCreatedAt(), response.getBody().getCreatedAt());
    }

    @Test
    void toggleLike_NewLike_Success() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        User liker = new User();
        liker.setId(UUID.randomUUID());
        liker.setRole(Role.SUPPORTER);
        PostLike like = new PostLike();
        like.setUser(liker);

        SupporterProfile likerProfile = new SupporterProfile();
        likerProfile.setFirstName("Jane");
        likerProfile.setLastName("Doe");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(false);
        when(postLikeRepository.countByPostId(postId)).thenReturn(1L);
        when(postLikeRepository.existsByPostIdAndUserId(eq(postId), any())).thenReturn(true);
        when(postLikeRepository.findLatestLikes(eq(postId), any())).thenReturn(List.of(like));
        when(supporterProfileRepository.findById(liker.getId())).thenReturn(Optional.of(likerProfile));

        ResponseEntity<PostDTO> response = controller.toggleLike(postId, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getLikeCount());
        assertTrue(response.getBody().isLiked());
        assertEquals("Jane Doe", response.getBody().getLastLikerName());
        verify(postLikeRepository, times(1)).save(any(PostLike.class));
    }

    @Test
    void getFeed_WithMultipleLikes_LastLikerNameCorrect() {
        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());
        supporter1.setEmail("supporter1@test.com");
        supporter1.setRole(Role.SUPPORTER);

        User supporter2 = new User();
        supporter2.setId(UUID.randomUUID());
        supporter2.setEmail("supporter2@test.com");
        supporter2.setRole(Role.SUPPORTER);

        SupporterProfile profile2 = new SupporterProfile();
        profile2.setFirstName("John");
        profile2.setLastName("Doe");

        Authentication auth1 = mock(Authentication.class);
        lenient().when(auth1.isAuthenticated()).thenReturn(true);
        lenient().when(auth1.getName()).thenReturn("supporter1@test.com");
        lenient().when(userRepository.findByEmailIgnoreCase("supporter1@test.com")).thenReturn(Optional.of(supporter1));

        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        PostLike like2 = new PostLike();
        like2.setUser(supporter2);

        when(postRepository.findAllForSupporter(supporter1.getId(), RequestStatus.APPROVED)).thenReturn(List.of(post));
        when(postLikeRepository.countByPostId(post.getId())).thenReturn(2L);
        when(postLikeRepository.existsByPostIdAndUserId(post.getId(), supporter1.getId())).thenReturn(true);

        PostLike like1 = new PostLike();
        like1.setUser(supporter1);
        when(postLikeRepository.findLatestLikes(eq(post.getId()), any())).thenReturn(List.of(like1, like2));

        ResponseEntity<List<PostDTO>> response = controller.getFeed(auth1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        PostDTO dto = response.getBody().get(0);
        assertEquals(2, dto.getLikeCount());
        assertTrue(dto.isLiked());
        assertEquals("you", dto.getLastLikerName());
    }

    @Test
    void getFeed_WithMultipleLikes_SomeoneElseRecent() {
        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());
        supporter1.setEmail("supporter1@test.com");
        supporter1.setRole(Role.SUPPORTER);

        User supporter2 = new User();
        supporter2.setId(UUID.randomUUID());
        supporter2.setEmail("supporter2@test.com");
        supporter2.setRole(Role.SUPPORTER);

        SupporterProfile profile2 = new SupporterProfile();
        profile2.setFirstName("John");
        profile2.setLastName("Doe");

        Authentication auth1 = mock(Authentication.class);
        lenient().when(auth1.isAuthenticated()).thenReturn(true);
        lenient().when(auth1.getName()).thenReturn("supporter1@test.com");
        lenient().when(userRepository.findByEmailIgnoreCase("supporter1@test.com")).thenReturn(Optional.of(supporter1));

        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        PostLike like1 = new PostLike();
        like1.setUser(supporter1);
        PostLike like2 = new PostLike();
        like2.setUser(supporter2);

        when(postRepository.findAllForSupporter(supporter1.getId(), RequestStatus.APPROVED)).thenReturn(List.of(post));
        when(postLikeRepository.countByPostId(post.getId())).thenReturn(2L);
        when(postLikeRepository.existsByPostIdAndUserId(post.getId(), supporter1.getId())).thenReturn(true);

        // like2 is most recent
        when(postLikeRepository.findLatestLikes(eq(post.getId()), any())).thenReturn(List.of(like2, like1));
        when(supporterProfileRepository.findById(supporter2.getId())).thenReturn(Optional.of(profile2));

        ResponseEntity<List<PostDTO>> response = controller.getFeed(auth1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PostDTO dto = response.getBody().get(0);
        assertEquals("John Doe", dto.getLastLikerName());
    }

    @Test
    void toggleLike_RemoveLike_Success() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthor(missionaryProfile);
        post.setCreatedAt(OffsetDateTime.now());

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsById(any())).thenReturn(true);
        when(postLikeRepository.countByPostId(postId)).thenReturn(0L);
        when(postLikeRepository.existsByPostIdAndUserId(eq(postId), any())).thenReturn(false);

        ResponseEntity<PostDTO> response = controller.toggleLike(postId, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getLikeCount());
        assertFalse(response.getBody().isLiked());
        verify(postLikeRepository, times(1)).deleteById(any());
    }

    @Test
    void updatePost_Forbidden() {
        UUID postId = UUID.randomUUID();
        MissionaryProfile otherMissionary = new MissionaryProfile();
        otherMissionary.setId(UUID.randomUUID());

        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setAuthor(otherMissionary);

        PostDTO updateDTO = PostDTO.builder()
                .title("New Title")
                .content("New Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        ResponseEntity<PostDTO> response = controller.updatePost(postId, updateDTO, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(postRepository, never()).save(any());
    }
}
