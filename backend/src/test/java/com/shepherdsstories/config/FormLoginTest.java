package com.shepherdsstories.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormLoginTest {

    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig = new SecurityConfig();
        successHandler = securityConfig.formLoginSuccessHandler();
        failureHandler = securityConfig.formLoginFailureHandler();
    }

    @Test
    void formLogin_Success_ReturnsJsonWithRole() throws Exception {
        String email = "test@example.com";
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("MISSIONARY");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "password", Collections.singletonList(authority));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String content = response.getContentAsString();
        assertEquals(String.format("{\"username\":\"%s\",\"id\":\"\",\"role\":\"MISSIONARY\"}", email), content);
    }

    @Test
    void formLogin_Supporter_Success_ReturnsJsonWithRole() throws Exception {
        String email = "supporter@example.com";
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("SUPPORTER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "password", Collections.singletonList(authority));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String content = response.getContentAsString();
        assertEquals(String.format("{\"username\":\"%s\",\"id\":\"\",\"role\":\"SUPPORTER\"}", email), content);
    }

    @Test
    void formLogin_Success_ReturnsJsonWithNoRoleFound() throws Exception {
        String email = "test@example.com";
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "password", Collections.emptyList());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String content = response.getContentAsString();
        assertEquals(String.format("{\"username\":\"%s\",\"id\":\"\",\"role\":\"NO ROLE FOUND\"}", email), content);
    }

    @Test
    void formLogin_Failure_Returns401AndErrorJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String content = response.getContentAsString();
        assertEquals("{\"error\":\"Invalid email or password\"}", content);
    }
}
