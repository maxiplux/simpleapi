package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Scope;
import app.quantun.simpleapi.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RegisteredClientRepository that uses JPA to store client details in a database.
 */
@Service
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Save a registered client to the database.
     *
     * @param registeredClient the client to save
     */
    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        Client client = toEntity(registeredClient);
        clientRepository.save(client);
    }

    /**
     * Find a registered client by ID.
     *
     * @param id the client ID
     * @return the registered client, or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        return clientRepository.findById(Long.parseLong(id))
                .map(this::toObject)
                .orElse(null);
    }

    /**
     * Find a registered client by client ID.
     *
     * @param clientId the client ID
     * @return the registered client, or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "clients", key = "#clientId")
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .map(this::toObject)
                .orElse(null);
    }

    /**
     * Convert a Client entity to a RegisteredClient object.
     *
     * @param client the client entity
     * @return the registered client object
     */
    private RegisteredClient toObject(Client client) {
        Set<org.springframework.security.oauth2.core.ClientAuthenticationMethod> authenticationMethods = client.getClientAuthenticationMethods().stream()
                .map(method -> new org.springframework.security.oauth2.core.ClientAuthenticationMethod(method.getValue()))
                .collect(Collectors.toSet());

        Set<AuthorizationGrantType> grantTypes = client.getAuthorizationGrantTypes().stream()
                .map(grantType -> new AuthorizationGrantType(grantType.getValue()))
                .collect(Collectors.toSet());

        RegisteredClient.Builder builder = RegisteredClient.withId(client.getId().toString())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientIdIssuedAt(client.getClientIdIssuedAt())
                .clientSecretExpiresAt(client.getClientSecretExpiresAt())
                .clientName(client.getClientName());

        // Add authentication methods
        authenticationMethods.forEach(builder::clientAuthenticationMethod);

        // Add grant types
        grantTypes.forEach(builder::authorizationGrantType);

        // Add scopes
        if (client.getScope() != null) {
            client.getScope().stream()
                    .map(Scope::getValue)
                    .forEach(builder::scope);
        }

        // Configure client settings with default values
        ClientSettings clientSettings = ClientSettings.builder()
                .requireProofKey(false)
                .requireAuthorizationConsent(false)
                .build();
        builder.clientSettings(clientSettings);

        // Configure token settings
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();

        if (client.getAccessTokenTimeToLiveSeconds() != null) {
            tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenTimeToLiveSeconds()));
        }
        if (client.getRefreshTokenTimeToLiveSeconds() != null) {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTokenTimeToLiveSeconds()));
        }
        builder.tokenSettings(tokenSettingsBuilder.build());

        return builder.build();
    }

    /**
     * Convert a RegisteredClient object to a Client entity.
     *
     * @param registeredClient the registered client object
     * @return the client entity
     */
    private Client toEntity(RegisteredClient registeredClient) {
        Set<ClientAuthenticationMethod> authenticationMethods = registeredClient.getClientAuthenticationMethods().stream()
                .map(method -> ClientAuthenticationMethod.fromValue(method.getValue()))
                .collect(Collectors.toSet());

        Set<GrantType> grantTypes = registeredClient.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .map(GrantType::fromValue)
                .collect(Collectors.toSet());

        Set<Scope> scopes = registeredClient.getScopes().stream()
                .map(Scope::fromValue)
                .collect(Collectors.toSet());

        Client client = Client.builder()
                .clientId(registeredClient.getClientId())
                .clientName(registeredClient.getClientName())
                .clientIdIssuedAt(registeredClient.getClientIdIssuedAt())
                .clientSecretExpiresAt(registeredClient.getClientSecretExpiresAt())
                .clientAuthenticationMethods(authenticationMethods)
                .authorizationGrantTypes(grantTypes)
                .scope(scopes)
                .build();

        // Set ID if it's an existing client
        if (registeredClient.getId() != null) {
            try {
                client.setId(Long.parseLong(registeredClient.getId()));
            } catch (NumberFormatException e) {
                // Ignore if ID is not a number
            }
        }

        // Set client secret (encode if it's not already encoded)
        String clientSecret = registeredClient.getClientSecret();
        if (clientSecret != null && !clientSecret.startsWith("{")) {
            clientSecret = passwordEncoder.encode(clientSecret);
        }
        client.setClientSecret(clientSecret);

        // Set token settings
        TokenSettings tokenSettings = registeredClient.getTokenSettings();
        if (tokenSettings.getAccessTokenTimeToLive() != null) {
            client.setAccessTokenTimeToLiveSeconds((int) tokenSettings.getAccessTokenTimeToLive().toSeconds());
        }
        if (tokenSettings.getRefreshTokenTimeToLive() != null) {
            client.setRefreshTokenTimeToLiveSeconds((int) tokenSettings.getRefreshTokenTimeToLive().toSeconds());
        }

        return client;
    }
}
