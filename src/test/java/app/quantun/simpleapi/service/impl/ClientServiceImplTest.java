package app.quantun.simpleapi.service.impl;

import app.quantun.simpleapi.exception.ClientAlreadyExists;
import app.quantun.simpleapi.model.contract.dto.ClientMapper;
import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;
import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Scope;
import app.quantun.simpleapi.repository.ClientRepository;
import app.quantun.simpleapi.service.JpaRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private JpaRegisteredClientRepository registeredClientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client testClient;
    private ClientRequest testClientRequest;
    private ClientResponse testClientResponse;
    private RegisteredClient testRegisteredClient;

    @BeforeEach
    void setUp() {
        // Setup test client
        Set<ClientAuthenticationMethod> authMethods = new HashSet<>();
        authMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(GrantType.CLIENT_CREDENTIALS);

        Set<Scope> scope = new HashSet<>();
        scope.add(Scope.READ);

        testClient = Client.builder()
                .id(1L)
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("encoded-secret")
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now().plusSeconds(3600))
                .clientAuthenticationMethods(authMethods)
                .authorizationGrantTypes(grantTypes)
                .scope(scope)
                .accessTokenTimeToLiveSeconds(300)
                .refreshTokenTimeToLiveSeconds(600)
                .build();

        // Setup test client request
        testClientRequest = ClientRequest.builder()
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("secret")
                .build();

        // Setup test client response
        testClientResponse = ClientResponse.builder()
                .id(1L)
                .clientId("test-client")
                .clientName("Test Client")
                .clientIdIssuedAt(Instant.now())
                .build();

        // Use lenient mode for all stubbings to avoid "unnecessary stubbings" errors
        // Mock password encoder
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-secret");

        // Mock client mapper
        lenient().when(clientMapper.toEntity(any(ClientRequest.class))).thenReturn(testClient);
        lenient().when(clientMapper.toResponse(any(Client.class))).thenReturn(testClientResponse);
        lenient().when(clientMapper.toResponseList(anyList())).thenReturn(Arrays.asList(testClientResponse));
    }

    @Test
    @DisplayName("Should get all clients")
    void shouldGetAllClients() {
        // Arrange
        when(clientRepository.findAll()).thenReturn(Arrays.asList(testClient));

        // Act
        List<ClientResponse> result = clientService.getAllClients();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-client", result.get(0).getClientId());

        verify(clientRepository).findAll();
        verify(clientMapper).toResponseList(anyList());
    }

    @Test
    @DisplayName("Should get client by ID")
    void shouldGetClientById() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        // Act
        Optional<ClientResponse> result = clientService.getClientById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test-client", result.get().getClientId());

        verify(clientRepository).findById(1L);
        verify(clientMapper).toResponse(testClient);
    }

    @Test
    @DisplayName("Should return empty when client not found by ID")
    void shouldReturnEmptyWhenClientNotFoundById() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<ClientResponse> result = clientService.getClientById(1L);

        // Assert
        assertFalse(result.isPresent());

        verify(clientRepository).findById(1L);
        verify(clientMapper, never()).toResponse(any(Client.class));
    }

    @Test
    @DisplayName("Should get client by client ID")
    void shouldGetClientByClientId() {
        // Arrange
        when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

        // Act
        Optional<ClientResponse> result = clientService.getClientByClientId("test-client");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test-client", result.get().getClientId());

        verify(clientRepository).findByClientId("test-client");
        verify(clientMapper).toResponse(testClient);
    }

    @Test
    @DisplayName("Should create client")
    void shouldCreateClient() {
        // Arrange
        when(clientRepository.existsByClientId("test-client")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(registeredClientRepository.findByClientId("test-client")).thenReturn(null);

        // Act
        ClientResponse result = clientService.createClient(testClientRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test-client", result.getClientId());

        verify(clientRepository).existsByClientId("test-client");
        verify(clientMapper).toEntity(testClientRequest);
        verify(passwordEncoder).encode("secret");
        verify(clientRepository).save(testClient);
        // The method is called twice: once in createClient and once in toRegisteredClient
        verify(registeredClientRepository, times(2)).findByClientId("test-client");
        verify(clientMapper).toResponse(testClient);
    }

    @Test
    @DisplayName("Should throw exception when client already exists")
    void shouldThrowExceptionWhenClientAlreadyExists() {
        // Arrange
        when(clientRepository.existsByClientId("test-client")).thenReturn(true);

        // Act & Assert
        assertThrows(ClientAlreadyExists.class, () -> clientService.createClient(testClientRequest));

        verify(clientRepository).existsByClientId("test-client");
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Should update client")
    void shouldUpdateClient() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(registeredClientRepository.findById("1")).thenReturn(mock(RegisteredClient.class));

        // Act
        Optional<ClientResponse> result = clientService.updateClient(1L, testClientRequest);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test-client", result.get().getClientId());

        verify(clientRepository).findById(1L);
        verify(clientMapper).updateEntity(testClientRequest, testClient);
        verify(passwordEncoder).encode("secret");
        verify(clientRepository).save(testClient);
        verify(registeredClientRepository).findById("1");
        verify(clientMapper).toResponse(testClient);
    }

    @Test
    @DisplayName("Should return empty when updating non-existent client")
    void shouldReturnEmptyWhenUpdatingNonExistentClient() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<ClientResponse> result = clientService.updateClient(1L, testClientRequest);

        // Assert
        assertFalse(result.isPresent());

        verify(clientRepository).findById(1L);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Should delete client")
    void shouldDeleteClient() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        // Act
        boolean result = clientService.deleteClient(1L);

        // Assert
        assertTrue(result);

        verify(clientRepository).findById(1L);
        verify(clientRepository).delete(testClient);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent client")
    void shouldReturnFalseWhenDeletingNonExistentClient() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = clientService.deleteClient(1L);

        // Assert
        assertFalse(result);

        verify(clientRepository).findById(1L);
        verify(clientRepository, never()).delete(any(Client.class));
    }

    @Test
    @DisplayName("Should check if client exists by client ID")
    void shouldCheckIfClientExistsByClientId() {
        // Arrange
        when(clientRepository.existsByClientId("test-client")).thenReturn(true);

        // Act
        boolean result = clientService.existsByClientId("test-client");

        // Assert
        assertTrue(result);

        verify(clientRepository).existsByClientId("test-client");
    }
}
