package com.shepherdsstories.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionaryProfileDTO {
    private String missionaryName;
    private String locationRegion;
    private String biography;
    private String referenceNumber;
    private Boolean isReferenceDisabled;
}
