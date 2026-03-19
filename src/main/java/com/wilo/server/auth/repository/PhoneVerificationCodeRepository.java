package com.wilo.server.auth.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PhoneVerificationCodeRepository {

    private static final String KEY_PREFIX = "auth:phone-verification:";
    private static final long VERIFICATION_CODE_TTL_MINUTES = 3L;

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String phoneNumber, String verificationCode) {
        redisTemplate.opsForValue()
                .set(createKey(phoneNumber), verificationCode, VERIFICATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Optional<String> findByPhoneNumber(String phoneNumber) {
        Object value = redisTemplate.opsForValue().get(createKey(phoneNumber));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value.toString());
    }

    public void deleteByPhoneNumber(String phoneNumber) {
        redisTemplate.delete(createKey(phoneNumber));
    }

    private String createKey(String phoneNumber) {
        return KEY_PREFIX + phoneNumber;
    }
}
