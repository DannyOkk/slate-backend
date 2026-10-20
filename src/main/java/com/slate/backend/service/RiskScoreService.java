package com.slate.backend.service;

import com.slate.backend.entity.Client;
import com.slate.backend.entity.RiskLevel;
import com.slate.backend.entity.Transaction;
import com.slate.backend.entity.TransactionType;
import com.slate.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiskScoreService {

    public static final BigDecimal HIGH_DEBT_THRESHOLD = new BigDecimal("50000.00");
    public static final long DAYS_OVERDUE_THRESHOLD = 30L;

    private final TransactionRepository transactionRepository;

    /**
     * Calcula el saldo neto total a partir de una lista de transacciones.
     * Saldo = Suma(FIADO) - Suma(PAGO).
     */
    public BigDecimal calculateTotalBalance(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
            if (transaction.getType() == TransactionType.FIADO) {
                balance = balance.add(amount);
            } else if (transaction.getType() == TransactionType.PAGO) {
                balance = balance.subtract(amount);
            }
        }
        return balance;
    }

    /**
     * Calcula el nivel de riesgo de un cliente a partir de sus transacciones y saldo acumulado.
     *
     * Reglas de negocio:
     * 1. Si no tiene transacciones o saldo acumulado <= 0: BAJO.
     * 2. Si saldo acumulado > 0:
     *    - Si la deuda supera los $50,000 O pasaron más de 30 días desde el último pago (o última transacción si no hay pagos): ALTO.
     *    - En cualquier otro caso de deuda menor y reciente: MEDIO.
     */
    public RiskLevel calculateRiskLevel(List<Transaction> transactions, BigDecimal totalBalance) {
        if (transactions == null || transactions.isEmpty() || totalBalance == null || totalBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevel.BAJO;
        }

        // Si la deuda supera el umbral elevado, el riesgo es ALTO inmediatamente
        if (totalBalance.compareTo(HIGH_DEBT_THRESHOLD) > 0) {
            return RiskLevel.ALTO;
        }

        // Determinar fecha de referencia: fecha del último PAGO, o de la última transacción si no hay pagos
        Optional<LocalDateTime> lastPaymentDate = transactions.stream()
                .filter(t -> t.getType() == TransactionType.PAGO && t.getCreatedAt() != null)
                .map(Transaction::getCreatedAt)
                .max(Comparator.naturalOrder());

        LocalDateTime referenceDate = lastPaymentDate.orElseGet(() ->
                transactions.stream()
                        .map(Transaction::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null)
        );

        if (referenceDate == null) {
            return RiskLevel.MEDIO;
        }

        long daysSinceReference = ChronoUnit.DAYS.between(referenceDate.toLocalDate(), LocalDate.now());
        if (daysSinceReference > DAYS_OVERDUE_THRESHOLD) {
            return RiskLevel.ALTO;
        }

        return RiskLevel.MEDIO;
    }

    /**
     * Calcula el nivel de riesgo cargando las transacciones del cliente desde la base de datos.
     */
    public RiskLevel calculateRiskLevel(Client client) {
        if (client == null || client.getId() == null) {
            return RiskLevel.BAJO;
        }

        List<Transaction> transactions = transactionRepository.findByClientIdOrderByCreatedAtDesc(client.getId());
        BigDecimal totalBalance = calculateTotalBalance(transactions);
        return calculateRiskLevel(transactions, totalBalance);
    }
}
