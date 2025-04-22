package app.quantun.simpleapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing authentication and authorization.
 */
@RestController
@RequestMapping("/api/auth-test")
@Tag(name = "Authentication", description = "Authentication and authorization test endpoints")
public class AuthTestController {

    /**
     * Endpoint that requires authentication but no specific role.
     *
     * @param jwt the JWT token of the authenticated user
     * @return information about the authenticated user
     */
    @GetMapping("/user-info")
    @Operation(summary = "Get authenticated user info", description = "Returns information about the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user information")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("preferred_username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("email", jwt.getClaimAsString("email"));

        // Include token claims for debugging
        Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        // Remove potentially large claims
        claims.remove("aud");
        claims.remove("iss");

        userInfo.put("claims", claims);
        return userInfo;
    }

    /**
     * Endpoint that requires the ADMIN role.
     *
     * @param jwt the JWT token of the authenticated user
     * @return a message indicating the user has admin access
     */
    @GetMapping("/admin-only")
    @Operation(summary = "Admin only endpoint", description = "Endpoint that requires the ADMIN role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User has admin access"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not an admin")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminOnly(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You have ADMIN access");
        response.put("username", jwt.getClaimAsString("preferred_username"));
        return response;
    }

    /**
     * Endpoint that requires either the ADMIN or EDITOR role.
     *
     * @param jwt the JWT token of the authenticated user
     * @return a message indicating the user has editor access
     */
    @GetMapping("/editor-or-admin")
    @Operation(summary = "Editor or admin endpoint", description = "Endpoint that requires either the EDITOR or ADMIN role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User has editor or admin access"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is neither an editor nor an admin")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Map<String, Object> editorOrAdmin(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You have EDITOR or ADMIN access");
        response.put("username", jwt.getClaimAsString("preferred_username"));
        return response;
    }
}