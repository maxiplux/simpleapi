package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Scope;
import app.quantun.simpleapi.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaRegisteredClientRepositoryTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private JpaRegisteredClientRepository jpaRegisteredClientRepository;

    private Client testClient;
    private RegisteredClient testRegisteredClient;

    @BeforeEach
    void setUp() {
        // Setup test client
        Set<ClientAuthenticationMethod> authMethods = new HashSet<>();
        authMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(GrantType.CLIENT_CREDENTIALS);

        Set<Scope> scopes = new HashSet<>();
        scopes.add(Scope.READ);

        testClient = Client.builder()
                .id(1L)
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("{encoded}secret")
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now().plusSeconds(3600))
                .clientAuthenticationMethods(authMethods)
                .authorizationGrantTypes(grantTypes)
                .scope(scopes)
                .accessTokenTimeToLiveSeconds(300)
                .refreshTokenTimeToLiveSeconds(600)
                .build();

        // Mock password encoder - use lenient mode to avoid "unnecessary stubbings" errors
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("{encoded}secret");
    }

    @Test
    @DisplayName("Should save client")
    void shouldSaveClient() {
        // Arrange
        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("secret")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build();

        when(clientRepository.save(any(Client.class))).thenReturn(testClient);

        // Act
        jpaRegisteredClientRepository.save(registeredClient);

        // Assert
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("Should find client by ID")
    void shouldFindClientById() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        // Act
        RegisteredClient result = jpaRegisteredClientRepository.findById("1");

        // Assert
        assertNotNull(result);
        assertEquals("test-client", result.getClientId());
        assertEquals("Test Client", result.getClientName());
        assertEquals("{encoded}secret", result.getClientSecret());
        assertTrue(result.getClientAuthenticationMethods().stream()
                .anyMatch(method -> method.getValue().equals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())));
        assertTrue(result.getAuthorizationGrantTypes().stream()
                .anyMatch(grantType -> grantType.getValue().equals(GrantType.CLIENT_CREDENTIALS.getValue())));
        assertTrue(result.getScopes().contains(Scope.READ.getValue()));

        verify(clientRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return null when client ID not found")
    void shouldReturnNullWhenClientIdNotFound() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        RegisteredClient result = jpaRegisteredClientRepository.findById("1");

        // Assert
        assertNull(result);
        verify(clientRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find client by client ID")
    void shouldFindClientByClientId() {
        // Arrange
        when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

        // Act
        RegisteredClient result = jpaRegisteredClientRepository.findByClientId("test-client");

        // Assert
        assertNotNull(result);
        assertEquals("test-client", result.getClientId());
        assertEquals("Test Client", result.getClientName());

        verify(clientRepository).findByClientId("test-client");
    }

    @Test
    @DisplayName("Should return null when client not found by client ID")
    void shouldReturnNullWhenClientNotFoundByClientId() {
        // Arrange
        when(clientRepository.findByClientId("test-client")).thenReturn(Optional.empty());

        // Act
        RegisteredClient result = jpaRegisteredClientRepository.findByClientId("test-client");

        // Assert
        assertNull(result);
        verify(clientRepository).findByClientId("test-client");
    }

    @Test
    @DisplayName("Should encode client secret when not already encoded")
    void shouldEncodeClientSecretWhenNotAlreadyEncoded() {
        // Arrange
        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("plain-secret") // Not encoded
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build();

        when(clientRepository.save(any(Client.class))).thenReturn(testClient);

        // Act
        jpaRegisteredClientRepository.save(registeredClient);

        // Assert
        verify(passwordEncoder).encode("plain-secret");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("Should not encode client secret when already encoded")
    void shouldNotEncodeClientSecretWhenAlreadyEncoded() {
        // Arrange
        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("{encoded}secret") // Already encoded
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build();

        when(clientRepository.save(any(Client.class))).thenReturn(testClient);

        // Act
        jpaRegisteredClientRepository.save(registeredClient);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
        verify(clientRepository).save(any(Client.class));
    }
}
