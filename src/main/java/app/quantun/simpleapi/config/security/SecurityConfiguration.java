package app.quantun.simpleapi.config.security;

import app.quantun.simpleapi.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer.authorizationServer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
class SecurityConfiguration {

    private final ClientRepository clientRepository;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/.well-known/**",
                                "/h2-console/**")
                        .permitAll()
                        .requestMatchers("/api/clients/**", "/api/users/**").authenticated()
                        .anyRequest().authenticated())
                .with(authorizationServer(), Customizer.withDefaults())
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
                .csrf(CsrfConfigurer::disable)
                .sessionManagement( session -> session.sessionCreationPolicy( SessionCreationPolicy.STATELESS))

                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions().disable()) // For H2 console
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    //Token enhancement
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getAuthorizationGrant().getPrincipal() != null) {
                String clientId = context.getAuthorizationGrant().getName();
                clientRepository.findByClientId(clientId).ifPresent(client -> {
                    context.getClaims().claim("roles", client.getRoles());
                });
            }
        };
    }
}
