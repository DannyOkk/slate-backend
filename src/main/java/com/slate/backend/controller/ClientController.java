package com.slate.backend.controller;

import com.slate.backend.dto.ClientRequest;
import com.slate.backend.dto.ClientResponse;
import com.slate.backend.dto.DebtorResponse;
import com.slate.backend.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /**
     * GET /api/clients
     * Obtiene la lista completa de clientes pertenecientes al usuario comerciante autenticado.
     */
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClientsForCurrentUser();
        return ResponseEntity.ok(clients);
    }

    /**
     * GET /api/clients/debtors
     * Retorna la lista consolidada de clientes con saldo deudor pendiente ordenados de mayor a menor deuda.
     */
    @GetMapping("/debtors")
    public ResponseEntity<List<DebtorResponse>> getDebtors(
            @RequestParam(defaultValue = "0.01") BigDecimal minBalance
    ) {
        List<DebtorResponse> debtors = clientService.getDebtors(minBalance);
        return ResponseEntity.ok(debtors);
    }

    /**
     * GET /api/clients/{id}
     * Obtiene el detalle de un cliente por su ID (si pertenece al usuario autenticado).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        ClientResponse client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }

    /**
     * POST /api/clients
     * Crea un nuevo cliente vinculado al usuario comerciante autenticado.
     */
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        ClientResponse createdClient = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    /**
     * PUT /api/clients/{id}
     * Actualiza los datos de un cliente existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request
    ) {
        ClientResponse updatedClient = clientService.updateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    /**
     * DELETE /api/clients/{id}
     * Elimina un cliente por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
