package app.quantun.simpleapi.service.message.consumer;

import app.quantun.simpleapi.config.external.search.CrawLerClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.exception.CustomBusinessException;
import app.quantun.simpleapi.model.contract.request.AuthRequest;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.util.Enumeration;

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
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                String requestType = message.getStringProperty("requestType");

                if (requestType !=null && requestType.equals("demo"))
                {
                    log.info("This is message {}", textMessage.getJMSMessageID());

                    // Access JMS standard headers
                    String messageId = message.getJMSMessageID();
                    String correlationId = message.getJMSCorrelationID();
                    String replyTo = message.getJMSReplyTo() != null ? message.getJMSReplyTo().toString() : null;
                    int priority = message.getJMSPriority();


                    // Access custom headers/properties
                    String customHeader = message.getStringProperty("customHeader");
                    String userId = message.getStringProperty("userId");

//                    Enumeration<String> propertyNames = message.getPropertyNames();
//                    while (propertyNames.hasMoreElements()) {
//                        String name = propertyNames.nextElement();
//                        Object value = message.getObjectProperty(name);
//                        log.info("Property {} = {}", name, value);
//                    }


                    // Log headers for debugging
                    log.info("️❤️❤️❤️❤️❤️ Received  | MessageID: {} | CorrelationID: {} | CustomHeader: {}, userid:{}  ️requestType :{} ❤️❤️❤️❤️❤️",
                            messageId, correlationId, customHeader, userId, requestType);

                    // Process the message with header context
                    boolean processingSuccessful = processMessage(text);

                    if (processingSuccessful) {
                        message.acknowledge();
                        log.info("Message acknowledged: {}", "❤️❤️❤️❤️❤️❤️❤️❤️❤️");
                    } else {
                        log.warn("Message processing failed, not acknowledging: {}", "❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌");
                    }

                }

            }
            else {
                log.error("Received a non-text-bussines message: {}", message);
                message.acknowledge();
            }
        } catch (JMSException e) {
            log.error("Error processing message", e);

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




            // Implement your message processing logic here
            // Return true if processing succeeds, false if it fails
            return true;
        } catch (Exception e) {
            log.error("Failed to process message: ❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌{} ❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌", e.getMessage());
            return false;
        }
    }


    @CircuitBreaker(name = "businessService", fallbackMethod = "callBusinessLogicFallback")
    @Retry(name = "businessService")
    @TimeLimiter(name = "businessService")
    public void callBusinessLogic(String messageText) {
        log.info("Start getDocuments \uD83D\uDFE5");
        this.crawLerClient.getDocuments();
        log.info("End getDocuments \uD83D\uDC9A OK  \uD83D\uDFE9\uD83D\uDFE9 \uD83D\uDC9A ");


        if (messageText != null && messageText.contains("xml")) {
            log.info("Start search ⛑\uFE0F");
            this.crawLerClient.search(messageText);
            log.info("end search \uD83D\uDC9A OK \uD83D\uDFE9\uD83D\uDFE9  \uD83D\uDC9A");
        }
    }
    public void  callBusinessLogicFallback(String messageText, Exception e) {
        log.error("MqMessageListener:callBusinessLogicFallback {} ", e.getMessage());
        throw new CustomBusinessException(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

}