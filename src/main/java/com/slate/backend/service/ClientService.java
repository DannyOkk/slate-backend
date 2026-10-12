package com.slate.backend.service;

import com.slate.backend.dto.ClientRequest;
import com.slate.backend.dto.ClientResponse;
import com.slate.backend.entity.Client;
import com.slate.backend.entity.User;
import com.slate.backend.exception.ResourceNotFoundException;
import com.slate.backend.repository.ClientRepository;
import com.slate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

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
     * Mapea una entidad Client a su DTO ClientResponse.
     */
    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .phone(client.getPhone())
                .address(client.getAddress())
                .notes(client.getNotes())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    /**
     * Obtiene todos los clientes asociados al comerciante autenticado.
     */
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClientsForCurrentUser() {
        User currentUser = getCurrentUser();
        return clientRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Obtiene un cliente por ID asegurando que pertenezca al comerciante autenticado.
     */
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        return mapToResponse(client);
    }

    /**
     * Crea un nuevo cliente vinculado al comerciante autenticado.
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
        return mapToResponse(saved);
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
        return mapToResponse(updated);
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
}
