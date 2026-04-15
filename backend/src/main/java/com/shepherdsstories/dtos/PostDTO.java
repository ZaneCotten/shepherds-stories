package com.shepherdsstories.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private UUID id;
    private String title;
    private String content;
    private UUID authorId;
    private String authorName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long likeCount;
    private Boolean liked;
    private String lastLikerName;
    private List<MediaDTO> media;
}
