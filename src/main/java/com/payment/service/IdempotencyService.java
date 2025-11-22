package com.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.payment.entity.IdempotencyKey;
import com.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<Boolean> checkIdempotency(String key) {
        return redisTemplate.hasKey("idempotency:" + key)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.just(true);
                    }
                    return idempotencyKeyRepository.findByIdempotencyKey(key)
                            .map(k -> true)
                            .defaultIfEmpty(false);
                });
    }

    public Mono<Void> saveIdempotencyKey(String key, JsonNode response) {
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .idempotencyKey(key)
                .responseData(response)
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        return idempotencyKeyRepository.save(idempotencyKey)
                .flatMap(saved -> redisTemplate.opsForValue()
                        .set("idempotency:" + key, "PROCESSED", Duration.ofHours(24))
                        .then());
    }
}
