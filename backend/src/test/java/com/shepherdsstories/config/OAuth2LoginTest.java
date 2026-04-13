package com.shepherdsstories.config;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OAuth2LoginTest {

    private UserRepository userRepository;
    private AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        SecurityConfig securityConfig = new SecurityConfig();
        successHandler = securityConfig.oauth2SuccessHandler(userRepository);
    }

    @Test
    void onAuthenticationSuccess_ExistingUser_RedirectsToCallback() throws Exception {
        String email = "existing@example.com";
        User user = new User();
        user.setEmail(email);
        user.setRole(Role.MISSIONARY);

        // When finding by email OR by oauthId (GOOGLE:existing@example.com)
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(userRepository.findByOauthId("GOOGLE:" + email)).thenReturn(Optional.of(user));

        OAuth2User principal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("MISSIONARY")),
                Map.of("email", email, "name", "Test User"),
                "email"
        );

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        String rawRedirectedUrl = response.getRedirectedUrl();
        assertNotNull(rawRedirectedUrl, "Redirected URL should not be null");
        String redirectedUrl = URLDecoder.decode(rawRedirectedUrl, StandardCharsets.UTF_8);
        assertTrue(redirectedUrl.contains("/oauth/callback"), "URL should contain /oauth/callback, but was: " + redirectedUrl);
        assertTrue(redirectedUrl.contains("username=" + email), "URL should contain username=" + email);
        assertTrue(redirectedUrl.contains("role=MISSIONARY"), "URL should contain role=MISSIONARY");
    }

    @Test
    void onAuthenticationSuccess_ExistingSupporter_RedirectsToCallback() throws Exception {
        String email = "supporter@example.com";
        User user = new User();
        user.setEmail(email);
        user.setRole(Role.SUPPORTER);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        OAuth2User principal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("SUPPORTER")),
                Map.of("email", email, "name", "Supporter User"),
                "email"
        );

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        String rawRedirectedUrl = response.getRedirectedUrl();
        assertNotNull(rawRedirectedUrl, "Redirected URL should not be null");
        String redirectedUrl = URLDecoder.decode(rawRedirectedUrl, StandardCharsets.UTF_8);
        assertTrue(redirectedUrl.contains("/oauth/callback"), "URL should contain /oauth/callback");
        assertTrue(redirectedUrl.contains("username=" + email), "URL should contain username=" + email);
        assertTrue(redirectedUrl.contains("role=SUPPORTER"), "URL should contain role=SUPPORTER");
    }

    @Test
    void onAuthenticationSuccess_NewUser_RedirectsToRoleSelection() throws Exception {
        String email = "new@example.com";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(userRepository.findByOauthId("GOOGLE:" + email)).thenReturn(Optional.empty());

        OAuth2User principal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("EMPTY")),
                Map.of("email", email, "name", "New User", "given_name", "New", "family_name", "User"),
                "email"
        );

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        String rawRedirectedUrl = response.getRedirectedUrl();
        assertNotNull(rawRedirectedUrl, "Redirected URL should not be null");
        String redirectedUrl = URLDecoder.decode(rawRedirectedUrl, StandardCharsets.UTF_8);
        assertTrue(redirectedUrl.contains("/register/select-role"), "URL should contain /register/select-role");
        assertTrue(redirectedUrl.contains("email=" + email), "URL should contain email=" + email);
        assertTrue(redirectedUrl.contains("provider=GOOGLE"), "URL should contain provider=GOOGLE");
    }

    @Test
    void onAuthenticationSuccess_MissingEmail_RedirectsToLogin() throws Exception {
        // Mock principal to return null for "email" and null for name
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn(null);
        when(principal.getName()).thenReturn(""); // Empty name

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal,
                Collections.emptyList(),
                "google"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        String redirectedUrl = response.getRedirectedUrl();
        assertEquals("http://localhost:5173/login", redirectedUrl, "Should redirect to login when email is missing");
    }
}
