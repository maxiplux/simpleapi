package app.quantun.simpleapi.config.external.search;

import app.quantun.simpleapi.config.interceptor.AuthenticationInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Slf4j

public class CrawlerClientConfig {

    @Value("${app.server.external.oauth.base-url}")
    private String oAuthBaseUrl;


    @Bean
    public CrawLerClient myApiClient(AuthenticationInterceptor authInterceptor) {
        RestClient restClient = RestClient.builder()
                .requestInterceptor(authInterceptor)
                .baseUrl(this.oAuthBaseUrl)
//                .defaultHeader("User-Agent", "MyApp/1.0")
                //               .defaultHeader("Accept", "application/json")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(CrawLerClient.class);
    }

}
