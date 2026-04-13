package com.shepherdsstories.config;

import com.shepherdsstories.data.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

@Configuration
public class UserAuthConfig {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(appUser -> new User(
                        appUser.getEmail(),
                        appUser.getPasswordHash(),
                        appUser.getIsLocked() == null || !appUser.getIsLocked(),
                        true,
                        true,
                        true,
                        Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
