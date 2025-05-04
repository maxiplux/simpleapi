package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for client operations.
 */
public interface ClientService {

    /**
     * Get all clients.
     *
     * @return list of all clients
     */
    List<ClientResponse> getAllClients();

    /**
     * Get a client by ID.
     *
     * @param id the client ID
     * @return the client, or empty if not found
     */
    Optional<ClientResponse> getClientById(Long id);

    /**
     * Get a client by client ID.
     *
     * @param clientId the client ID
     * @return the client, or empty if not found
     */
    Optional<ClientResponse> getClientByClientId(String clientId);

    /**
     * Create a new client.
     *
     * @param clientRequest the client request
     * @return the created client
     */
    ClientResponse createClient(ClientRequest clientRequest);

    /**
     * Update an existing client.
     *
     * @param id            the client ID
     * @param clientRequest the client request
     * @return the updated client, or empty if not found
     */
    Optional<ClientResponse> updateClient(Long id, ClientRequest clientRequest);

    /**
     * Delete a client.
     *
     * @param id the client ID
     * @return true if deleted, false if not found
     */
    boolean deleteClient(Long id);

    /**
     * Check if a client with the given client ID exists.
     *
     * @param clientId the client ID
     * @return true if exists, false otherwise
     */
    boolean existsByClientId(String clientId);
}