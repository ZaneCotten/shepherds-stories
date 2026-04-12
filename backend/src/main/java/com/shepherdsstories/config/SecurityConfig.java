package com.shepherdsstories.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {
    private static String successBody(Authentication authentication) {
        String username = authentication.getName();

        if (authentication.getAuthorities().isEmpty()) {
            return String.format("{\"username\":\"%s\",\"role\":\"NO ROLE FOUND\"}", username);
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        return String.format("{\"username\":\"%s\",\"role\":\"%s\"}", username, role);

    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**", "/oauth2/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write(successBody(authentication));
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Invalid email or password\"}");
                        }))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)))
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().iterator().next().getAuthority();
                            String email = authentication.getName();

                            // In OAuth2AuthenticationToken, we can find which provider was used
                            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
                            String provider = authToken.getAuthorizedClientRegistrationId().toUpperCase(); // "GOOGLE"

                            assert role != null;
                            if (role.equals("EMPTY")) {
                                // Redirect to selection, passing BOTH email and provider
                                String url = String.format("http://localhost:5173/register/select-role?email=%s&provider=%s",
                                        email, provider);
                                response.sendRedirect(url);
                            } else {
                                // Logged in successfully
                                response.sendRedirect("http://localhost:5173/" + role.toLowerCase() + "view");
                            }
                        })
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
