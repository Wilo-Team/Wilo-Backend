package com.wilo.server.user.repository;

import com.wilo.server.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    List<User> findAllByPhoneNumber(String phoneNumber);
}
