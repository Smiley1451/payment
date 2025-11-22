package com.payment.repository;

import com.payment.entity.Payment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface PaymentRepository extends R2dbcRepository<Payment, UUID> {

    Mono<Payment> findByProviderOrderId(String providerOrderId);

    @Query("SELECT * FROM payments WHERE job_id = $1 ORDER BY created_at DESC LIMIT 1")
    Mono<Payment> findLatestByJobId(UUID jobId);
}
