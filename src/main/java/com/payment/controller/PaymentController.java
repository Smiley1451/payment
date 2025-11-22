package com.payment.controller;

import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public Mono<ResponseEntity<PaymentResponse>> initiatePayment(
            @RequestHeader(value = "Authorization", required = false) String token, // Token is now optional
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PaymentRequest request) {
        log.info("Received payment initiation request for job: {}", request.getJobId());
        // We still pass the token (even if null) to the service, but the service will ignore it.
        return paymentService.initiatePayment(request, token, idempotencyKey)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Error initiating payment", error);
                    if (error instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/verify")
    public Mono<ResponseEntity<Void>> verifyPayment(
            @RequestParam String providerOrderId,
            @RequestParam String transactionId) {
        log.info("Received payment verification request");
        return paymentService.verifyPayment(providerOrderId, transactionId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(error -> {
                    log.error("Error verifying payment", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/status/{paymentId}")
    public Mono<ResponseEntity<String>> getPaymentStatus(@PathVariable UUID paymentId) {
        log.info("Fetching payment status for id: {}", paymentId);
        return paymentService.getPaymentStatus(paymentId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(error -> {
                    log.error("Error fetching payment status", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}