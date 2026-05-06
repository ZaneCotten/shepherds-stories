package com.shepherdsstories.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrayerRequestDTO {
    private UUID id;
    private String title;
    private String content;
    @JsonProperty("isAnswered")
    private boolean answered;
    private LocalDateTime createdAt;
    private UUID missionaryId;
    private String missionaryName;
}
