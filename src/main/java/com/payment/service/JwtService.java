package com.payment.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${payment.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<Boolean> validateToken(String token, UUID userId) {
        return Mono.fromCallable(() -> {
            if (token == null || !token.startsWith("Bearer ")) {
                return false;
            }
            String jwt = token.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwt)
                        .getPayload();

                String subject = claims.getSubject();
                Date expiration = claims.getExpiration();

                return subject.equals(userId.toString()) && expiration.after(new Date());
            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
                return false;
            }
        });
    }
}
