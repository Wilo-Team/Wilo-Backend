package com.wilo.server.media.dto;

import com.wilo.server.media.entity.Media;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadResponse {

    private Long mediaId;
    private String url;
    private String thumbnailUrl;
    private String mediaType;

    public static MediaUploadResponse from(Media media) {
        return MediaUploadResponse.builder()
                .mediaId(media.getId())
                .url(media.getUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .mediaType(media.getMediaType().name())
                .build();
    }
}
