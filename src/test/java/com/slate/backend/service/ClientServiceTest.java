package com.slate.backend.service;

import com.slate.backend.dto.DebtorResponse;
import com.slate.backend.entity.Client;
import com.slate.backend.entity.Transaction;
import com.slate.backend.entity.TransactionType;
import com.slate.backend.entity.User;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ClientService clientService;

    private User merchantUser;
    private Client client1;
    private Client client2;
    private Client client3;

    @BeforeEach
    void setUp() {
        merchantUser = User.builder()
                .id(1L)
                .name("Comerciante Don Pepe")
                .email("pepe@comercio.com")
                .build();

        client1 = Client.builder()
                .id(10L)
                .name("Cliente Deuda Grande")
                .phone("11111111")
                .user(merchantUser)
                .build();

        client2 = Client.builder()
                .id(20L)
                .name("Cliente Deuda Chica")
                .phone("22222222")
                .user(merchantUser)
                .build();

        client3 = Client.builder()
                .id(30L)
                .name("Cliente Sin Deuda")
                .phone("33333333")
                .user(merchantUser)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("pepe@comercio.com", null, Collections.emptyList());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getDebtors - Retorna deudores filtrados por minBalance y ordenados de mayor a menor deuda")
    void getDebtors_success_orderedAndFiltered() {
        LocalDateTime now = LocalDateTime.now();

        // Cliente 1: FIADO 1000 - PAGO 200 = Deuda 800
        Transaction t1_1 = Transaction.builder()
                .id(1L)
                .client(client1)
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.PAGO)
                .createdAt(now.minusHours(1))
                .build();

        Transaction t1_2 = Transaction.builder()
                .id(2L)
                .client(client1)
                .amount(new BigDecimal("1000.00"))
                .type(TransactionType.FIADO)
                .createdAt(now.minusDays(1))
                .build();

        // Cliente 2: FIADO 150 = Deuda 150
        Transaction t2_1 = Transaction.builder()
                .id(3L)
                .client(client2)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.FIADO)
                .createdAt(now.minusHours(3))
                .build();

        // Cliente 3: FIADO 100 - PAGO 100 = Deuda 0
        Transaction t3_1 = Transaction.builder()
                .id(4L)
                .client(client3)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.PAGO)
                .createdAt(now.minusDays(2))
                .build();

        Transaction t3_2 = Transaction.builder()
                .id(5L)
                .client(client3)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.FIADO)
                .createdAt(now.minusDays(3))
                .build();

        // findByClientUserIdOrderByCreatedAtDesc devuelve la lista ya ordenada desc por fecha
        List<Transaction> transactions = List.of(t1_1, t2_1, t1_2, t3_1, t3_2);

        when(userRepository.findByEmail("pepe@comercio.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByUserId(1L)).thenReturn(List.of(client1, client2, client3));
        when(transactionRepository.findByClientUserIdOrderByCreatedAtDesc(1L)).thenReturn(transactions);

        List<DebtorResponse> debtors = clientService.getDebtors(new BigDecimal("0.01"));

        // Cliente 3 (deuda 0) debe quedar excluido
        assertThat(debtors).hasSize(2);

        // Primero debe aparecer Cliente 1 con deuda 800
        assertThat(debtors.get(0).getClientId()).isEqualTo(10L);
        assertThat(debtors.get(0).getClientName()).isEqualTo("Cliente Deuda Grande");
        assertThat(debtors.get(0).getClientPhone()).isEqualTo("11111111");
        assertThat(debtors.get(0).getTotalBalance()).isEqualByComparingTo("800.00");
        assertThat(debtors.get(0).getLastTransactionDate()).isEqualTo(now.minusHours(1));

        // Segundo debe aparecer Cliente 2 con deuda 150
        assertThat(debtors.get(1).getClientId()).isEqualTo(20L);
        assertThat(debtors.get(1).getClientName()).isEqualTo("Cliente Deuda Chica");
        assertThat(debtors.get(1).getTotalBalance()).isEqualByComparingTo("150.00");
        assertThat(debtors.get(1).getLastTransactionDate()).isEqualTo(now.minusHours(3));
    }

    @Test
    @DisplayName("getDebtors - Filtra respetando el minBalance personalizado")
    void getDebtors_withCustomMinBalance() {
        Transaction t1 = Transaction.builder()
                .id(1L)
                .client(client1)
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction t2 = Transaction.builder()
                .id(2L)
                .client(client2)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.FIADO)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("pepe@comercio.com")).thenReturn(Optional.of(merchantUser));
        when(clientRepository.findByUserId(1L)).thenReturn(List.of(client1, client2));
        when(transactionRepository.findByClientUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(t1, t2));

        // Filtro con minBalance = 200.00 -> Solo cliente 1 (500) pasa el filtro
        List<DebtorResponse> debtors = clientService.getDebtors(new BigDecimal("200.00"));

        assertThat(debtors).hasSize(1);
        assertThat(debtors.get(0).getClientId()).isEqualTo(10L);
        assertThat(debtors.get(0).getTotalBalance()).isEqualByComparingTo("500.00");
    }
}
