package app.quantun.simpleapi.config;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms

@Slf4j
public class JmsConfig {
//    @Bean
//    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
//            @Qualifier("pooledJmsConnectionFactory") ConnectionFactory connectionFactory) {
//
//        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//        factory.setConnectionFactory(connectionFactory);
//        factory.setErrorHandler(t -> {
//            log.error("Error in JMS listener: {}", t.getMessage());
//            t.printStackTrace();
//        });
//
//        return factory;
//    }


    @Value("${ibm.mq.queue.name.request}")
    private String queueRequest;

    @Value("${ibm.mq.queue.name.request}")
    private String queueResponse;

    @Bean
    @Primary
    public JmsTemplate jmsTemplateRequest(@Qualifier("pooledJmsConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName(queueRequest);
        template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        return template;
    }

    @Bean
    public JmsTemplate jmsTemplateResponse(@Qualifier("pooledJmsConnectionFactory") ConnectionFactory connectionFactory) {

        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName(queueResponse);
        template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        return template;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(@Qualifier("pooledJmsConnectionFactory") ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // Critical: Set CLIENT_ACKNOWLEDGE mode
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        // Disable auto-startup for testing if needed
        // factory.setAutoStartup(false);

        // Configure concurrency (set to 1 for testing to avoid race conditions)
        factory.setConcurrency("1-1");

        // Ensure transacted sessions are disabled (conflicts with CLIENT_ACKNOWLEDGE)
        factory.setSessionTransacted(false);

        // CRITICAL: Prevent automatic acknowledgment on exceptions
        factory.setErrorHandler(throwable -> {
            log.error("Error in JMS listener (message will NOT be acknowledged): {}", throwable.getMessage(), throwable);
            // Don't acknowledge - let message remain on queue
            // Do NOT rethrow the exception as it might trigger auto-acknowledgment
        });

        // Configure recovery settings
        factory.setRecoveryInterval(5000L); // 5 seconds

        // IMPORTANT: Configure the container to not auto-acknowledge
        // This prevents Spring from auto-acknowledging even on successful completion




        log.info("JmsListenerContainerFactory configured with CLIENT_ACKNOWLEDGE mode");
        return factory;
    }


}