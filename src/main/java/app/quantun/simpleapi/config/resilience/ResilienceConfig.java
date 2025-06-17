package app.quantun.simpleapi.config.resilience;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ResilienceConfig {
    @Bean
    public ScheduledExecutorService resilienceExecutorService() {
        return Executors.newScheduledThreadPool(10);
    }

//    @Bean
//    public CircuitBreaker authCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
//        return circuitBreakerRegistry.circuitBreaker("authService");
//    }
//
//    @Bean
//    public Retry authRetry(RetryRegistry retryRegistry) {
//        return retryRegistry.retry("authService");
//    }
//
//    @Bean
//    public TimeLimiter authTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
//        return timeLimiterRegistry.timeLimiter("authService");
//    }
}

