package com.shepherdsstories.config;

import com.shepherdsstories.entities.User;
import com.shepherdsstories.data.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String clientName = userRequest.getClientRegistration().getRegistrationId();
        String email = oidcUser.getEmail();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String oauthId = clientName.toUpperCase() + ":" + normalizedEmail;

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .or(() -> userRepository.findByOauthId(oauthId));

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!"google".equalsIgnoreCase(clientName)) {
                throw new OAuth2AuthenticationException("Invalid provider for this account");
            }

            authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        } else {
            authorities.add(new SimpleGrantedAuthority("EMPTY"));
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
