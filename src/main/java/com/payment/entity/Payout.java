package com.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payouts")
public class Payout {

    @Id
    private UUID id;

    @Column("job_id")
    private UUID jobId;

    @Column("payment_id")
    private UUID paymentId;

    @Column("labour_id")
    private UUID labourId;

    private BigDecimal amount;

    private BigDecimal commission;

    @Column("settlement_status")
    private String settlementStatus;

    @Column("bank_account_ref")
    private String bankAccountRef;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
