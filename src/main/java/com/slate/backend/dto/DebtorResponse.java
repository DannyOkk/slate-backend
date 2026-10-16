package com.slate.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtorResponse {

    private Long clientId;
    private String clientName;
    private String clientPhone;
    private BigDecimal totalBalance;
    private LocalDateTime lastTransactionDate;
}
