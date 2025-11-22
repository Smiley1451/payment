package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    private UUID jobId;
    private UUID userId;
    private UUID labourId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentProvider;
}
