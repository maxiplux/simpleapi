package app.quantun.simpleapi.model.entity;

import app.quantun.simpleapi.model.enums.ClientAuthenticationMethod;
import app.quantun.simpleapi.model.enums.GrantType;
import app.quantun.simpleapi.model.enums.Role;
import app.quantun.simpleapi.model.enums.Scope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Entity class representing an OAuth2 client registration.
 * Stores client details including authentication methods, grant types, and scopes.
 */
@Entity
@Table(name = "oauth2_registered_client")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_description")
    private String clientDescription;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_authentication_methods",
            joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "authentication_method")
    @Enumerated(EnumType.STRING)
    private Set<ClientAuthenticationMethod> clientAuthenticationMethods;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_grant_types",
            joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type")
    @Enumerated(EnumType.STRING)
    private Set<GrantType> authorizationGrantTypes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_scopes",
            joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    @Enumerated(EnumType.STRING)
    private Set<Scope> scope;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_authorities",
            joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "authority")
    @Enumerated(EnumType.STRING)
    private Set<Role> authorities;

    @Column(name = "access_token_time_to_live_seconds")
    private Integer accessTokenTimeToLiveSeconds;

    @Column(name = "refresh_token_time_to_live_seconds")
    private Integer refreshTokenTimeToLiveSeconds;
}
