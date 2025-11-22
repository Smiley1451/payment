package com.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("idempotency_keys")
public class IdempotencyKey {

    @Id
    private UUID id;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("request_hash")
    private String requestHash;

    @Column("response_data")
    private JsonNode responseData;

    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;
}
