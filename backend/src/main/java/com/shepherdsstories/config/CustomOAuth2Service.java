package com.shepherdsstories.config;

import com.shepherdsstories.data.entities.User;
import com.shepherdsstories.data.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        Optional<User> userOptional = userRepository.findByEmail(email);

        List<SimpleGrantedAuthority> authorities;

        if (userOptional.isPresent()) {
            // This gets the string value of your enum (e.g., "MISSIONARY")
            String roleName = userOptional.get().getRole().name();
            authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));
        } else {
            // Temporary string for the redirect bridge
            authorities = Collections.singletonList(new SimpleGrantedAuthority("EMPTY"));
        }

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
    }
}