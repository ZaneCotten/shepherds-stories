package com.shepherdsstories.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {
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
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)
                        )
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().iterator().next().getAuthority();
                            String email = authentication.getName();

                            assert role != null;
                            if (role.equals("EMPTY")) {
                                // Redirect new Google users to pick their role
                                response.sendRedirect("http://localhost:5173/register/select-role?email=" + email);
                            } else {
                                // Known users go to their respective views
                                String redirectUrl = role.equals("MISSIONARY") ? "/missionary" : "/supporter";
                                response.sendRedirect("http://localhost:5173" + redirectUrl);
                            }
                        })
                )
                .build();
    }
}
