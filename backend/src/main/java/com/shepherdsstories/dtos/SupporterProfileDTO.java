package com.shepherdsstories.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupporterProfileDTO extends UserProfileDTO {
    private String firstName;
    private String lastName;
    private Boolean isVerified;
}
