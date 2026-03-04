package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunitySearchHistory;
import com.wilo.server.community.repository.query.CommunitySearchHistoryQueryRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunitySearchHistoryRepository extends JpaRepository<CommunitySearchHistory, Long>, CommunitySearchHistoryQueryRepository {

    Optional<CommunitySearchHistory> findByUserIdAndKeyword(Long userId, String keyword);

    int deleteByUserId(Long userId);
}
