package app.quantun.simpleapi.model.contract.response;

import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Role;
import app.quantun.simpleapi.model.enums.Scope;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * DTO for client response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResponse {

    private Long id;

    private String clientId;

    // Client secret is not included in the response for security reasons

    private String clientName;

    private String clientDescription;

    private String clientEmail;

    private Instant clientIdIssuedAt;

    private Instant clientSecretExpiresAt;

    private Set<ClientAuthenticationMethod> clientAuthenticationMethods;

    private Set<GrantType> authorizationGrantTypes;

    private Set<Scope> scope;

    private Set<Role> authorities;

    private Integer accessTokenTimeToLiveSeconds;

    private Integer refreshTokenTimeToLiveSeconds;
}
