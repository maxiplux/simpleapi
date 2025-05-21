package app.quantun.simpleapi.config.restclient.client;

import app.quantun.simpleapi.model.contract.request.AuthRequest;
import app.quantun.simpleapi.model.contract.response.AuthResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api")
public interface AuthClient {
    @PostExchange
    AuthResponse login(@RequestBody AuthRequest request);
}
