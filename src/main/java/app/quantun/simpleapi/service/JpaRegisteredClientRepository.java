package app.quantun.simpleapi.service;

import app.quantun.simpleapi.entity.Client;
import app.quantun.simpleapi.repository.ClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.stream.Collectors;

@Service
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final TokenSettings tokenSettings;

    public JpaRegisteredClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
        this.tokenSettings = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(5))
                .refreshTokenTimeToLive(Duration.ofMinutes(60))
                .build();
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        // Check if client already exists
        Client client = clientRepository.findByClientId(registeredClient.getClientId())
                .orElse(new Client());

        // Update client properties
        client.setClientId(registeredClient.getClientId());
        client.setClientSecret(registeredClient.getClientSecret());
        client.setClientAuthenticationMethods(
                registeredClient.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .collect(Collectors.toSet()));
        client.setAuthorizationGrantTypes(
                registeredClient.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .collect(Collectors.toSet()));
        client.setScopes(registeredClient.getScopes());
        client.setRedirectUris(registeredClient.getRedirectUris());

        clientRepository.save(client);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(Long.valueOf(id))
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    public RegisteredClient toRegisteredClient(Client client) {
        return RegisteredClient.withId(client.getId().toString())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethods(methods -> client.getClientAuthenticationMethods().forEach(
                        method -> methods.add(new ClientAuthenticationMethod(method))))
                .authorizationGrantTypes(grantTypes -> client.getAuthorizationGrantTypes().forEach(
                        grantType -> grantTypes.add(new AuthorizationGrantType(grantType))))
                .scopes(scopes -> scopes.addAll(client.getScopes()))
                .redirectUris(uris -> uris.addAll(client.getRedirectUris()))
                .tokenSettings(tokenSettings)
                .clientSettings(ClientSettings.builder().build())
                .build();
    }
}
