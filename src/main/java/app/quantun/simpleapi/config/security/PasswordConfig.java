package app.quantun.simpleapi.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for password encoding.
 * Provides a PasswordEncoder bean for encoding and verifying passwords.
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates a BCryptPasswordEncoder bean for password encoding.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}