package app.quantun.simpleapi.config.security;

import app.quantun.simpleapi.model.entity.Client;
import app.quantun.simpleapi.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Collections;

/**
 * Customizes JWT tokens by adding additional claims.
 * Adds client information, scopes, and authorities to the token.
 */

@RequiredArgsConstructor
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final ClientRepository clientRepository;

    /**
     * Customize the JWT token by adding additional claims.
     *
     * @param context the JWT encoding context
     */
    @Override
    public void customize(JwtEncodingContext context) {
        // Only customize access tokens
        if ("access_token".equals(context.getTokenType().getValue())) {
            // Get client ID from the token request
            String clientId = context.getRegisteredClient().getClientId();

            // Find client in the database
            clientRepository.findByClientId(clientId).ifPresent(client -> {
                // Add client profile information
                addClientProfileClaims(context, client);

                // Add authorities
                addAuthoritiesClaims(context, client);
            });

            // Add scopes (already in the token, but we can customize the claim name if needed)
            context.getClaims().claim("scope", context.getAuthorizedScopes());
        }
    }

    /**
     * Add client profile information to the token.
     *
     * @param context the JWT encoding context
     * @param client  the client entity
     */
    private void addClientProfileClaims(JwtEncodingContext context, Client client) {
        if (client.getClientName() != null) {
            context.getClaims().claim("client_name", client.getClientName());
        }

        if (client.getClientEmail() != null) {
            context.getClaims().claim("email", client.getClientEmail());
        }


        if (client.getClientDescription() != null) {
            context.getClaims().claim("description", client.getClientDescription());
        }
    }

    /**
     * Add authorities to the token.
     *
     * @param context the JWT encoding context
     * @param client  the client entity
     */
    private void addAuthoritiesClaims(JwtEncodingContext context, Client client) {
        var authorities = client.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            context.getClaims().claim("authorities", authorities);
        } else {
            // Add default authority if none specified
            context.getClaims().claim("authorities", Collections.singleton("ROLE_CLIENT"));
        }
    }
}
