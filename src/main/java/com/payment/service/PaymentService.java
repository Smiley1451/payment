package com.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.entity.Payment;
import com.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final PaymentProviderService paymentProviderService;
    private final KafkaEventProducer kafkaEventProducer;
    private final JobServiceClient jobServiceClient;
    // Removed JwtService dependency
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          WebClient webClient,
                          @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, // Added Qualifier to fix startup error
                          PaymentProviderService paymentProviderService,
                          KafkaEventProducer kafkaEventProducer,
                          JobServiceClient jobServiceClient,
                          // JwtService removed from constructor
                          IdempotencyService idempotencyService,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
        this.paymentProviderService = paymentProviderService;
        this.kafkaEventProducer = kafkaEventProducer;
        this.jobServiceClient = jobServiceClient;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    public Mono<PaymentResponse> initiatePayment(PaymentRequest request, String token, String idempotencyKey) {
        log.info("Initiating payment for job: {}, user: {}", request.getJobId(), request.getUserId());

        // 1. Idempotency Check
        Mono<Void> idempotencyCheck = (idempotencyKey != null)
                ? idempotencyService.checkIdempotency(idempotencyKey)
                .filter(exists -> exists)
                .flatMap(exists -> Mono.error(new IllegalArgumentException("Duplicate request")))
                .then()
                : Mono.empty();

        // 2. JWT Validation REMOVED - Proceeding directly to Job Validation

        return idempotencyCheck
                .then(jobServiceClient.validateJob(request.getJobId()))
                .filter(valid -> valid)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Job validation failed")))
                .then(paymentProviderService.generatePaymentLink(request))
                .flatMap(paymentLink -> {
                    Payment payment = Payment.builder()
                            .jobId(request.getJobId())
                            .userId(request.getUserId())
                            .labourId(request.getLabourId())
                            .amount(request.getAmount())
                            .currency("INR")
                            .paymentMethod(request.getPaymentMethod())
                            .paymentProvider(request.getPaymentProvider())
                            .providerOrderId(paymentLink.getOrderId())
                            .status("PENDING")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return paymentRepository.save(payment)
                            .flatMap(savedPayment -> {
                                log.info("Payment saved with id: {}", savedPayment.getId());
                                PaymentResponse response = PaymentResponse.builder()
                                        .id(savedPayment.getId())
                                        .jobId(savedPayment.getJobId())
                                        .userId(savedPayment.getUserId())
                                        .labourId(savedPayment.getLabourId())
                                        .amount(savedPayment.getAmount())
                                        .status(savedPayment.getStatus())
                                        .paymentLink(paymentLink.getPaymentUrl())
                                        .createdAt(savedPayment.getCreatedAt())
                                        .build();

                                Mono<Void> cacheAndIdempotency = cachePaymentStatus(savedPayment.getId().toString(), "PENDING");

                                if (idempotencyKey != null) {
                                    cacheAndIdempotency = cacheAndIdempotency.then(
                                            idempotencyService.saveIdempotencyKey(idempotencyKey, objectMapper.valueToTree(response))
                                                    .then()
                                    );
                                }

                                return cacheAndIdempotency.thenReturn(response);
                            });
                })
                .doOnError(error -> log.error("Error initiating payment", error));
    }

    public Mono<Void> verifyPayment(String providerOrderId, String transactionId) {
        log.info("Verifying payment for provider order: {}", providerOrderId);

        return paymentProviderService.verifyTransaction(providerOrderId, transactionId)
                .flatMap(verificationResult -> {
                    String status = verificationResult.isSuccessful() ? "SUCCESS" : "FAILED";

                    return paymentRepository.findByProviderOrderId(providerOrderId)
                            .flatMap(payment -> {
                                payment.setStatus(status);
                                payment.setUpdatedAt(LocalDateTime.now());
                                return paymentRepository.save(payment);
                            })
                            .flatMap(updatedPayment ->
                                    redisTemplate.opsForValue()
                                            .set(updatedPayment.getId().toString(), status, Duration.ofHours(24))
                                            .then(Mono.just(updatedPayment))
                            )
                            .flatMap(updatedPayment -> {
                                if ("SUCCESS".equals(status)) {
                                    return kafkaEventProducer.publishPaymentSuccessEvent(updatedPayment);
                                } else {
                                    return kafkaEventProducer.publishPaymentFailureEvent(updatedPayment);
                                }
                            });
                });
    }

    public Mono<String> getPaymentStatus(UUID paymentId) {
        log.info("Fetching payment status for id: {}", paymentId);

        return redisTemplate.opsForValue().get(paymentId.toString())
                .switchIfEmpty(
                        paymentRepository.findById(paymentId)
                                .flatMap(payment ->
                                        cachePaymentStatus(paymentId.toString(), payment.getStatus())
                                                .thenReturn(payment.getStatus())
                                )
                );
    }

    private Mono<Void> cachePaymentStatus(String paymentId, String status) {
        return redisTemplate.opsForValue()
                .set(paymentId, status, Duration.ofHours(24))
                .then();
    }
}