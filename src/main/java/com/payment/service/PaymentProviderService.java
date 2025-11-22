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

    @Value("${payment.providers.stripe.api-key}")
    private String stripeApiKey;

    @Value("${payment.providers.cashfree.app-id}")
    private String cashfreeAppId;

    @Value("${payment.providers.cashfree.secret-key}")
    private String cashfreeSecretKey;

    private final WebClient webClient;

    public PaymentProviderService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<PaymentLink> generatePaymentLink(PaymentRequest request) {
        // Strict check: Only allow STRIPE or CASHFREE
        return switch (request.getPaymentProvider().toUpperCase()) {
            case "STRIPE" -> generateStripeLink(request);
            case "CASHFREE" -> generateCashfreeLink(request);
            default -> Mono.error(new IllegalArgumentException("Unsupported provider: " + request.getPaymentProvider() + ". Only STRIPE and CASHFREE are supported."));
        };
    }

    // =================================================================================
    // STRIPE INTEGRATION
    // =================================================================================
    private Mono<PaymentLink> generateStripeLink(PaymentRequest request) {
        log.info("Generating Stripe payment link for amount: {}", request.getAmount());

        // In a real production scenario, you would make a POST request to:
        // https://api.stripe.com/v1/checkout/sessions
        // Headers: Authorization: Bearer <stripeApiKey>

        // Simulating a successful response for functionality testing
        String orderId = "stripe_ord_" + System.currentTimeMillis();
        return Mono.just(PaymentLink.builder()
                .orderId(orderId)
                .paymentUrl("https://checkout.stripe.com/pay/" + orderId + "?simulated=true")
                .provider("STRIPE")
                .build());
    }

    // =================================================================================
    // CASHFREE INTEGRATION
    // =================================================================================
    private Mono<PaymentLink> generateCashfreeLink(PaymentRequest request) {
        log.info("Generating Cashfree payment link for amount: {}", request.getAmount());

        // In a real production scenario, you would make a POST request to:
        // https://sandbox.cashfree.com/pg/orders
        // Headers: x-client-id: <cashfreeAppId>, x-client-secret: <cashfreeSecretKey>

        // Simulating a successful response for functionality testing
        String orderId = "cashfree_ord_" + System.currentTimeMillis();
        return Mono.just(PaymentLink.builder()
                .orderId(orderId)
                .paymentUrl("https://sandbox.cashfree.com/pay/" + orderId + "?simulated=true")
                .provider("CASHFREE")
                .build());
    }

    // =================================================================================
    // VERIFICATION LOGIC
    // =================================================================================
    public Mono<VerificationResult> verifyTransaction(String providerOrderId, String transactionId) {
        log.info("Verifying transaction with provider order: {}", providerOrderId);

        if (providerOrderId.startsWith("stripe_")) {
            return verifyStripeTransaction(providerOrderId, transactionId);
        } else if (providerOrderId.startsWith("cashfree_")) {
            return verifyCashfreeTransaction(providerOrderId, transactionId);
        }

        return Mono.just(VerificationResult.builder()
                .successful(false)
                .error("Unknown provider order id format. Expected start with 'stripe_' or 'cashfree_'")
                .build());
    }

    private Mono<VerificationResult> verifyStripeTransaction(String orderId, String transactionId) {
        log.info("Verifying Stripe transaction. Order: {}, Txn: {}", orderId, transactionId);
        // In production: Call https://api.stripe.com/v1/payment_intents/{id}
        return Mono.just(VerificationResult.builder()
                .successful(true)
                .transactionId(transactionId)
                .build());
    }

    private Mono<VerificationResult> verifyCashfreeTransaction(String orderId, String transactionId) {
        log.info("Verifying Cashfree transaction. Order: {}, Txn: {}", orderId, transactionId);
        // In production: Call https://sandbox.cashfree.com/pg/orders/{order_id}
        return Mono.just(VerificationResult.builder()
                .successful(true)
                .transactionId(transactionId)
                .build());
    }
}

// Helper DTOs for Internal Use
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