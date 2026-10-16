package com.slate.backend.controller;

import com.slate.backend.dto.ClientTransactionsSummaryResponse;
import com.slate.backend.dto.TransactionRequest;
import com.slate.backend.dto.TransactionResponse;
import com.slate.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/{clientId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/clients/{clientId}/transactions
     * Registra un fiado o pago para el cliente especificado.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long clientId,
            @Valid @RequestBody TransactionRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/clients/{clientId}/transactions
     * Devuelve el resumen del cliente con saldo total y el historial de movimientos ordenados por fecha descendente.
     */
    @GetMapping
    public ResponseEntity<ClientTransactionsSummaryResponse> getTransactionsByClient(@PathVariable Long clientId) {
        ClientTransactionsSummaryResponse summary = transactionService.getTransactionsByClient(clientId);
        return ResponseEntity.ok(summary);
    }
}
