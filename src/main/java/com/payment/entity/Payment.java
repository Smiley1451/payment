package com.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
@Table("payments")
public class Payment {

    @Id
    private UUID id;

    @Column("job_id")
    private UUID jobId;

    @Column("user_id")
    private UUID userId;

    @Column("labour_id")
    private UUID labourId;

    private BigDecimal amount;
    
    private String currency;

    @Column("payment_method")
    private String paymentMethod;

    @Column("payment_provider")
    private String paymentProvider;

    @Column("provider_order_id")
    private String providerOrderId;

    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    private JsonNode metadata;
}
