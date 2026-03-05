package com.wilo.server.media.repository;

import com.wilo.server.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}
