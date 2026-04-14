package com.shepherdsstories.dtos;

import com.shepherdsstories.data.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private UUID id;
    private String url; // Presigned URL for viewing
    private String fileName;
    private MediaType mediaType;
    private Integer orderNumber;
    private String s3Key; // Useful for tracking uploads
}
