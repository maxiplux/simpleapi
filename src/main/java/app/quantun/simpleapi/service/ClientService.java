package app.quantun.simpleapi.service;

import app.quantun.simpleapi.entity.Client;
import app.quantun.simpleapi.repository.ClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final JpaRegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository, 
                         JpaRegisteredClientRepository registeredClientRepository,
                         PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    public Client getClientByClientId(String clientId) {
        return clientRepository.findByClientId(clientId).orElse(null);
    }

    @Transactional
    public Client createClient(Client client) {
        // Check if client already exists by clientId
        Optional<Client> existingClient = clientRepository.findByClientId(client.getClientId());
        if (existingClient.isPresent()) {
            return existingClient.get();
        }

        // Encode client secret if it doesn't already start with {noop}
        if (client.getClientSecret() != null && !client.getClientSecret().startsWith("{noop}")) {
            client.setClientSecret("{noop}" + client.getClientSecret());
        }

        Client savedClient = clientRepository.save(client);

        // Register with Spring Security OAuth2
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(client.getClientId());
        if (registeredClient == null) {
            registeredClientRepository.save(registeredClientRepository.toRegisteredClient(savedClient));
        }

        return savedClient;
    }

    @Transactional
    public Client updateClient(Long id, Client updatedClient) {
        return clientRepository.findById(id)
                .map(client -> {
                    client.setClientId(updatedClient.getClientId());
                    if (updatedClient.getClientSecret() != null && !updatedClient.getClientSecret().isEmpty() &&
                            !updatedClient.getClientSecret().startsWith("{noop}")) {
                        client.setClientSecret("{noop}" + updatedClient.getClientSecret());
                    }
                    client.setClientAuthenticationMethods(updatedClient.getClientAuthenticationMethods());
                    client.setAuthorizationGrantTypes(updatedClient.getAuthorizationGrantTypes());
                    client.setScopes(updatedClient.getScopes());
                    client.setRedirectUris(updatedClient.getRedirectUris());

                    Client savedClient = clientRepository.save(client);

                    // Update in Spring Security OAuth2
                    RegisteredClient registeredClient = registeredClientRepository.findByClientId(client.getClientId());
                    if (registeredClient != null) {
                        registeredClientRepository.save(registeredClientRepository.toRegisteredClient(savedClient));
                    }

                    return savedClient;
                })
                .orElse(null);
    }

    @Transactional
    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }
}
