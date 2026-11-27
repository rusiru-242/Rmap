package com.rmap.platform.auth;

import com.rmap.platform.config.RmapProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class RedisOtpStore implements OtpStore {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final Duration otpTtl;

    public RedisOtpStore(StringRedisTemplate redisTemplate, RmapProperties properties) {
        this.redisTemplate = redisTemplate;
        this.otpTtl = Duration.ofSeconds(properties.security().otpTtlSeconds());
    }

    @Override
    public String generateAndStore(String email, String purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = buildKey(email, purpose);
        redisTemplate.opsForValue().set(key, sha256(code), otpTtl);
        return code;
    }

    @Override
    public boolean verifyAndConsume(String email, String purpose, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String key = buildKey(email, purpose);
        String storedHash = redisTemplate.opsForValue().get(key);
        if (storedHash == null) {
            return false;
        }
        String candidateHash = sha256(code.trim());
        boolean match = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                candidateHash.getBytes(StandardCharsets.UTF_8)
        );
        if (match) {
            redisTemplate.delete(key);
        }
        return match;
    }

    private String buildKey(String email, String purpose) {
        return "rmap:otp:" + purpose.toUpperCase(Locale.ROOT) + ":" + email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
