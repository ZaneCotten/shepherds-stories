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
public class CommentDTO {
    private UUID id;
    private UUID postId;
    private UUID userId;
    private String userName;
    private String content;
    private UUID parentCommentId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean edited;
}
