package com.payment.controller;

import com.payment.dto.PayoutRequest;
import com.payment.service.PayoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/payouts")
public class PayoutController {

    private final PayoutService payoutService;

    @Autowired
    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @PostMapping("/initiate")
    public Mono<ResponseEntity<Void>> initiatePayoutToLabour(@RequestBody PayoutRequest request) {
        log.info("Received payout initiation request");
        return payoutService.initiatePayoutToLabour(request.getPaymentId(), request.getLabourId(), request.getBankAccountRef())
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).<Void>build()))
                .onErrorResume(error -> {
                    log.error("Error initiating payout", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/status/{payoutId}")
    public Mono<ResponseEntity<String>> getPayoutStatus(@PathVariable UUID payoutId) {
        log.info("Fetching payout status for id: {}", payoutId);
        return payoutService.getPayoutStatus(payoutId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(error -> {
                    log.error("Error fetching payout status", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
