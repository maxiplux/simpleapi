package app.quantun.simpleapi.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsUtils;
import org.springframework.util.ErrorHandler;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

@Slf4j
public class CustomJmsErrorHandler implements ErrorHandler {


    private static final int MAX_REDELIVERY_ATTEMPTS = 5;

    private final JmsTemplate jmsTemplate;
    private final String deadLetterQueueName;

    public CustomJmsErrorHandler(JmsTemplate jmsTemplate, String deadLetterQueueName) {
        this.jmsTemplate = jmsTemplate;
        this.deadLetterQueueName = deadLetterQueueName;
    }

    @Override
    public void handleError(Throwable t) {
        log.warn("In custom error handler {}", t.getMessage());
        if (t.getCause() instanceof JMSException) {
            JMSException jmsException = (JMSException) t.getCause();
            try {
                Message failedMessage = (Message) jmsException.getLinkedException();
                if (failedMessage != null) {
                    int deliveryCount = failedMessage.getIntProperty("JMSXDeliveryCount");

                    if (deliveryCount > MAX_REDELIVERY_ATTEMPTS) {
                        log.error("Message has been redelivered {} times. Sending to DLQ.", deliveryCount);
                        jmsTemplate.send(deadLetterQueueName, session -> failedMessage);
                        // Message is acknowledged here implicitly by not throwing an exception further
                    } else {
                        log.warn("Redelivery attempt {}. Throwing exception to retry.", deliveryCount);
                        // Re-throw to trigger redelivery
                        throw new RuntimeException(t);
                    }
                }
            } catch (JMSException e) {
                log.error("Error while handling poison message.{}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
        else
        {
            //TODO: Handle other types of exceptions if needed
            //This is in case of error in the DLQ processing or any other error
            throw new RuntimeException(t);
        }
    }
}