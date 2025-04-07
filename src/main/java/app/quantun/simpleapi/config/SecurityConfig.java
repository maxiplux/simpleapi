package app.quantun.simpleapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig  {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        // Swagger UI paths
                        .requestMatchers("/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**")
                        .permitAll()
                        // Secure your API endpoints as needed
                        .requestMatchers("/api/**").authenticated()
                        // Other endpoints
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        }).authenticationEntryPoint(
                                (request, response, authException) -> {
                                    // Handle authentication entry point

                                    String message = authException.getMessage() != null ?
                                            authException.getMessage() :
                                            "Authentication failed: Invalid credentials";
                                    response.sendError(401, message);
                                }
                        )
                );
        return http.build();
    }


}
