package com.slate.backend.service;

import com.slate.backend.entity.Client;
import com.slate.backend.entity.RiskLevel;
import com.slate.backend.entity.Transaction;
import com.slate.backend.entity.TransactionType;
import com.slate.backend.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskScoreServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RiskScoreService riskScoreService;

    @Test
    @DisplayName("calculateTotalBalance - Suma FIADO y resta PAGO correctamente")
    void calculateTotalBalance_success() {
        Transaction t1 = Transaction.builder()
                .amount(new BigDecimal("1000.00"))
                .type(TransactionType.FIADO)
                .build();
        Transaction t2 = Transaction.builder()
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.PAGO)
                .build();

        BigDecimal balance = riskScoreService.calculateTotalBalance(List.of(t1, t2));
        assertThat(balance).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("calculateTotalBalance - Retorna CERO si la lista es nula o vacía")
    void calculateTotalBalance_emptyOrNull() {
        assertThat(riskScoreService.calculateTotalBalance(null)).isEqualByComparingTo("0.00");
        assertThat(riskScoreService.calculateTotalBalance(Collections.emptyList())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente sin transacciones o con saldo <= 0 debe tener riesgo BAJO")
    void calculateRiskLevel_lowRisk_zeroOrNegativeBalance() {
        // Sin transacciones
        assertThat(riskScoreService.calculateRiskLevel(Collections.emptyList(), BigDecimal.ZERO))
                .isEqualTo(RiskLevel.BAJO);

        // Con saldo a favor (negativo)
        Transaction t1 = Transaction.builder()
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.PAGO)
                .createdAt(LocalDateTime.now())
                .build();
        assertThat(riskScoreService.calculateRiskLevel(List.of(t1), new BigDecimal("-500.00")))
                .isEqualTo(RiskLevel.BAJO);
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente con deuda mayor a $50,000 debe tener riesgo ALTO incluso con pago reciente")
    void calculateRiskLevel_highRisk_debtAbove50000() {
        Transaction paymentYesterday = Transaction.builder()
                .amount(new BigDecimal("1000.00"))
                .type(TransactionType.PAGO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        RiskLevel level = riskScoreService.calculateRiskLevel(
                List.of(paymentYesterday),
                new BigDecimal("50000.01")
        );
        assertThat(level).isEqualTo(RiskLevel.ALTO);
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente con deuda moderada y pago hace más de 30 días debe tener riesgo ALTO")
    void calculateRiskLevel_highRisk_overduePayment() {
        Transaction oldPayment = Transaction.builder()
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.PAGO)
                .createdAt(LocalDateTime.now().minusDays(35))
                .build();

        Transaction fiado = Transaction.builder()
                .amount(new BigDecimal("1000.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now().minusDays(36))
                .build();

        RiskLevel level = riskScoreService.calculateRiskLevel(
                List.of(oldPayment, fiado),
                new BigDecimal("800.00")
        );
        assertThat(level).isEqualTo(RiskLevel.ALTO);
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente sin pagos previos con única compra hace más de 30 días debe tener riesgo ALTO")
    void calculateRiskLevel_highRisk_noPaymentsAndOldPurchase() {
        Transaction oldPurchase = Transaction.builder()
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now().minusDays(45))
                .build();

        RiskLevel level = riskScoreService.calculateRiskLevel(
                List.of(oldPurchase),
                new BigDecimal("500.00")
        );
        assertThat(level).isEqualTo(RiskLevel.ALTO);
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente con deuda moderada y pago reciente (<= 30 días) debe tener riesgo MEDIO")
    void calculateRiskLevel_mediumRisk_recentPayment() {
        Transaction recentPayment = Transaction.builder()
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.PAGO)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        RiskLevel level = riskScoreService.calculateRiskLevel(
                List.of(recentPayment),
                new BigDecimal("1500.00")
        );
        assertThat(level).isEqualTo(RiskLevel.MEDIO);
    }

    @Test
    @DisplayName("calculateRiskLevel - Cliente con deuda moderada sin pagos pero compra reciente (<= 30 días) debe tener riesgo MEDIO")
    void calculateRiskLevel_mediumRisk_recentPurchaseNoPayments() {
        Transaction recentPurchase = Transaction.builder()
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        RiskLevel level = riskScoreService.calculateRiskLevel(
                List.of(recentPurchase),
                new BigDecimal("300.00")
        );
        assertThat(level).isEqualTo(RiskLevel.MEDIO);
    }

    @Test
    @DisplayName("calculateRiskLevel(Client) - Consulta transacciones desde el repositorio y delega correctamente")
    void calculateRiskLevel_byClient_delegatesToRepository() {
        Client client = Client.builder().id(99L).name("Test Client").build();
        Transaction t = Transaction.builder()
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        when(transactionRepository.findByClientIdOrderByCreatedAtDesc(99L))
                .thenReturn(List.of(t));

        RiskLevel level = riskScoreService.calculateRiskLevel(client);
        assertThat(level).isEqualTo(RiskLevel.MEDIO);
    }
}
