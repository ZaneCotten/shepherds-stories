package com.shepherdsstories.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannedSupporterDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private OffsetDateTime bannedAt;
}
