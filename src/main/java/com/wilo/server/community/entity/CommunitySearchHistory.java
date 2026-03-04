package com.wilo.server.community.entity;

import com.wilo.server.global.entity.BaseEntity;
import com.wilo.server.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "community_search_histories",
        uniqueConstraints = @UniqueConstraint(name = "uk_search_history_user_keyword", columnNames = {"user_id", "keyword"}),
        indexes = {
                @Index(name = "idx_search_history_user_last_id", columnList = "user_id, last_searched_at, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunitySearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String keyword;

    @Column(nullable = false)
    private LocalDateTime lastSearchedAt;

    @Builder
    private CommunitySearchHistory(User user, String keyword, LocalDateTime lastSearchedAt) {
        this.user = user;
        this.keyword = keyword;
        this.lastSearchedAt = lastSearchedAt;
    }

    public void touch(LocalDateTime searchedAt) {
        this.lastSearchedAt = searchedAt;
    }
}
