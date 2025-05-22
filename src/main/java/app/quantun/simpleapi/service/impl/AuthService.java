package app.quantun.simpleapi.service.impl;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.model.contract.request.AuthRequest;
import app.quantun.simpleapi.model.contract.response.AuthResponse;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    //private final AuthClient authClient;

    private final CircuitBreaker authCircuitBreaker;
    private final Retry authRetry;

    private final OAuthClient oAuthClient;
    @Value("${app.server.external.oauth.grant-type}")
    private String getGrantType;
    @Value("#{${app.server.external.oauth.headers}}")
    private Map<String, String> headersMap;

    public TokenResponse getAccessToken() {
        // Create full URL


        // Create headers from SpEL evaluated map
        HttpHeaders headers = new HttpHeaders();
        headersMap.forEach(headers::add);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create body
        String body = "grant_type=" + this.getGrantType;

        // Make the request
        ResponseEntity<TokenResponse> response = oAuthClient.getToken(headers, body);
        return response.getBody();
    }


    public AuthResponse login(String username, String password, Integer expiresInMins) {
        log.info("Attempting to authenticate user: {}", username);

        AuthRequest request = AuthRequest.builder()
                .username(username)
                .password(password)
                .expiresInMins(expiresInMins)
                .build();
        return null;
        // Apply circuit breaker and retry patterns
//        return authCircuitBreaker.executeSupplier(() ->
//                authRetry.executeSupplier(() ->
//                        authClient.login(request)
//                )
//        );
    }
}
