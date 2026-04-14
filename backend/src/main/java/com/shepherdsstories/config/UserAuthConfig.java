package com.shepherdsstories.config;

import com.shepherdsstories.data.repositories.UserRepository;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Configuration
public class UserAuthConfig {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(appUser -> new AppUserDetails(
                        appUser.getId(),
                        appUser.getEmail(),
                        appUser.getPasswordHash(),
                        appUser.getIsLocked() == null || !appUser.getIsLocked(),
                        Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Getter
    public static class AppUserDetails extends User {
        private final UUID id;

        public AppUserDetails(UUID id, String username, String password, boolean enabled, Collection<? extends GrantedAuthority> authorities) {
            super(username, password, enabled, true, true, true, authorities);
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AppUserDetails that)) return false;
            if (!super.equals(o)) return false;
            return java.util.Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(super.hashCode(), id);
        }
    }
}
