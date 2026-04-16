package com.shepherdsstories.config;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        UserAuthConfig userAuthConfig = new UserAuthConfig();
        userDetailsService = userAuthConfig.userDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        String email = "USER@EXAMPLE.COM";
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole(Role.MISSIONARY);
        user.setIsLocked(false);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals("user@example.com", userDetails.getUsername());
        assertEquals("hashed_password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> "MISSIONARY".equals(a.getAuthority())));
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_UserLocked_ReturnsUserDetailsWithLockedAccount() {
        String email = "locked@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed_password");
        user.setRole(Role.SUPPORTER);
        user.setIsLocked(true);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
    }
}
