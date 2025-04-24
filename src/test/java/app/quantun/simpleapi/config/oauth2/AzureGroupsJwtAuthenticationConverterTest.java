package app.quantun.simpleapi.config.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the AzureGroupsJwtAuthenticationConverter.
 */
@ExtendWith(MockitoExtension.class)
class AzureGroupsJwtAuthenticationConverterTest {

    @Mock
    private GroupIdToNameConverter groupConverter;

    @Mock
    private AzureGroupResolver azureGroupResolver;

    @InjectMocks
    private AzureGroupsJwtAuthenticationConverter converter;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        // Create a mock JWT token with groups claim
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://login.microsoftonline.com/tenant-id/v2.0");
        claims.put("groups", Arrays.asList("group1", "group2", "group3"));
        claims.put("preferred_username", "test.user@example.com");
        claims.put("name", "Test User");
        claims.put("oid", "object-id-123");
        claims.put("tid", "tenant-id-123");

        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );
    }

    @Test
    @DisplayName("Should convert JWT to authorities")
    void shouldConvertJwtToAuthorities() {
        // Arrange
        when(azureGroupResolver.resolveGroupName("group1")).thenReturn("ROLE_ADMIN");
        when(azureGroupResolver.resolveGroupName("group2")).thenReturn("ROLE_USER");
        when(azureGroupResolver.resolveGroupName("group3")).thenReturn("ROLE_VIEWER");
        when(azureGroupResolver.enrichUserProfileWithGraphData(any(Jwt.class))).thenReturn(new app.quantun.simpleapi.model.dto.UserProfile());

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).hasSize(3);
        assertThat(authorities).contains(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_VIEWER")
        );
        verify(azureGroupResolver, times(3)).enrichUserProfileWithGraphData(jwt);
        verify(azureGroupResolver, times(3)).resolveGroupName(anyString());
    }

    @Test
    @DisplayName("Should handle JWT without groups claim")
    void shouldHandleJwtWithoutGroupsClaim() {
        // Arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://login.microsoftonline.com/tenant-id/v2.0");
        // No groups claim
        claims.put("preferred_username", "test.user@example.com");

        Jwt jwtWithoutGroups = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwtWithoutGroups);

        // Assert
        assertThat(authorities).isEmpty();
        verify(azureGroupResolver, never()).enrichUserProfileWithGraphData(any(Jwt.class));
        verify(azureGroupResolver, never()).resolveGroupName(anyString());
    }

    @Test
    @DisplayName("Should handle empty groups claim")
    void shouldHandleEmptyGroupsClaim() {
        // Arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://login.microsoftonline.com/tenant-id/v2.0");
        claims.put("groups", Arrays.asList()); // Empty groups
        claims.put("preferred_username", "test.user@example.com");

        Jwt jwtWithEmptyGroups = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        Collection<GrantedAuthority> authorities = converter.convert(jwtWithEmptyGroups);

        // Assert
        assertThat(authorities).isEmpty();
        verify(azureGroupResolver, never()).enrichUserProfileWithGraphData(any(Jwt.class));
        verify(azureGroupResolver, never()).resolveGroupName(anyString());
    }
}
