package com.shepherdsstories.services;

import com.shepherdsstories.data.enums.AuthProvider;
import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.InviteCodeRepository;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.data.repositories.UserRepository;
import com.shepherdsstories.dtos.RegistrationRequestDTO;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import com.shepherdsstories.factories.UserFactory;
import com.shepherdsstories.utils.CodeGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.shepherdsstories.utils.ValidationConstants.REF_CODE_LENGTH;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistrationService(UserRepository userRepository,
                               MissionaryProfileRepository missionaryProfileRepository,
                               SupporterProfileRepository supporterProfileRepository,
                               InviteCodeRepository inviteCodeRepository,
                               UserFactory userFactory,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.userRepository = userRepository;
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void register(RegistrationRequestDTO dto) {
        // Ensure profile picture is NOT set for local registration initially
        dto.setProfilePictureUrl(null);

        User user = userFactory.createBaseUser(dto);
        String secureHash = passwordEncoder.encode(dto.getPassword());
        user.setPasswordHash(secureHash);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setIsEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(OffsetDateTime.now().plusDays(1));
        user = userRepository.save(user);
        saveRoleProfile(dto, user);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
    }

    @Transactional
    public void registerSocial(RegistrationRequestDTO dto, String oauthId, AuthProvider authProvider) {
        User user = userFactory.createBaseUser(dto);
        user.setAuthProvider(authProvider);
        user.setOauthId(oauthId);
        user.setIsEmailVerified(true);
        user = userRepository.save(user);
        saveRoleProfile(dto, user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(OffsetDateTime.now().plusDays(1));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
    }

    private void saveRoleProfile(RegistrationRequestDTO dto, User user) {
        if (dto.getRole() == Role.MISSIONARY) {
            String referenceNumber = CodeGenerator.generateReference(REF_CODE_LENGTH);
            MissionaryProfile profile = userFactory.createMissionary(user, dto, referenceNumber);
            missionaryProfileRepository.save(profile);
            inviteCodeRepository.save(userFactory.createInviteCode(profile, referenceNumber));
            return;
        }

        if (dto.getRole() == Role.SUPPORTER) {
            supporterProfileRepository.save(userFactory.createSupporter(user, dto));
            return;
        }

        throw new IllegalArgumentException("Unsupported role: " + dto.getRole());
    }
}
