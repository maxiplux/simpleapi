package app.quantun.simpleapi.config;

import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Role;
import app.quantun.simpleapi.model.enums.Scope;
import app.quantun.simpleapi.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Initializes the database with default clients on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    public static final int MINUTES = 10;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("#{T(Integer).parseInt('${spring.security.oauth2.authorizationserver.token.access-token-time-to-live}'.replaceAll('[^0-9]', ''))}")
    private int accessTokenTtlSeconds;

    @Value("#{T(Integer).parseInt('${spring.security.oauth2.authorizationserver.token.refresh-token-time-to-live}'.replaceAll('[^0-9]', ''))}")
    private int refreshTokenTtlSeconds;


    @Override
    public void run(String... args) {
        // Only initialize if no clients exist
        if (clientRepository.count() == 0) {
            log.info("Initializing default OAuth2 clients");
            createDefaultClients();
        }
    }

    private void createDefaultClients() {
        // Create default client 1
        createClient(
                "mcp-client",
                "secret",
                "MCP Client",
                "Default client for MCP application",
                "mcp@example.com",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(GrantType.CLIENT_CREDENTIALS, GrantType.REFRESH_TOKEN),
                Set.of(Scope.READ, Scope.WRITE),
                Set.of(Role.ROLE_CLIENT),
                this.accessTokenTtlSeconds * MINUTES,
                this.refreshTokenTtlSeconds * MINUTES
        );

        // Create default client 2
        createClient(
                "customer1",
                "customer1",
                "Customer 1",
                "Customer 1 client",
                "customer1@example.com",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(GrantType.CLIENT_CREDENTIALS, GrantType.REFRESH_TOKEN),
                Set.of(Scope.READ, Scope.WRITE),
                Set.of(Role.ROLE_ADMIN),
                this.accessTokenTtlSeconds * MINUTES,
                this.refreshTokenTtlSeconds * MINUTES
        );

        // Create default client 3
        createClient(
                "customer2",
                "customer2",
                "Customer 2",
                "Customer 2 client",
                "customer2@example.com",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(GrantType.CLIENT_CREDENTIALS, GrantType.REFRESH_TOKEN),
                Set.of(Scope.READ, Scope.WRITE),
                Set.of(Role.ROLE_CLIENT),
                this.accessTokenTtlSeconds * MINUTES,
                this.refreshTokenTtlSeconds * MINUTES
        );

        log.info("Default OAuth2 clients created successfully");
    }

    private void createClient(
            String clientId,
            String clientSecret,
            String clientName,
            String clientDescription,
            String clientEmail,
            Set<ClientAuthenticationMethod> authenticationMethods,
            Set<GrantType> grantTypes,
            Set<Scope> scope,
            Set<Role> authorities,
            Integer accessTokenTtlSeconds,
            Integer refreshTokenTtlSeconds
    ) {
        Client client = Client.builder()
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientName(clientName)
                .clientDescription(clientDescription)
                .clientEmail(clientEmail)
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethods(authenticationMethods)
                .authorizationGrantTypes(grantTypes)
                .scope(scope)
                .authorities(authorities)
                .accessTokenTimeToLiveSeconds(accessTokenTtlSeconds)
                .refreshTokenTimeToLiveSeconds(refreshTokenTtlSeconds)
                .build();

        clientRepository.save(client);
        log.info("Created client: {}", clientId);
    }
}
