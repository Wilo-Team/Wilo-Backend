package com.wilo.server.media.entity;

import com.wilo.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "media")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaSourceType sourceType;

    public static Media create(String url, String thumbnailUrl, MediaType mediaType, MediaSourceType sourceType) {
        Media media = new Media();
        media.url = url;
        media.thumbnailUrl = thumbnailUrl;
        media.mediaType = mediaType;
        media.sourceType = sourceType;
        return media;
    }
}
