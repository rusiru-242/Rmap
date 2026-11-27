package com.rmap.platform.auth;

import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;
    private final Clock clock;

    public JwtService(RmapProperties properties, Clock clock) {
        byte[] keyBytes = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("rmap.security.jwt-secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = properties.security().accessTokenTtlSeconds();
        this.clock = clock;
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        Instant expiry = now.plusSeconds(accessTokenTtlSeconds);
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        String roleStr = claims.get("role", String.class);
        if (userId == null || email == null || roleStr == null) {
            throw new JwtException("Missing required JWT claims");
        }
        return new AuthenticatedUser(userId, email, Role.valueOf(roleStr));
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
