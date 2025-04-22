package app.quantun.simpleapi.config.oauth2;

import app.quantun.simpleapi.model.dto.UserProfile;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.core.authentication.AzureIdentityAuthenticationProvider;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AzureGroupResolver {

    private final GraphServiceClient graphClient;
    private final Map<String, String> groupCache = new ConcurrentHashMap<>();

    public AzureGroupResolver(
            @Value("${azure.tenant-id}") String tenantId,
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            @Value("${microsoft.graph.scope}") String graphScope
            ) {

        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        var scopes = Arrays.asList("https://graph.microsoft.com/.default");
        this.graphClient = new GraphServiceClient(credential, scopes.toArray(new String[0]));
    }

    @Cacheable(value = "groupNames", key = "#groupId")
    public String resolveGroupName(String groupId) {
        // Check cache first
        if (groupCache.containsKey(groupId)) {
            return groupCache.get(groupId);
        }

        try {
            // Fetch group name from Microsoft Graph API
            Group group = graphClient.groups().byGroupId(groupId).get();
            String groupName = "ROLE_" + group.getDisplayName().toUpperCase().replace(" ", "_");

            // Cache the result
            groupCache.put(groupId, groupName);
            return groupName;
        } catch (Exception e) {
            // Fallback to a default name if API call fails
            return "GROUP_" + groupId;
        }
    }


    public UserProfile enrichUserProfileWithGraphData(Jwt jwt) {

        String objectId = jwt.getClaimAsString("oid");

        // Basic profile from JWT

        UserProfile basicProfile = getUserProfile(jwt);

        // If we have an object ID, enhance with Graph API data
        if (objectId != null) {
            try {
                User user = graphClient.users().byUserId(objectId).get();

                // Enhance the profile with additional data
                basicProfile.setDisplayName(user.getDisplayName());
                basicProfile.setEmail(user.getMail() != null ? user.getMail() : user.getUserPrincipalName());
                // Add more fields as needed
            } catch (Exception e)
            {
                // Log the error but continue with basic profile
                log.error( "Error fetching user data from Graph API: {}", e.getMessage());
            }
        }

        return basicProfile;
    }

    public UserProfile getUserProfile(Jwt jwt) {


        // Extract claims
        String subject = jwt.getSubject();
        String name = jwt.getClaimAsString("name");
        String email = jwt.getClaimAsString("preferred_username");
        String objectId = jwt.getClaimAsString("oid");
        String tenantId = jwt.getClaimAsString("tid");

        // Extract groups/roles
        List<String> roles = extractRoles(jwt);

        // Build the profile
        return UserProfile.builder()
                .id(subject)
                .displayName(name)
                .email(email)
                .objectId(objectId)
                .tenantId(tenantId)
                .roles(roles)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        // Try to get roles first
        List<String> roles = Optional.ofNullable(jwt.getClaim("roles"))
                .filter(obj -> obj instanceof List)
                .map(obj -> (List<String>) obj)
                .orElse(null);

        // If no roles, try to get groups
        if (roles == null) {
            roles = Optional.ofNullable(jwt.getClaim("groups"))
                    .filter(obj -> obj instanceof List)
                    .map(obj -> (List<String>) obj)
                    .orElse(Collections.emptyList());
        }

        return roles;
    }


}
