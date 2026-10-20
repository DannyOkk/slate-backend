package com.slate.backend.service;

import com.slate.backend.dto.ClientRequest;
import com.slate.backend.dto.ClientResponse;
import com.slate.backend.dto.DebtorResponse;
import com.slate.backend.entity.Client;
import com.slate.backend.entity.RiskLevel;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RiskScoreService riskScoreService;

    /**
     * Obtiene el usuario autenticado actual a partir del email presente en el SecurityContext.
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
     * Mapea una entidad Client a su DTO ClientResponse incluyendo saldo acumulado y nivel de riesgo.
     */
    private ClientResponse mapToResponse(Client client, BigDecimal totalBalance, com.slate.backend.entity.RiskLevel riskLevel) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .phone(client.getPhone())
                .address(client.getAddress())
                .notes(client.getNotes())
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .riskLevel(riskLevel != null ? riskLevel : com.slate.backend.entity.RiskLevel.BAJO)
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    /**
     * Obtiene todos los clientes asociados al comerciante autenticado con su saldo y nivel de riesgo calculados.
     */
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClientsForCurrentUser() {
        User currentUser = getCurrentUser();
        List<Client> clients = clientRepository.findByUserId(currentUser.getId());
        if (clients.isEmpty()) {
            return Collections.emptyList();
        }

        // Carga todas las transacciones del comerciante en una sola consulta para evitar N+1
        List<Transaction> allTransactions = transactionRepository.findByClientUserIdOrderByCreatedAtDesc(currentUser.getId());
        Map<Long, List<Transaction>> transactionsByClient = allTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getClient().getId()));

        return clients.stream()
                .map(client -> {
                    List<Transaction> clientTx = transactionsByClient.getOrDefault(client.getId(), Collections.emptyList());
                    BigDecimal balance = riskScoreService.calculateTotalBalance(clientTx);
                    com.slate.backend.entity.RiskLevel risk = riskScoreService.calculateRiskLevel(clientTx, balance);
                    return mapToResponse(client, balance, risk);
                })
                .toList();
    }

    /**
     * Obtiene un cliente por ID asegurando que pertenezca al comerciante autenticado,
     * calculando su saldo actual y nivel de riesgo crediticio.
     */
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        List<Transaction> transactions = transactionRepository.findByClientIdOrderByCreatedAtDesc(id);
        BigDecimal balance = riskScoreService.calculateTotalBalance(transactions);
        com.slate.backend.entity.RiskLevel risk = riskScoreService.calculateRiskLevel(transactions, balance);

        return mapToResponse(client, balance, risk);
    }

    /**
     * Crea un nuevo cliente vinculado al comerciante autenticado.
     * Al crearse, su saldo es 0 y su riesgo inicial es BAJO.
     */
    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        User currentUser = getCurrentUser();

        Client client = Client.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .notes(request.getNotes())
                .user(currentUser)
                .build();

        Client saved = clientRepository.save(client);
        return mapToResponse(saved, BigDecimal.ZERO, com.slate.backend.entity.RiskLevel.BAJO);
    }

    /**
     * Actualiza los datos de un cliente existente asegurando que pertenezca al comerciante autenticado.
     */
    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request) {
        User currentUser = getCurrentUser();

        Client client = clientRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        client.setName(request.getName());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setNotes(request.getNotes());

        Client updated = clientRepository.save(client);

        List<Transaction> transactions = transactionRepository.findByClientIdOrderByCreatedAtDesc(id);
        BigDecimal balance = riskScoreService.calculateTotalBalance(transactions);
        com.slate.backend.entity.RiskLevel risk = riskScoreService.calculateRiskLevel(transactions, balance);

        return mapToResponse(updated, balance, risk);
    }

    /**
     * Elimina un cliente verificando que pertenezca al comerciante autenticado.
     */
    @Transactional
    public void deleteClient(Long id) {
        User currentUser = getCurrentUser();

        Client client = clientRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        clientRepository.delete(client);
    }

    /**
     * Retorna la lista consolidada de clientes con deuda pendiente mayor o igual a minBalance,
     * ordenados de mayor a menor deuda (totalBalance descendente).
     */
    @Transactional(readOnly = true)
    public List<DebtorResponse> getDebtors(BigDecimal minBalance) {
        User currentUser = getCurrentUser();
        BigDecimal threshold = minBalance != null ? minBalance : new BigDecimal("0.01");

        List<Client> clients = clientRepository.findByUserId(currentUser.getId());
        if (clients.isEmpty()) {
            return Collections.emptyList();
        }

        // Carga en una sola consulta todas las transacciones del comerciante ordenadas descendentemente por fecha
        List<Transaction> transactions = transactionRepository.findByClientUserIdOrderByCreatedAtDesc(currentUser.getId());

        // Agrupa las transacciones por clientId preservando el orden de fecha
        Map<Long, List<Transaction>> transactionsByClient = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getClient().getId()));

        List<DebtorResponse> debtors = new ArrayList<>();

        for (Client client : clients) {
            List<Transaction> clientTransactions = transactionsByClient.getOrDefault(client.getId(), Collections.emptyList());

            BigDecimal totalBalance = BigDecimal.ZERO;
            for (Transaction t : clientTransactions) {
                BigDecimal amount = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                if (t.getType() == TransactionType.FIADO) {
                    totalBalance = totalBalance.add(amount);
                } else if (t.getType() == TransactionType.PAGO) {
                    totalBalance = totalBalance.subtract(amount);
                }
            }

            if (totalBalance.compareTo(threshold) >= 0) {
                LocalDateTime lastDate = clientTransactions.isEmpty() ? null : clientTransactions.get(0).getCreatedAt();
                debtors.add(DebtorResponse.builder()
                        .clientId(client.getId())
                        .clientName(client.getName())
                        .clientPhone(client.getPhone())
                        .totalBalance(totalBalance)
                        .lastTransactionDate(lastDate)
                        .build());
            }
        }

        debtors.sort(Comparator.comparing(DebtorResponse::getTotalBalance, Comparator.reverseOrder()));
        return debtors;
    }
}
