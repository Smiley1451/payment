package com.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    private String paymentId;
    private String jobId;
    private String userId;
    private String status;
    private String amount;
    private String timestamp;
}
