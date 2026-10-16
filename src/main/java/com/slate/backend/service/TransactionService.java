package com.slate.backend.service;

import com.slate.backend.dto.ClientTransactionsSummaryResponse;
import com.slate.backend.dto.TransactionRequest;
import com.slate.backend.dto.TransactionResponse;
import com.slate.backend.entity.Client;
import com.slate.backend.entity.Transaction;
import com.slate.backend.entity.TransactionType;
import com.slate.backend.entity.User;
import com.slate.backend.exception.ResourceNotFoundException;
import com.slate.backend.repository.ClientRepository;
import com.slate.backend.repository.TransactionRepository;
import com.slate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    /**
     * Obtiene el comerciante autenticado actual a partir del SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario comerciante no encontrado con email: " + email));
    }

    /**
     * Obtiene y valida que el cliente exista y pertenezca al comerciante autenticado.
     */
    private Client getClientForCurrentUser(Long clientId) {
        User currentUser = getCurrentUser();
        return clientRepository.findByIdAndUserId(clientId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clientId));
    }

    /**
     * Mapea una entidad Transaction a su TransactionResponse.
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .clientId(transaction.getClient().getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Registra un movimiento (FIADO o PAGO) para un cliente del comerciante autenticado.
     */
    @Transactional
    public TransactionResponse createTransaction(Long clientId, TransactionRequest request) {
        Client client = getClientForCurrentUser(clientId);

        Transaction transaction = Transaction.builder()
                .client(client)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    /**
     * Retorna el resumen del cliente incluyendo el saldo/deuda total calculado y la lista de movimientos.
     * Deuda total = Suma(FIADO) - Suma(PAGO).
     */
    @Transactional(readOnly = true)
    public ClientTransactionsSummaryResponse getTransactionsByClient(Long clientId) {
        // Valida existencia y pertenencia del cliente al usuario autenticado
        Client client = getClientForCurrentUser(clientId);

        List<Transaction> transactions = transactionRepository.findByClientIdOrderByCreatedAtDesc(clientId);

        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
            if (transaction.getType() == TransactionType.FIADO) {
                totalBalance = totalBalance.add(amount);
            } else if (transaction.getType() == TransactionType.PAGO) {
                totalBalance = totalBalance.subtract(amount);
            }
        }

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::mapToResponse)
                .toList();

        return ClientTransactionsSummaryResponse.builder()
                .clientId(client.getId())
                .clientName(client.getName())
                .totalBalance(totalBalance)
                .transactions(transactionResponses)
                .build();
    }
}
