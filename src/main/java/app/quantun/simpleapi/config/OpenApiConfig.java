package app.quantun.simpleapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://login.microsoftonline.com/tenant-id/v2.0}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "azure_oauth2";

        return new OpenAPI()
                .info(new Info()
                        .title("Products and Categories API")
                        .description("REST API for managing products and categories with H2 database")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Quantun")
                                .email("info@quantun.app")
                                .url("https://quantun.app"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .description("OAuth2 authentication with Azure AD")
                                        .flows(new OAuthFlows()
                                                .implicit(new OAuthFlow()
                                                        .authorizationUrl(issuerUri.replace("/v2.0", "/oauth2/v2.0/authorize"))
                                                        .tokenUrl(issuerUri.replace("/v2.0", "/oauth2/v2.0/token"))
                                                        .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                                .addString("api.access", "Access the API"))
                                                )
                                        )
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
