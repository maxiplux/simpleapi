package app.quantun.simpleapi.service.message.consumer;

import app.quantun.simpleapi.config.external.search.CrawLerClient;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqMessageListener {


    private final CrawLerClient crawLerClient;
    /**
     * Listens for messages on the configured queue and manually acknowledges them.
     *
     * @param message The JMS message received from the queue
     */
    @JmsListener(destination = "${ibm.mq.queue.name.request}", containerFactory = "jmsListenerContainerFactory")
    public void receiveMessage(Message message) {
        try {
            if (message instanceof TextMessage) {

                String text = ((TextMessage) message).getText();
                log.info("Received message: {}", text);

                // Process the message here
                boolean processingSuccessful = processMessage(text);

                if (processingSuccessful) {
                    // Manually acknowledge the message
                    message.acknowledge();
                    log.info("Message acknowledged: {}", "❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F❤\uFE0F");
                } else {
                    // Don't acknowledge - message will be redelivered
                    log.warn("Message processing failed, not acknowledging: {}", "❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌");
                }
            } else {
                log.debug("Received non-text message: {}", message);
                // Decide whether to acknowledge non-text messages based on your requirements
                message.acknowledge();
            }
        } catch (JMSException e) {
            log.error("Error processing message", e);
            // In case of exception, the message will not be acknowledged
            // and will be redelivered based on the IBM MQ redelivery policy
        }
    }

    /**
     * Process the received message.
     *
     * @param messageText The text content of the received message
     * @return true if processing was successful, false otherwise
     */
    private boolean processMessage(String messageText) {
        try {

            log.info("Start getDocuments ❤\uFE0F❤");
            this.crawLerClient.getDocuments();
            log.info("End getDocuments ❤\uFE0F❤ OK  ❤\uFE0F❤ ");


            if  (messageText != null && messageText.contains("xml"))
            {
                log.info("Start search ⛑\uFE0F");
                this.crawLerClient.search(messageText);
                log.info("end search ⛑\uFE0F OK  ⛑\uFE0F");
            }



            // Implement your message processing logic here
            // Return true if processing succeeds, false if it fails
            return true;
        } catch (Exception e) {
            log.error("Failed to process message: ❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌{} ❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌", e.getMessage());
            return true;
        }
    }

}