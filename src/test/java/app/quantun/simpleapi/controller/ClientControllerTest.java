package app.quantun.simpleapi.controller;

import app.quantun.simpleapi.exception.ClientAlreadyExists;
import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;
import app.quantun.simpleapi.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@Import({ClientControllerTest.TestConfig.class, ClientControllerTest.SecurityTestConfig.class})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ObjectMapper objectMapper;
    private ClientRequest clientRequest;
    private ClientResponse clientResponse;
    private List<ClientResponse> clientResponses;

    @BeforeEach
    void setUp() {
        // Setup test data
        clientRequest = ClientRequest.builder()
                .clientId("test-client")
                .clientName("Test Client")
                .clientSecret("secret12345") // At least 8 characters to meet validation requirements
                .build();

        clientResponse = ClientResponse.builder()
                .id(1L)
                .clientId("test-client")
                .clientName("Test Client")
                .clientIdIssuedAt(Instant.now())
                .build();

        ClientResponse clientResponse2 = ClientResponse.builder()
                .id(2L)
                .clientId("test-client-2")
                .clientName("Test Client 2")
                .clientIdIssuedAt(Instant.now())
                .build();

        clientResponses = Arrays.asList(clientResponse, clientResponse2);
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when authenticated but not admin")
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenAuthenticatedButNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return all clients when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAllClients_whenAuthenticatedAsAdmin() throws Exception {
        when(clientService.getAllClients()).thenReturn(clientResponses);

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].clientId", is("test-client")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].clientId", is("test-client-2")));

        verify(clientService).getAllClients();
    }

    @Test
    @DisplayName("Should return client by ID when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnClientById_whenAuthenticatedAsAdmin() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(Optional.of(clientResponse));

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isOk());


    }

    @Test
    @DisplayName("Should return 404 when client not found")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404_whenClientNotFound() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isNotFound());

        verify(clientService).getClientById(1L);
    }

    @Test
    @DisplayName("Should create client when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateClient_whenAuthenticatedAsAdmin() throws Exception {
        when(clientService.createClient(any(ClientRequest.class))).thenReturn(clientResponse);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.clientId", is("test-client")));

        verify(clientService).createClient(any(ClientRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when client already exists")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400_whenClientAlreadyExists() throws Exception {
        when(clientService.createClient(any(ClientRequest.class)))
                .thenThrow(new ClientAlreadyExists("Client with client ID test-client already exists"));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("Should update client when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateClient_whenAuthenticatedAsAdmin() throws Exception {
        when(clientService.updateClient(eq(1L), any(ClientRequest.class))).thenReturn(Optional.of(clientResponse));

        mockMvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.clientId", is("test-client")));

    }

    @Test
    @DisplayName("Should return 404 when updating non-existent client")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404_whenUpdatingNonExistentClient() throws Exception {
        when(clientService.updateClient(eq(1L), any(ClientRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRequest)))
                .andDo(print())
                .andExpect(status().isNotFound());

    }

    @Test
    @DisplayName("Should delete client when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteClient_whenAuthenticatedAsAdmin() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/clients/1"))
                .andExpect(status().isNoContent());

        verify(clientService).deleteClient(1L);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent client")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404_whenDeletingNonExistentClient() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/clients/1"))
                .andDo(print())
                .andExpect(status().isNotFound());

    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ClientService clientService() {
            return Mockito.mock(ClientService.class);
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated())
                    .httpBasic(httpBasic -> httpBasic
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.setStatus(401);
                            }))
                    .build();
        }
    }
}
