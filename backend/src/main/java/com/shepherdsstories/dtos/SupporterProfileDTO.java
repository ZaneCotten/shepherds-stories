package com.shepherdsstories.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SupporterProfileDTO extends UserProfileDTO {
    private String firstName;
    private String lastName;
    private Boolean isVerified;
}
