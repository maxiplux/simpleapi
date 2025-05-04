package app.quantun.simpleapi.controller;

import app.quantun.simpleapi.exception.ClientNotFoundException;
import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;
import app.quantun.simpleapi.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing OAuth2 client registrations.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Management", description = "APIs for managing OAuth2 client registrations")

@PreAuthorize("hasAnyRole('ROLE_ADMIN')")


public class ClientController {

    private final ClientService clientService;

    /**
     * Get all registered clients.
     *
     * @return list of all clients
     */
    @GetMapping
    @Operation(summary = "Get all clients", description = "Retrieves a list of all registered OAuth2 clients")
    @ApiResponse(responseCode = "200", description = "Clients retrieved successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ClientResponse.class)))
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        log.info("Retrieving all clients");
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * Get a client by ID.
     *
     * @param id the client ID
     * @return the client, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves a client by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content)
    })
    public ResponseEntity<ClientResponse> getClientById(
            @Parameter(description = "ID of the client to retrieve", required = true)
            @PathVariable Long id) {
        log.info("Retrieving client with ID: {}", id);
        return clientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with ID: " + id));
    }

    /**
     * Create a new client.
     *
     * @param clientRequest the client to create
     * @return the created client
     */
    @PostMapping
    @Operation(summary = "Create a new client", description = "Creates a new OAuth2 client registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Client created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Client ID already exists",
                    content = @Content)
    })

    public ResponseEntity<ClientResponse> createClient(
            @Parameter(description = "Client details", required = true)
            @Valid @RequestBody ClientRequest clientRequest) {
        log.info("Creating new client with client ID: {}", clientRequest.getClientId());
        ClientResponse createdClient = clientService.createClient(clientRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    /**
     * Update an existing client.
     *
     * @param id            the client ID
     * @param clientRequest the updated client details
     * @return the updated client, or 404 if not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a client", description = "Updates an existing OAuth2 client registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content)
    })
    public ResponseEntity<ClientResponse> updateClient(
            @Parameter(description = "ID of the client to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated client details", required = true)
            @Valid @RequestBody ClientRequest clientRequest) {
        log.info("Updating client with ID: {}", id);
        return clientService.updateClient(id, clientRequest)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with ID: " + id));
    }

    /**
     * Delete a client.
     *
     * @param id the client ID
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a client", description = "Deletes an OAuth2 client registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Client deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "ID of the client to delete", required = true)
            @PathVariable Long id) {
        log.info("Deleting client with ID: {}", id);
        boolean deleted = clientService.deleteClient(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ClientNotFoundException("Client not found with ID: " + id);
        }
    }
}