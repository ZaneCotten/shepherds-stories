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
public class MissionaryProfileDTO extends UserProfileDTO {
    private String missionaryName;
    private String locationRegion;
    private String biography;
    private String referenceNumber;
    private Boolean isReferenceDisabled;
}
