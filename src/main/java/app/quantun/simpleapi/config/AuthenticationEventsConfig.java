package app.quantun.simpleapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

@Configuration
public class AuthenticationEventsConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEventsConfig.class);
    
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher() {
        DefaultAuthenticationEventPublisher publisher = new DefaultAuthenticationEventPublisher();
        
        // Add custom handling for OAuth2 authentication exceptions
        publisher.setAdditionalExceptionMappings(
                Map.of(OAuth2AuthenticationException.class,
                       OAuth2AuthenticationFailureEvent.class));
        
        return new AuthenticationEventPublisher() {
            @Override
            public void publishAuthenticationSuccess(Authentication authentication) {
                if (authentication instanceof OAuth2ClientAuthenticationToken) {
                    logger.info("OAuth2 client authentication success: {}", 
                              ((OAuth2ClientAuthenticationToken) authentication).getRegisteredClient().getClientId());
                }
                publisher.publishAuthenticationSuccess(authentication);
            }
            
            @Override
            public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
                if (exception instanceof OAuth2AuthenticationException) {
                    OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
                    logger.error("OAuth2 authentication failure: {} - Error code: {}", 
                               oauth2Exception.getMessage(), 
                               oauth2Exception.getError().getErrorCode());
                    
                    if (authentication instanceof OAuth2ClientAuthenticationToken) {
                        logger.error("Failed client ID: {}", 
                                  ((OAuth2ClientAuthenticationToken) authentication).getPrincipal());
                    }
                }
                publisher.publishAuthenticationFailure(exception, authentication);
            }
        };
    }
    
    // Custom event class for OAuth2 authentication failures
    public static class OAuth2AuthenticationFailureEvent extends AbstractAuthenticationFailureEvent {
        public OAuth2AuthenticationFailureEvent(Authentication authentication, AuthenticationException exception) {
            super(authentication, exception);
        }
    }
}