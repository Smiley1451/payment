package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID jobId;
    private UUID userId;
    private UUID labourId;
    private BigDecimal amount;
    private String status;
    private String paymentLink;
    private LocalDateTime createdAt;
}
