package com.shepherdsstories.config;

import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.services.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String APPLICATION_JSON = "application/json";
    private static final String EMAIL_LITERAL = "email";

    private static String successBody(Authentication authentication) {
        String username = authentication.getName();
        String id = "";

        if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails userDetails) {
            id = userDetails.getId().toString();
        }

        if (authentication.getAuthorities().isEmpty()) {
            return String.format("{\"username\":\"%s\",\"id\":\"%s\",\"role\":\"NO ROLE FOUND\"}", username, id);
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
        return String.format("{\"username\":\"%s\",\"id\":\"%s\",\"role\":\"%s\"}", username, id, role);

    }

    private static String asString(OAuth2AuthenticationToken authToken, String key) {
        if (authToken == null) {
            return null;
        }
        return Optional.ofNullable(authToken.getPrincipal())
                .map(principal -> principal.getAttribute(key))
                .map(Object::toString)
                .map(String::trim)
                .orElse(null);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler(UserRepository userRepository, SecurityContextRepository securityContextRepository, AuditLogService auditLogService) {
        return (request, response, authentication) -> {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            String provider = authToken.getAuthorizedClientRegistrationId().toUpperCase();
            String email = firstNonBlank(
                    asString(authToken, EMAIL_LITERAL),
                    authentication.getName()
            );
            String normalizedEmail = normalizeEmail(email);

            if (normalizedEmail.isBlank()) {
                response.sendRedirect("http://localhost:5173/login");
                return;
            }

            String name = asString(authToken, "name");
            String givenName = asString(authToken, "given_name");
            String familyName = asString(authToken, "family_name");
            String oauthId = provider + ":" + normalizedEmail;

            Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .or(() -> userRepository.findByOauthId(oauthId));
            if (userOptional.isEmpty()) {
                auditLogService.log("LOGIN_OAUTH2_NEW_USER", normalizedEmail, null, "Provider: " + provider + " - Redirecting to role selection", request.getRemoteAddr());
                String url = String.format(
                        "http://localhost:5173/register/select-role?email=%s&provider=%s&name=%s&given_name=%s&family_name=%s",
                        encode(normalizedEmail),
                        encode(provider),
                        encode(nullSafe(name)),
                        encode(nullSafe(givenName)),
                        encode(nullSafe(familyName))
                );
                response.sendRedirect(url);
            } else {
                User user = userOptional.get();
                auditLogService.log("LOGIN_OAUTH2", normalizedEmail, user.getId(), "Provider: " + provider, request.getRemoteAddr());
                String role = user.getRole().name();
                String url = String.format("http://localhost:5173/oauth/callback?username=%s&role=%s&id=%s",
                        encode(normalizedEmail), encode(role), encode(user.getId().toString()));
                SecurityContext context = SecurityContextHolder.getContext();
                securityContextRepository.saveContext(context, request, response);
                response.sendRedirect(url);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler formLoginSuccessHandler(SecurityContextRepository securityContextRepository, AuditLogService auditLogService) {
        return (request, response, authentication) -> {
            UUID userId = null;
            if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
                userId = details.getId();
            }
            auditLogService.log("LOGIN", authentication.getName(), userId, "Success", request.getRemoteAddr());
            SecurityContext context = SecurityContextHolder.getContext();
            securityContextRepository.saveContext(context, request, response);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(APPLICATION_JSON);
            response.getWriter().write(successBody(authentication));
        };
    }

    @Bean
    public AuthenticationFailureHandler formLoginFailureHandler(AuditLogService auditLogService) {
        return (request, response, _) -> {
            String email = request.getParameter(EMAIL_LITERAL);
            auditLogService.log("LOGIN_FAILED", email, null, "Invalid credentials", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(APPLICATION_JSON);
            response.getWriter().write("{\"error\":\"Invalid email or password\"}");
        };
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService, CustomOidcUserService customOidcUserService, AuthenticationSuccessHandler oauth2SuccessHandler, AuthenticationSuccessHandler formLoginSuccessHandler, AuthenticationFailureHandler formLoginFailureHandler, SecurityContextRepository securityContextRepository, AuditLogService auditLogService) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**", "/oauth2/**").permitAll();
                    auth.requestMatchers("/api/missionary/**").hasAuthority("MISSIONARY");
                    auth.requestMatchers("/api/supporter/**").hasAnyAuthority("SUPPORTER", "MISSIONARY");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, _) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(APPLICATION_JSON);
                                response.getWriter().write("{\"error\":\"Unauthorized access\"}");
                            } else {
                                response.sendRedirect("/login");
                            }
                        }))
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter(EMAIL_LITERAL)
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                UUID userId = null;
                                if (authentication.getPrincipal() instanceof UserAuthConfig.AppUserDetails details) {
                                    userId = details.getId();
                                }
                                auditLogService.log("LOGOUT", authentication.getName(), userId, "Success", request.getRemoteAddr());
                            }
                            response.setStatus(HttpServletResponse.SC_OK);
                        }))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .successHandler(oauth2SuccessHandler)
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,    // salt length in bytes
                32,    // hash length in bytes
                1,     // parallelism (threads)
                16384, // memory cost in KiB (16 MB)
                2      // iterations
        );
    }
}
