package app.quantun.simpleapi.model.contract.dto;

import app.quantun.simpleapi.model.contract.request.ClientRequest;
import app.quantun.simpleapi.model.contract.response.ClientResponse;
import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.util.ClientUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.util.List;

/**
 * Mapper interface for converting between Client entity and DTOs.
 */
@Mapper(componentModel = "spring", imports = {Instant.class, ClientUtils.class})
public interface ClientMapper {

    /**
     * Convert a Client entity to a ClientResponse DTO.
     *
     * @param client the client entity
     * @return the client response DTO
     */
    ClientResponse toResponse(Client client);

    /**
     * Convert a list of Client entities to a list of ClientResponse DTOs.
     *
     * @param clients the list of client entities
     * @return the list of client response DTOs
     */
    List<ClientResponse> toResponseList(List<Client> clients);

    /**
     * Convert a ClientRequest DTO to a Client entity.
     *
     * @param request the client request DTO
     * @return the client entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientIdIssuedAt", expression = "java(Instant.now())")
    @Mapping(target = "clientSecretExpiresAt", ignore = true)
    Client toEntity(ClientRequest request);

    /**
     * Update a Client entity with data from a ClientRequest DTO.
     *
     * @param request the client request DTO
     * @param client  the client entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "clientIdIssuedAt", ignore = true)
    @Mapping(target = "clientSecretExpiresAt", ignore = true)
    @Mapping(target = "clientSecret", conditionExpression = "java(request.getClientSecret() != null && !request.getClientSecret().isEmpty())")
    void updateEntity(ClientRequest request, @MappingTarget Client client);
}