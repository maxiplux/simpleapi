package app.quantun.simpleapi.config.external.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Slf4j
public class OAuthClientConfig {

    @Value("${app.server.external.oauth.base-url}")
    private String oAuthBaseUrl;


    @Bean
    public OAuthClient oAuthClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(this.oAuthBaseUrl)
//                .defaultStatusHandler(
//                        HttpStatusCode::isError,
//                        (request, response) ->
//                        {
//                            String requestId = UUID.randomUUID().toString();
//                            String errorBody = "";
//                            try {
//                                errorBody = new String(response.getBody().readAllBytes());
//                                errorBody = Helper.sanitizeErrorBody(errorBody);
//                                log.error("Auth service error [ID: {}]: Status={}",
//                                        requestId, response.getStatusCode());
//                                // Log sanitized body at debug level only
//                                log.debug("Auth service error details [ID: {}]: {}",
//                                        requestId, errorBody);
//                            } catch (Exception e) {
//                                log.error("Failed to process auth service error [ID: {}]", requestId, e);
//                            }
//                            throw new CustomAuthException(errorBody, response.getStatusCode());
//                        }
//                )
                //.defaultHeader("User-Agent", "MyApp/1.0")
                //.defaultHeader("Accept", "application/json")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(OAuthClient.class);
    }


}
