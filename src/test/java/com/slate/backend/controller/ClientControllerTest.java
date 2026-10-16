package com.slate.backend.controller;

import com.slate.backend.dto.DebtorResponse;
import com.slate.backend.exception.GlobalExceptionHandler;
import com.slate.backend.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/clients/debtors - Retorna 200 OK con la lista de deudores")
    void getDebtors_returnsOk() throws Exception {
        DebtorResponse debtor1 = DebtorResponse.builder()
                .clientId(1L)
                .clientName("Juan Deudor")
                .clientPhone("12345678")
                .totalBalance(new BigDecimal("1500.00"))
                .lastTransactionDate(LocalDateTime.now())
                .build();

        DebtorResponse debtor2 = DebtorResponse.builder()
                .clientId(2L)
                .clientName("Maria Deudora")
                .clientPhone("87654321")
                .totalBalance(new BigDecimal("450.00"))
                .lastTransactionDate(LocalDateTime.now().minusDays(1))
                .build();

        when(clientService.getDebtors(any(BigDecimal.class)))
                .thenReturn(List.of(debtor1, debtor2));

        mockMvc.perform(get("/api/clients/debtors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andExpect(jsonPath("$[0].clientName").value("Juan Deudor"))
                .andExpect(jsonPath("$[0].totalBalance").value(1500.00))
                .andExpect(jsonPath("$[1].clientId").value(2))
                .andExpect(jsonPath("$[1].totalBalance").value(450.00));
    }
}
