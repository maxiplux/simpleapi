package app.quantun.simpleapi.config.external.auth;

import app.quantun.simpleapi.model.contract.response.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/oauth2/token")
public interface OAuthClient {


    @PostExchange()
    ResponseEntity<TokenResponse> getToken(@RequestHeader HttpHeaders headers, @RequestBody String body);
}
