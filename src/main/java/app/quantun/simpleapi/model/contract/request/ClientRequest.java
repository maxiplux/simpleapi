package app.quantun.simpleapi.model.contract.request;

import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Role;
import app.quantun.simpleapi.model.enums.Scope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for client creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {

    @NotBlank(message = "Client ID is required")
    @Size(min = 3, max = 100, message = "Client ID must be between 3 and 100 characters")
    private String clientId;

    @NotBlank(message = "Client secret is required")
    @Size(min = 8, message = "Client secret must be at least 8 characters")
    private String clientSecret;

    @NotBlank(message = "Client name is required")
    @Size(max = 200, message = "Client name must not exceed 200 characters")
    private String clientName;

    @Size(max = 1000, message = "Client description must not exceed 1000 characters")
    private String clientDescription;

    @Email(message = "Valid email is required")
    private String clientEmail;

    private Set<ClientAuthenticationMethod> clientAuthenticationMethods;

    private Set<GrantType> authorizationGrantTypes;

    private Set<Scope> scope;

    private Set<Role> authorities;

    private Integer accessTokenTimeToLiveSeconds;

    private Integer refreshTokenTimeToLiveSeconds;
}
