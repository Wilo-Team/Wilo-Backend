package com.wilo.server.community.entity;

import com.wilo.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "community_post_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE community_post_images SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CommunityPostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @Column(nullable = false, length = 512)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder
    private CommunityPostImage(CommunityPost post, String imageUrl, Integer sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}
