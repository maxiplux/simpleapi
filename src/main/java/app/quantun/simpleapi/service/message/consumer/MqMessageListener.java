package app.quantun.simpleapi.service.message.consumer;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
public class MqMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MqMessageListener.class);

    /**
     * Listens for messages on the configured queue.
     *
     * @param message The JMS message received from the queue
     */
    @JmsListener(destination = "${ibm.mq.queue}")
    public void receiveMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                logger.info("Received message: {}", text);
                // Process the message here
            } else {
                logger.info("Received non-text message: {}", message);
            }
        } catch (JMSException e) {
            logger.error("Error processing message", e);
        }
    }
}