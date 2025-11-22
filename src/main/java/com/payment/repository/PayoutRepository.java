package com.payment.repository;

import com.payment.entity.Payout;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface PayoutRepository extends R2dbcRepository<Payout, UUID> {

    Mono<Payout> findByPaymentId(UUID paymentId);
}
