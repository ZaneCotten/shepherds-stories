package com.shepherdsstories.services;

import com.shepherdsstories.data.enums.Role;
import com.shepherdsstories.data.repositories.MissionaryProfileRepository;
import com.shepherdsstories.data.repositories.SupporterProfileRepository;
import com.shepherdsstories.entities.MissionaryProfile;
import com.shepherdsstories.entities.User;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final MissionaryProfileRepository missionaryProfileRepository;
    private final SupporterProfileRepository supporterProfileRepository;

    public ProfileService(MissionaryProfileRepository missionaryProfileRepository,
                          SupporterProfileRepository supporterProfileRepository) {
        this.missionaryProfileRepository = missionaryProfileRepository;
        this.supporterProfileRepository = supporterProfileRepository;
    }

    public String getUserDisplayName(User user) {
        if (user.getRole() == Role.MISSIONARY) {
            return missionaryProfileRepository.findById(user.getId())
                    .filter(mp -> (mp.getMissionaryName() != null && !mp.getMissionaryName().isEmpty()))
                    .map(MissionaryProfile::getMissionaryName)
                    .orElse(user.getEmail());
        } else if (user.getRole() == Role.SUPPORTER) {
            return supporterProfileRepository.findById(user.getId())
                    .map(sp -> {
                        String name = ((sp.getFirstName() != null ? sp.getFirstName() : "") + " " + (sp.getLastName() != null ? sp.getLastName() : "")).trim();
                        return name.isEmpty() ? user.getEmail() : name;
                    })
                    .orElse(user.getEmail());
        }
        return user.getEmail();
    }

    public String getUserName(User user) {
        if (user.getRole() == Role.MISSIONARY) {
            return missionaryProfileRepository.findById(user.getId())
                    .map(MissionaryProfile::getMissionaryName)
                    .orElse("Unknown Missionary");
        } else if (user.getRole() == Role.SUPPORTER) {
            return supporterProfileRepository.findById(user.getId())
                    .map(sp -> sp.getFirstName() + " " + sp.getLastName())
                    .orElse("Unknown Supporter");
        }
        return "Unknown User";
    }
}
