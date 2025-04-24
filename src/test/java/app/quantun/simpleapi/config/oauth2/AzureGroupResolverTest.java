package app.quantun.simpleapi.config.oauth2;

import app.quantun.simpleapi.model.dto.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AzureGroupResolver.
 * 
 * Note: This test focuses on the JWT token processing functionality.
 * Testing methods that interact with Microsoft Graph API would require
 * complex mocking of the GraphServiceClient and is beyond the scope of these tests.
 */
@ExtendWith(MockitoExtension.class)
class AzureGroupResolverTest {

    private AzureGroupResolver azureGroupResolver;

    @BeforeEach
    void setUp() {
        // Create the AzureGroupResolver with test values
        azureGroupResolver = new AzureGroupResolver(
                "tenant-id",
                "client-id",
                "client-secret",
                "https://graph.microsoft.com/.default"
        );
        
        // Initialize the group cache with reflection
        ReflectionTestUtils.setField(azureGroupResolver, "groupCache", new ConcurrentHashMap<>());
    }

    @Test
    @DisplayName("Should get user profile from JWT with groups claim")
    void shouldGetUserProfileFromJwtWithGroupsClaim() {
        // Arrange
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

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        UserProfile userProfile = azureGroupResolver.getUserProfile(jwt);

        // Assert
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getDisplayName()).isEqualTo("Test User");
        assertThat(userProfile.getEmail()).isEqualTo("test.user@example.com");
        assertThat(userProfile.getId()).isEqualTo("user123");
        assertThat(userProfile.getObjectId()).isEqualTo("object-id-123");
        assertThat(userProfile.getTenantId()).isEqualTo("tenant-id-123");
        
        // Verify roles extraction
        List<String> roles = userProfile.getRoles();
        assertThat(roles).hasSize(3);
        assertThat(roles).containsExactlyInAnyOrder("group1", "group2", "group3");
    }

    @Test
    @DisplayName("Should get user profile from JWT with roles claim")
    void shouldGetUserProfileFromJwtWithRolesClaim() {
        // Arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://login.microsoftonline.com/tenant-id/v2.0");
        claims.put("roles", Arrays.asList("ROLE_ADMIN", "ROLE_USER"));
        claims.put("preferred_username", "test.user@example.com");
        claims.put("name", "Test User");
        claims.put("oid", "object-id-123");
        claims.put("tid", "tenant-id-123");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        UserProfile userProfile = azureGroupResolver.getUserProfile(jwt);

        // Assert
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getDisplayName()).isEqualTo("Test User");
        assertThat(userProfile.getEmail()).isEqualTo("test.user@example.com");
        assertThat(userProfile.getId()).isEqualTo("user123");
        assertThat(userProfile.getObjectId()).isEqualTo("object-id-123");
        assertThat(userProfile.getTenantId()).isEqualTo("tenant-id-123");
        
        // Verify roles extraction
        List<String> roles = userProfile.getRoles();
        assertThat(roles).hasSize(2);
        assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("Should get user profile from JWT without roles or groups claim")
    void shouldGetUserProfileFromJwtWithoutRolesOrGroupsClaim() {
        // Arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://login.microsoftonline.com/tenant-id/v2.0");
        // No roles or groups claim
        claims.put("preferred_username", "test.user@example.com");
        claims.put("name", "Test User");
        claims.put("oid", "object-id-123");
        claims.put("tid", "tenant-id-123");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        UserProfile userProfile = azureGroupResolver.getUserProfile(jwt);

        // Assert
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getDisplayName()).isEqualTo("Test User");
        assertThat(userProfile.getEmail()).isEqualTo("test.user@example.com");
        assertThat(userProfile.getId()).isEqualTo("user123");
        assertThat(userProfile.getObjectId()).isEqualTo("object-id-123");
        assertThat(userProfile.getTenantId()).isEqualTo("tenant-id-123");
        
        // Verify roles extraction
        List<String> roles = userProfile.getRoles();
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("Should handle JWT with minimal claims")
    void shouldHandleJwtWithMinimalClaims() {
        // Arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        // Minimal claims

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        // Act
        UserProfile userProfile = azureGroupResolver.getUserProfile(jwt);

        // Assert
        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getId()).isEqualTo("user123");
        // Other fields should be null or empty
        assertThat(userProfile.getRoles()).isEmpty();
    }
}