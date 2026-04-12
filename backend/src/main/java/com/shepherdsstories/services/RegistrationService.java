package com.shepherdsstories.services;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.factories.UserFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               MissionaryProfileRepository missionaryProfileRepository,
                               SupporterProfileRepository supporterProfileRepository,
                               UserFactory userFactory,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationRequestDTO dto) {
        User user = userFactory.createBaseUser(dto);
        String secureHash = passwordEncoder.encode(dto.getPassword());
        user.setPasswordHash(secureHash);
        user.setIsLocked(false);
        user = userRepository.save(user);

        if (dto.getRole() == Role.MISSIONARY) {
            missionaryProfileRepository.save(userFactory.createMissionary(user, dto));
            return;
        }

        if (dto.getRole() == Role.SUPPORTER) {
            supporterProfileRepository.save(userFactory.createSupporter(user, dto));
            return;
        }

        throw new IllegalArgumentException("Unsupported role: " + dto.getRole());
    }
}
