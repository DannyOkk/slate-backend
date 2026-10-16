package com.slate.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientTransactionsSummaryResponse {

    private Long clientId;
    private String clientName;
    private BigDecimal totalBalance;
    private List<TransactionResponse> transactions;
}
