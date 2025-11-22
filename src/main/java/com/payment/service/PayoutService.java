package com.payment.service;

import com.payment.dto.PayoutRequest;
import com.payment.entity.Payment;
import com.payment.entity.Payout;
import com.payment.repository.PaymentRepository;
import com.payment.repository.PayoutRepository;
import com.payment.service.JobServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private final JobServiceClient jobServiceClient;

    @Autowired
    public PayoutService(PayoutRepository payoutRepository,
                        PaymentRepository paymentRepository,
                        KafkaEventProducer kafkaEventProducer,
                        JobServiceClient jobServiceClient) {
        this.payoutRepository = payoutRepository;
        this.paymentRepository = paymentRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.jobServiceClient = jobServiceClient;
    }

    public Mono<Void> initiatePayoutToLabour(UUID paymentId, UUID labourId, String bankAccountRef) {
        log.info("Initiating payout for labour: {} with payment: {}", labourId, paymentId);

        return paymentRepository.findById(paymentId)
                .flatMap(payment -> {
                    if (!"SUCCESS".equals(payment.getStatus())) {
                        return Mono.error(new IllegalStateException("Payment not successful"));
                    }

                    return jobServiceClient.isJobComplete(payment.getJobId())
                            .flatMap(isComplete -> {
                                if (!Boolean.TRUE.equals(isComplete)) {
                                    return Mono.error(new IllegalStateException("Job is not marked as complete"));
                                }

                                BigDecimal commission = payment.getAmount().multiply(BigDecimal.valueOf(0.1));
                                BigDecimal payoutAmount = payment.getAmount().subtract(commission);

                                Payout payout = Payout.builder()
                                        .jobId(payment.getJobId())
                                        .paymentId(payment.getId())
                                        .labourId(labourId)
                                        .amount(payoutAmount)
                                        .commission(commission)
                                        .settlementStatus("PENDING")
                                        .bankAccountRef(bankAccountRef)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                                return payoutRepository.save(payout)
                                        .flatMap(savedPayout -> {
                                            log.info("Payout created with id: {}", savedPayout.getId());
                                            return processBankTransfer(savedPayout)
                                                    .flatMap(v -> kafkaEventProducer.publishPayoutSuccessEvent(savedPayout));
                                        });
                            });
                })
                .doOnError(error -> log.error("Error initiating payout", error));
    }

    public Mono<Void> processBankTransfer(Payout payout) {
        log.info("Processing bank transfer for payout: {}", payout.getId());
        
        payout.setSettlementStatus("PROCESSED");
        payout.setUpdatedAt(LocalDateTime.now());
        
        return payoutRepository.save(payout)
                .then();
    }

    public Mono<String> getPayoutStatus(UUID payoutId) {
        log.info("Fetching payout status for id: {}", payoutId);

        return payoutRepository.findById(payoutId)
                .map(Payout::getSettlementStatus)
                .switchIfEmpty(Mono.error(new IllegalStateException("Payout not found")));
    }
}
