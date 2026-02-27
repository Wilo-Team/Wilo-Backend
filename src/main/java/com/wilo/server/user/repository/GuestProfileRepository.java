package com.wilo.server.user.repository;

import com.wilo.server.user.entity.GuestProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestProfileRepository extends JpaRepository<GuestProfile, Long> {
    Optional<GuestProfile> findByGuestId(String guestId);
}
