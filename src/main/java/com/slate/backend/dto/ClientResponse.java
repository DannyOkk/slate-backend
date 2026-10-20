package com.slate.backend.dto;

import com.slate.backend.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {

    private Long id;
    private String name;
    private String phone;
    private String address;
    private String notes;
    private BigDecimal totalBalance;
    private RiskLevel riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
