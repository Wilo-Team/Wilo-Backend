package com.wilo.server.community.entity;

import com.wilo.server.global.entity.BaseEntity;
import com.wilo.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "community_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE community_comments SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CommunityComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommunityComment parentComment;

    @Column(nullable = false, length = 1000)
    private String content;

    @Builder
    private CommunityComment(CommunityPost post, User user, CommunityComment parentComment, String content) {
        this.post = post;
        this.user = user;
        this.parentComment = parentComment;
        this.content = content;
    }
}
