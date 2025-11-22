package com.payment.service;

import com.payment.entity.Payment;
import com.payment.entity.Payout;
import com.payment.event.NotificationEvent;
import com.payment.event.PaymentEvent;
import com.payment.event.PayoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> publishPaymentSuccessEvent(Payment payment) {
        log.info("Publishing payment success event for payment: {}", payment.getId());

        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId().toString())
                .jobId(payment.getJobId().toString())
                .userId(payment.getUserId().toString())
                .status("SUCCESS")
                .amount(payment.getAmount().toPlainString())
                .timestamp(Instant.now().toString())
                .build();

        kafkaTemplate.send("payment-events", payment.getId().toString(), event);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "payment");
        metadata.put("timestamp", Instant.now().toString());

        NotificationEvent notification = NotificationEvent.builder()
                .userName("Payment Service")
                .username(payment.getUserId().toString())
                .subject("Payment Successful")
                .source("WHATSAPP")
                .message("Your payment of INR " + payment.getAmount() + " for Job #" + payment.getJobId() + " is successful.")
                .metadata(metadata)
                .build();

        kafkaTemplate.send("notifications", payment.getUserId().toString(), notification);

        return Mono.empty();
    }

    public Mono<Void> publishPaymentFailureEvent(Payment payment) {
        log.info("Publishing payment failure event for payment: {}", payment.getId());

        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId().toString())
                .jobId(payment.getJobId().toString())
                .userId(payment.getUserId().toString())
                .status("FAILED")
                .amount(payment.getAmount().toPlainString())
                .timestamp(Instant.now().toString())
                .build();

        kafkaTemplate.send("payment-events", payment.getId().toString(), event);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "payment");
        metadata.put("timestamp", Instant.now().toString());

        NotificationEvent notification = NotificationEvent.builder()
                .userName("Payment Service")
                .username(payment.getUserId().toString())
                .subject("Payment Failed")
                .source("EMAIL")
                .message("Your payment of INR " + payment.getAmount() + " for Job #" + payment.getJobId() + " has failed. Please try again.")
                .metadata(metadata)
                .build();

        kafkaTemplate.send("notifications", payment.getUserId().toString(), notification);

        return Mono.empty();
    }

    public Mono<Void> publishPayoutSuccessEvent(Payout payout) {
        log.info("Publishing payout success event for payout: {}", payout.getId());

        PayoutEvent event = PayoutEvent.builder()
                .payoutId(payout.getId().toString())
                .jobId(payout.getJobId().toString())
                .labourId(payout.getLabourId().toString())
                .status("PROCESSED")
                .amount(payout.getAmount().toPlainString())
                .commission(payout.getCommission().toPlainString())
                .timestamp(Instant.now().toString())
                .build();

        kafkaTemplate.send("payout-events", payout.getId().toString(), event);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "payout");
        metadata.put("timestamp", Instant.now().toString());

        NotificationEvent notification = NotificationEvent.builder()
                .userName("Payout Service")
                .username(payout.getLabourId().toString())
                .subject("Payout Settled")
                .source("WHATSAPP")
                .message("You received ₹" + payout.getAmount() + " for Job #" + payout.getJobId() + ".")
                .metadata(metadata)
                .build();

        kafkaTemplate.send("notifications", payout.getLabourId().toString(), notification);

        return Mono.empty();
    }

    public Mono<Void> publishPayoutFailureEvent(Payout payout) {
        log.info("Publishing payout failure event for payout: {}", payout.getId());

        PayoutEvent event = PayoutEvent.builder()
                .payoutId(payout.getId().toString())
                .jobId(payout.getJobId().toString())
                .labourId(payout.getLabourId().toString())
                .status("FAILED")
                .amount(payout.getAmount().toPlainString())
                .commission(payout.getCommission().toPlainString())
                .timestamp(Instant.now().toString())
                .build();

        kafkaTemplate.send("payout-events", payout.getId().toString(), event);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "payout");
        metadata.put("timestamp", Instant.now().toString());

        NotificationEvent notification = NotificationEvent.builder()
                .userName("Payout Service")
                .username(payout.getLabourId().toString())
                .subject("Payout Failed")
                .source("EMAIL")
                .message("Your payout for Job #" + payout.getJobId() + " could not be processed. Please contact support.")
                .metadata(metadata)
                .build();

        kafkaTemplate.send("notifications", payout.getLabourId().toString(), notification);

        return Mono.empty();
    }
}
