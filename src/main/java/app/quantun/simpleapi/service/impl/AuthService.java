package app.quantun.simpleapi.service.impl;

import app.quantun.simpleapi.config.restclient.AuthClient;
import app.quantun.simpleapi.model.contract.request.AuthRequest;
import app.quantun.simpleapi.model.contract.response.AuthResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthClient authClient;
    private final CircuitBreaker authCircuitBreaker;
    private final Retry authRetry;

    public AuthResponse login(String username, String password, Integer expiresInMins) {
        log.info("Attempting to authenticate user: {}", username);

        AuthRequest request = AuthRequest.builder()
                .username(username)
                .password(password)
                .expiresInMins(expiresInMins)
                .build();

        // Apply circuit breaker and retry patterns
        return authCircuitBreaker.executeSupplier(() ->
                authRetry.executeSupplier(() ->
                        authClient.login(request)
                )
        );
    }
}
