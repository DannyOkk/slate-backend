package com.slate.backend.service;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private TransactionService transactionService;

    private User merchantUser;
    private Client client;

    @BeforeEach
    void setUp() {
        merchantUser = User.builder()
                .id(1L)
                .name("Comerciante Test")
                .email("comercio@test.com")
                .build();

        client = Client.builder()
                .id(10L)
                .name("Juan Perez")
                .user(merchantUser)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("comercio@test.com", null, Collections.emptyList());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe registrar un fiado correctamente para un cliente del comerciante")
    void createTransaction_success_fiado() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("1500.50"))
                .type(TransactionType.FIADO)
                .description("Compra de víveres")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(100L)
                .client(client)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("comercio@test.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(client));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getClientId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("1500.50");
        assertThat(response.getType()).isEqualTo(TransactionType.FIADO);
        assertThat(response.getDescription()).isEqualTo("Compra de víveres");

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe registrar un pago correctamente para un cliente del comerciante")
    void createTransaction_success_pago() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.PAGO)
                .description("Entrega a cuenta")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(101L)
                .client(client)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("comercio@test.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(client));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getType()).isEqualTo(TransactionType.PAGO);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el cliente no existe o no pertenece al comerciante")
    void createTransaction_clientNotFound_throwsException() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.FIADO)
                .build();

        when(userRepository.findByEmail("comercio@test.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: 999");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe retornar las transacciones ordenadas descendentemente por fecha con el saldo total calculado")
    void getTransactionsByClient_success() {
        Transaction t1 = Transaction.builder()
                .id(1L)
                .client(client)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction t2 = Transaction.builder()
                .id(2L)
                .client(client)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.PAGO)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByEmail("comercio@test.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(t1, t2));

        com.slate.backend.dto.ClientTransactionsSummaryResponse summary = transactionService.getTransactionsByClient(10L);

        assertThat(summary).isNotNull();
        assertThat(summary.getClientId()).isEqualTo(10L);
        assertThat(summary.getClientName()).isEqualTo("Juan Perez");
        // totalBalance = 100.00 (FIADO) - 30.00 (PAGO) = 70.00
        assertThat(summary.getTotalBalance()).isEqualByComparingTo("70.00");
        assertThat(summary.getTransactions()).hasSize(2);
        assertThat(summary.getTransactions().get(0).getId()).isEqualTo(1L);
        assertThat(summary.getTransactions().get(1).getId()).isEqualTo(2L);
    }
}
