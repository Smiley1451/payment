package com.payment.repository;

import com.payment.entity.IdempotencyKey;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends R2dbcRepository<IdempotencyKey, UUID> {
    Mono<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
