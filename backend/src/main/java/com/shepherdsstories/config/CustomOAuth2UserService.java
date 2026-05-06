package com.shepherdsstories.config;

import com.shepherdsstories.entities.User;
import com.shepherdsstories.data.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String clientName = userRequest.getClientRegistration().getRegistrationId();
        String email = oAuth2User.getAttribute("email");
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String oauthId = clientName.toUpperCase() + ":" + normalizedEmail;

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .or(() -> userRepository.findByOauthId(oauthId));

        Set<GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!"google".equalsIgnoreCase(clientName)) {
                throw new OAuth2AuthenticationException("Invalid provider for this account");
            }

            authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        } else {
            authorities.add(new SimpleGrantedAuthority("EMPTY"));
        }

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
    }
}
