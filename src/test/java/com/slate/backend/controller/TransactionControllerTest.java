package com.slate.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.backend.dto.TransactionRequest;
import com.slate.backend.dto.TransactionResponse;
import com.slate.backend.entity.TransactionType;
import com.slate.backend.exception.GlobalExceptionHandler;
import com.slate.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/clients/{clientId}/transactions - Éxito retorna 201 Created")
    void createTransaction_success() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.FIADO)
                .description("Pan y leche")
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .clientId(5L)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.FIADO)
                .description("Pan y leche")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.createTransaction(eq(5L), any(TransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clients/5/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clientId").value(5))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.type").value("FIADO"))
                .andExpect(jsonPath("$.description").value("Pan y leche"));
    }

    @Test
    @DisplayName("POST /api/clients/{clientId}/transactions - Monto negativo o cero retorna 400 Bad Request")
    void createTransaction_invalidAmount_returnsBadRequest() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("0.00"))
                .type(TransactionType.FIADO)
                .build();

        mockMvc.perform(post("/api/clients/5/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.amount").exists());
    }

    @Test
    @DisplayName("POST /api/clients/{clientId}/transactions - Tipo nulo retorna 400 Bad Request")
    void createTransaction_nullType_returnsBadRequest() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .type(null)
                .build();

        mockMvc.perform(post("/api/clients/5/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.type").exists());
    }

    @Test
    @DisplayName("GET /api/clients/{clientId}/transactions - Retorna 200 OK con resumen y saldo total")
    void getTransactionsByClient_returnsOk() throws Exception {
        TransactionResponse response1 = TransactionResponse.builder()
                .id(1L)
                .clientId(5L)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.FIADO)
                .build();

        TransactionResponse response2 = TransactionResponse.builder()
                .id(2L)
                .clientId(5L)
                .amount(new BigDecimal("40.00"))
                .type(TransactionType.PAGO)
                .build();

        com.slate.backend.dto.ClientTransactionsSummaryResponse summary = com.slate.backend.dto.ClientTransactionsSummaryResponse.builder()
                .clientId(5L)
                .clientName("Cliente Prueba")
                .totalBalance(new BigDecimal("60.00"))
                .transactions(List.of(response1, response2))
                .build();

        when(transactionService.getTransactionsByClient(5L))
                .thenReturn(summary);

        mockMvc.perform(get("/api/clients/5/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(5))
                .andExpect(jsonPath("$.clientName").value("Cliente Prueba"))
                .andExpect(jsonPath("$.totalBalance").value(60.00))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].type").value("FIADO"))
                .andExpect(jsonPath("$.transactions[1].type").value("PAGO"));
    }
}
