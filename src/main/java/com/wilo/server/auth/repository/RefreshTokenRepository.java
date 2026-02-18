package com.wilo.server.auth.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Long userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(createKey(userId), refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public Optional<String> findByUserId(Long userId) {
        Object value = redisTemplate.opsForValue().get(createKey(userId));
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(value.toString());
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(createKey(userId));
    }

    private String createKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
