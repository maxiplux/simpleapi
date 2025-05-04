package app.quantun.simpleapi.service.impl;

import app.quantun.simpleapi.exception.ClientAlreadyExists;
import app.quantun.simpleapi.model.contract.dto.ClientMapper;
import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;
import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.repository.ClientRepository;
import app.quantun.simpleapi.service.ClientService;
import app.quantun.simpleapi.service.JpaRegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the ClientService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final JpaRegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientMapper clientMapper;

    /**
     * Get all clients.
     *
     * @return list of all clients
     */
    @Override
    @Cacheable(value = "clients")
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        log.info("Retrieving all clients");
        List<Client> clients = clientRepository.findAll();
        return clientMapper.toResponseList(clients);
    }

    /**
     * Get a client by ID.
     *
     * @param id the client ID
     * @return the client, or empty if not found
     */
    @Override
    @Cacheable(value = "clients", key = "#id")
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getClientById(Long id) {
        log.info("Retrieving client with ID: {}", id);
        return clientRepository.findById(id)
                .map(clientMapper::toResponse);
    }

    /**
     * Get a client by client ID.
     *
     * @param clientId the client ID
     * @return the client, or empty if not found
     */
    @Override
    @Cacheable(value = "clients", key = "#clientId")
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getClientByClientId(String clientId) {
        log.info("Retrieving client with client ID: {}", clientId);
        return clientRepository.findByClientId(clientId)
                .map(clientMapper::toResponse);
    }

    /**
     * Create a new client.
     *
     * @param clientRequest the client request
     * @return the created client
     */
    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse createClient(ClientRequest clientRequest) {
        log.info("Creating new client with client ID: {}", clientRequest.getClientId());

        // Check if client ID already exists
        if (clientRepository.existsByClientId(clientRequest.getClientId())) {
            log.error("Client with client ID {} already exists", clientRequest.getClientId());
            throw new ClientAlreadyExists("Client with client ID " + clientRequest.getClientId() + " already exists");
        }

        // Convert request to entity
        Client client = clientMapper.toEntity(clientRequest);

        // Encode client secret
        client.setClientSecret(passwordEncoder.encode(clientRequest.getClientSecret()));

        // Save client to database
        Client savedClient = clientRepository.save(client);
        log.debug("Saved client with ID: {}", savedClient.getId());

        // Create and save RegisteredClient
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(savedClient.getClientId());
        if (registeredClient == null) {
            log.debug("Creating new RegisteredClient for client ID: {}", savedClient.getClientId());
            registeredClient = toRegisteredClient(savedClient);
            registeredClientRepository.save(registeredClient);
        }

        return clientMapper.toResponse(savedClient);
    }

    /**
     * Update an existing client.
     *
     * @param id            the client ID
     * @param clientRequest the client request
     * @return the updated client, or empty if not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public Optional<ClientResponse> updateClient(Long id, ClientRequest clientRequest) {
        log.info("Updating client with ID: {}", id);

        return clientRepository.findById(id)
                .map(existingClient -> {
                    // Update client entity
                    clientMapper.updateEntity(clientRequest, existingClient);

                    // Encode client secret if provided
                    if (clientRequest.getClientSecret() != null && !clientRequest.getClientSecret().isEmpty()) {
                        existingClient.setClientSecret(passwordEncoder.encode(clientRequest.getClientSecret()));
                    }

                    // Save updated client
                    Client updatedClient = clientRepository.save(existingClient);
                    log.debug("Updated client with ID: {}", updatedClient.getId());

                    // Update RegisteredClient
                    RegisteredClient registeredClient = registeredClientRepository.findById(id.toString());
                    if (registeredClient != null) {
                        log.debug("Updating RegisteredClient for client ID: {}", updatedClient.getClientId());
                        registeredClient = toRegisteredClient(updatedClient);
                        registeredClientRepository.save(registeredClient);
                    }

                    return clientMapper.toResponse(updatedClient);
                });
    }

    /**
     * Delete a client.
     *
     * @param id the client ID
     * @return true if deleted, false if not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public boolean deleteClient(Long id) {
        log.info("Deleting client with ID: {}", id);

        return clientRepository.findById(id)
                .map(client -> {
                    clientRepository.delete(client);
                    log.debug("Deleted client with ID: {}", id);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Check if a client with the given client ID exists.
     *
     * @param clientId the client ID
     * @return true if exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByClientId(String clientId) {
        log.debug("Checking if client with client ID {} exists", clientId);
        return clientRepository.existsByClientId(clientId);
    }

    /**
     * Convert a Client entity to a RegisteredClient.
     *
     * @param client the client entity
     * @return the registered client
     */
    private RegisteredClient toRegisteredClient(Client client) {
        // This method is simplified as the actual conversion is handled by JpaRegisteredClientRepository
        return registeredClientRepository.findByClientId(client.getClientId());
    }
}