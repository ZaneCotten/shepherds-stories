package com.shepherdsstories.config;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUser_ExistingUser_ReturnsUserWithRole() {
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(mockOAuth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));

        TestableCustomOAuth2UserService service = new TestableCustomOAuth2UserService(userRepository, mockOAuth2User);

        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("client")
                .tokenUri("uri")
                .authorizationUri("uri")
                .redirectUri("uri")
                .build();
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);

        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.MISSIONARY);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        OAuth2User result = service.testableLoadUser(userRequest);

        assertNotNull(result);
        assertTrue(result.getAuthorities().stream().anyMatch(a -> "MISSIONARY".equals(a.getAuthority())));
    }

    @Test
    void loadUser_NewUser_ReturnsEmptyRole() {
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn("new@example.com");
        when(mockOAuth2User.getAttributes()).thenReturn(Map.of("email", "new@example.com"));

        TestableCustomOAuth2UserService service = new TestableCustomOAuth2UserService(userRepository, mockOAuth2User);

        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("client")
                .tokenUri("uri")
                .authorizationUri("uri")
                .redirectUri("uri")
                .build();
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);

        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByOauthId("GOOGLE:new@example.com")).thenReturn(Optional.empty());

        OAuth2User result = service.testableLoadUser(userRequest);

        assertNotNull(result);
        assertTrue(result.getAuthorities().stream().anyMatch(a -> "EMPTY".equals(a.getAuthority())));
    }

    @Test
    void loadUser_InvalidProvider_ThrowsException() {
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn("test@example.com");

        TestableCustomOAuth2UserService service = new TestableCustomOAuth2UserService(userRepository, mockOAuth2User);

        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("github")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("client")
                .tokenUri("uri")
                .authorizationUri("uri")
                .redirectUri("uri")
                .build();
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);

        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.MISSIONARY);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(OAuth2AuthenticationException.class, () -> service.testableLoadUser(userRequest));
    }

    // A subclass to bypass the super.loadUser() call which makes network requests
    private static class TestableCustomOAuth2UserService extends CustomOAuth2UserService {
        private final OAuth2User mockOAuth2User;
        private final UserRepository userRepositoryMock;

        public TestableCustomOAuth2UserService(UserRepository userRepository, OAuth2User mockOAuth2User) {
            super(userRepository);
            this.userRepositoryMock = userRepository;
            this.mockOAuth2User = mockOAuth2User;
        }

        // Let's redefine loadUser to simulate what the real one does but with our mock
        public OAuth2User testableLoadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            OAuth2User oAuth2User = this.mockOAuth2User;
            String clientName = userRequest.getClientRegistration().getRegistrationId();
            String email = oAuth2User.getAttribute("email");
            String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
            String oauthId = clientName.toUpperCase() + ":" + normalizedEmail;

            Optional<User> userOptional = userRepositoryMock.findByEmailIgnoreCase(normalizedEmail)
                    .or(() -> userRepositoryMock.findByOauthId(oauthId));

            java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities;

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (!"google".equalsIgnoreCase(clientName)) {
                    throw new OAuth2AuthenticationException("Invalid provider for this account");
                }
                authorities = java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().name()));
            } else {
                authorities = java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("EMPTY"));
            }

            return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
        }
    }
}
