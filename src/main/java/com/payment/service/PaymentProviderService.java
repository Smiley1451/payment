package com.payment.service;

import com.payment.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentProviderService {

    @Value("${payment.providers.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.providers.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.providers.stripe.api-key}")
    private String stripeApiKey;

    private final WebClient webClient;

    public PaymentProviderService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<PaymentLink> generatePaymentLink(PaymentRequest request) {
        return switch (request.getPaymentProvider().toUpperCase()) {
            case "RAZORPAY" -> generateRazorpayLink(request);
            case "STRIPE" -> generateStripeLink(request);
            case "PAYTM" -> generatePaytmLink(request);
            case "CASHFREE" -> generateCashfreeLink(request);
            default -> Mono.error(new IllegalArgumentException("Unsupported provider: " + request.getPaymentProvider()));
        };
    }

    private Mono<PaymentLink> generateRazorpayLink(PaymentRequest request) {
        log.info("Generating Razorpay payment link");
        return webClient.post()
                .uri("https://api.razorpay.com/v1/orders")
                .header("Authorization", "Basic " + java.util.Base64.getEncoder()
                        .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes()))
                .bodyValue(RazorpayOrderRequest.builder()
                        .amount((request.getAmount().longValue() * 100))
                        .currency("INR")
                        .build())
                .retrieve()
                .bodyToMono(RazorpayOrderResponse.class)
                .map(response -> PaymentLink.builder()
                        .orderId(response.getId())
                        .paymentUrl("https://razorpay.com/pay/" + response.getId())
                        .provider("RAZORPAY")
                        .build())
                .doOnError(error -> log.error("Razorpay error: ", error));
    }

    private Mono<PaymentLink> generateStripeLink(PaymentRequest request) {
        log.info("Generating Stripe payment link");
        return Mono.just(PaymentLink.builder()
                .orderId("stripe_" + System.currentTimeMillis())
                .paymentUrl("https://checkout.stripe.com/pay/" + System.currentTimeMillis())
                .provider("STRIPE")
                .build());
    }

    private Mono<PaymentLink> generatePaytmLink(PaymentRequest request) {
        log.info("Generating PayTM payment link");
        return Mono.just(PaymentLink.builder()
                .orderId("paytm_" + System.currentTimeMillis())
                .paymentUrl("https://pguat.paytm.com/olp")
                .provider("PAYTM")
                .build());
    }

    private Mono<PaymentLink> generateCashfreeLink(PaymentRequest request) {
        log.info("Generating Cashfree payment link");
        return Mono.just(PaymentLink.builder()
                .orderId("cashfree_" + System.currentTimeMillis())
                .paymentUrl("https://checkout.cashfree.com/pay")
                .provider("CASHFREE")
                .build());
    }

    public Mono<VerificationResult> verifyTransaction(String providerOrderId, String transactionId) {
        log.info("Verifying transaction with provider order: {}", providerOrderId);
        
        if (providerOrderId.startsWith("razorpay_")) {
            return verifyRazorpayTransaction(providerOrderId, transactionId);
        } else if (providerOrderId.startsWith("stripe_")) {
            // Mock Stripe verification
            return Mono.just(VerificationResult.builder().successful(true).transactionId(transactionId).build());
        } else if (providerOrderId.startsWith("paytm_")) {
            // Mock PayTM verification
            return Mono.just(VerificationResult.builder().successful(true).transactionId(transactionId).build());
        } else if (providerOrderId.startsWith("cashfree_")) {
            // Mock Cashfree verification
            return Mono.just(VerificationResult.builder().successful(true).transactionId(transactionId).build());
        }
        
        return Mono.just(VerificationResult.builder()
                .successful(false)
                .error("Unknown provider order id format")
                .build());
    }

    private Mono<VerificationResult> verifyRazorpayTransaction(String orderId, String paymentId) {
        return webClient.get()
                .uri("https://api.razorpay.com/v1/payments/" + paymentId)
                .header("Authorization", "Basic " + java.util.Base64.getEncoder()
                        .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes()))
                .retrieve()
                .bodyToMono(RazorpayPaymentResponse.class)
                .map(response -> VerificationResult.builder()
                        .successful("captured".equals(response.getStatus()))
                        .transactionId(response.getId())
                        .build())
                .onErrorResume(e -> Mono.just(VerificationResult.builder()
                        .successful(false)
                        .error(e.getMessage())
                        .build()));
    }
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class PaymentLink {
    private String orderId;
    private String paymentUrl;
    private String provider;
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class VerificationResult {
    private boolean successful;
    private String transactionId;
    private String error;
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class RazorpayOrderRequest {
    private Long amount;
    private String currency;
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class RazorpayOrderResponse {
    private String id;
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class RazorpayPaymentResponse {
    private String id;
    private String status;
}
