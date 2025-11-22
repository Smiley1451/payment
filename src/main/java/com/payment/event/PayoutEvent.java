package com.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutEvent {

    private String payoutId;
    private String jobId;
    private String labourId;
    private String status;
    private String amount;
    private String commission;
    private String timestamp;
}
