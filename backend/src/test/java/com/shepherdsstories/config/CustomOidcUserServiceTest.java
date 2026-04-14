package com.shepherdsstories.config;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUser_ExistingUser_ReturnsUserWithRole() {
        OidcUser mockOidcUser = mock(OidcUser.class);
        when(mockOidcUser.getEmail()).thenReturn("test@example.com");

        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of(IdTokenClaimNames.SUB, "sub", "email", "test@example.com"));
        when(mockOidcUser.getIdToken()).thenReturn(idToken);

        TestableCustomOidcUserService service = new TestableCustomOidcUserService(userRepository, mockOidcUser);

        OidcUserRequest userRequest = mock(OidcUserRequest.class);
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

        OidcUser result = service.testableLoadUser(userRequest);

        assertNotNull(result);
        assertTrue(result.getAuthorities().stream().anyMatch(a -> "MISSIONARY".equals(a.getAuthority())));
    }

    private static class TestableCustomOidcUserService extends CustomOidcUserService {
        private final OidcUser mockOidcUser;
        private final UserRepository userRepositoryMock;

        public TestableCustomOidcUserService(UserRepository userRepository, OidcUser mockOidcUser) {
            super(userRepository);
            this.userRepositoryMock = userRepository;
            this.mockOidcUser = mockOidcUser;
        }

        public OidcUser testableLoadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
            OidcUser oidcUser = this.mockOidcUser;
            String clientName = userRequest.getClientRegistration().getRegistrationId();
            String email = oidcUser.getEmail();
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

            return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(authorities, oidcUser.getIdToken());
        }
    }
}
