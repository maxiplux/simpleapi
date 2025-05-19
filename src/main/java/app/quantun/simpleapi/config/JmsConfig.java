package app.quantun.simpleapi.config;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
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
        template.convertAndSend(this.queueRequest, "message");
        return template;
    }

    @Bean
    public JmsTemplate jmsTemplateResponse(@Qualifier("pooledJmsConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName(queueResponse);
        template.convertAndSend(this.queueResponse, "message");
        return template;
    }

}