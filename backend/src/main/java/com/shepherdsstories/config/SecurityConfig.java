package com.shepherdsstories.config;

import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {
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
    public org.springframework.security.web.authentication.AuthenticationSuccessHandler oauth2SuccessHandler(UserRepository userRepository) {
        return (_, response, authentication) -> {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            String provider = authToken.getAuthorizedClientRegistrationId().toUpperCase();
            String email = firstNonBlank(
                    asString(authToken, "email"),
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
            String picture = asString(authToken, "picture");
            String oauthId = provider + ":" + normalizedEmail;

            Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .or(() -> userRepository.findByOauthId(oauthId));
            if (userOptional.isEmpty()) {
                String url = String.format(
                        "http://localhost:5173/register/select-role?email=%s&provider=%s&name=%s&given_name=%s&family_name=%s&picture=%s",
                        encode(normalizedEmail),
                        encode(provider),
                        encode(nullSafe(name)),
                        encode(nullSafe(givenName)),
                        encode(nullSafe(familyName)),
                        encode(nullSafe(picture))
                );
                response.sendRedirect(url);
            } else {
                User user = userOptional.get();
                String role = user.getRole().name();
                String url = String.format("http://localhost:5173/oauth/callback?username=%s&role=%s&id=%s",
                        encode(normalizedEmail), encode(role), encode(user.getId().toString()));
                response.sendRedirect(url);
            }
        };
    }

    @Bean
    public org.springframework.security.web.authentication.AuthenticationSuccessHandler formLoginSuccessHandler() {
        return (_, response, authentication) -> {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(successBody(authentication));
        };
    }

    @Bean
    public org.springframework.security.web.authentication.AuthenticationFailureHandler formLoginFailureHandler() {
        return (_, response, _) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid email or password\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow the frontend origin
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService, CustomOidcUserService customOidcUserService, org.springframework.security.web.authentication.AuthenticationSuccessHandler oauth2SuccessHandler, org.springframework.security.web.authentication.AuthenticationSuccessHandler formLoginSuccessHandler, org.springframework.security.web.authentication.AuthenticationFailureHandler formLoginFailureHandler, SecurityContextRepository securityContextRepository) {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .cors(withDefaults())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, _) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        }))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**", "/oauth2/**").permitAll();
                    auth.requestMatchers("/api/missionary/**").hasAuthority("MISSIONARY");
                    auth.requestMatchers("/api/supporter/**").hasAnyAuthority("SUPPORTER", "MISSIONARY");
                    auth.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((_, response, _) ->
                                response.setStatus(HttpServletResponse.SC_OK)))
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
