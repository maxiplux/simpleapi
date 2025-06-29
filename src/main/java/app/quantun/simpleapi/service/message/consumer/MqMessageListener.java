package app.quantun.simpleapi.service.message.consumer;

import app.quantun.simpleapi.config.external.search.CrawLerClient;
import app.quantun.simpleapi.exception.JmsExceptionInvalidProcessMessage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Service responsible for listening to MQ messages and processing them with resilience patterns.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MqMessageListener {
    private final CrawLerClient crawLerClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ScheduledExecutorService resilienceExecutorService;


    /**
     * Listens for messages on the configured queue and manually acknowledges them.
     * Only processes messages with requestType "demo".
     *
     * @param message The JMS message received from the queue
     */
    @JmsListener(destination = "${ibm.mq.queue.name.request}", containerFactory = "jmsListenerContainerFactory")
    @Retryable(value = {JmsExceptionInvalidProcessMessage.class}, maxAttempts = 3, backoff = @Backoff(delay = 10000))
    public void receiveMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                String requestType = message.getStringProperty("requestType");

                if (requestType != null && requestType.equals("demo")) {
                    // Extract message metadata
                    String messageId = message.getJMSMessageID();
                    String correlationId = message.getJMSCorrelationID();
                    String replyTo = message.getJMSReplyTo() != null ? message.getJMSReplyTo().toString() : null;
                    int priority = message.getJMSPriority();
                    String customHeader = message.getStringProperty("customHeader");
                    String userId = message.getStringProperty("userId");

                    // Log message receipt with relevant metadata
                    log.info("ℹ\uFE0Fℹ\uFE0F Received message | MessageID: {} | CorrelationID: {} | CustomHeader: {} | UserID: {} | RequestType: {} | Priority: {} | Text: {} | ReplyTo: {} ℹ\uFE0F",
                            messageId, correlationId, customHeader, userId, requestType, priority);

                    // Process the message with header context
                    boolean processingSuccessful = processMessage(text);

                    if (processingSuccessful) {
                        message.acknowledge();
                        log.info("Message successfully processed and acknowledged: ✅✅✅✅✅✅✅✅✅✅{}✅✅✅✅✅✅✅✅✅✅✅", messageId);
                    } else {
                        log.warn("Message processing failed, not acknowledging: ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ {}❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ", messageId);
                        throw new JmsExceptionInvalidProcessMessage("Message processing failed ");
                    }
                } else {
                    log.debug("Ignoring message with requestType: {}", requestType);
                }
            } else {
                log.error("Received a non-text message: {}", message);
                message.acknowledge();
            }
        } catch (JMSException e) {
            log.error("Error processing message", e);
        }
    }

    /**
     * Process the received message by executing business logic with resilience patterns.
     *
     * @param messageText The text content of the received message
     * @return true if processing was successful, false otherwise
     */
    private boolean processMessage(String messageText) {
        try {
            log.debug("Processing message text: {}", messageText);
            callBusinessLogicWithTimeLimiter(messageText).get();
            return true;
        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Executes business logic with resilience patterns (circuit breaker, retry, time limiter).
     *
     * @param messageText The text content of the message to process
     * @return CompletableFuture containing the result of the business logic execution
     */
    public CompletableFuture<Boolean> callBusinessLogicWithTimeLimiter(String messageText) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("businessService");
        Retry retry = retryRegistry.retry("businessService");
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("businessService");

        // Create the business logic supplier with resilience patterns
        Supplier<Boolean> businessLogicSupplier = () -> 
            retry.executeSupplier(() ->
                circuitBreaker.executeSupplier(() -> 
                    executeBusinessLogic(messageText)
                )
            );

        // Execute with time limiter
        CompletableFuture<Boolean> future = CompletableFuture
                .supplyAsync(businessLogicSupplier, resilienceExecutorService);

        return timeLimiter.executeCompletionStage(resilienceExecutorService, () -> future)
                .toCompletableFuture()
                .handle((result, throwable) -> 
                    handleResilienceResult(messageText, result, throwable)
                );
    }

    /**
     * Executes the core business logic by calling external services.
     * Retrieves documents and performs search if the message contains "xml".
     *
     * @param messageText The text content of the message to process
     * @return true if the business logic executed successfully
     */
    private Boolean executeBusinessLogic(String messageText) {
        log.info("Starting document retrieval operation");
        crawLerClient.getDocuments();
        log.info("Document retrieval completed successfully");

        if (messageText != null && messageText.contains("xml")) {
            log.info("Starting search operation for XML content");
            crawLerClient.search(messageText);
            log.info("Search operation completed successfully");
        }
        return true;
    }

    /**
     * Handles the result of resilience-wrapped business logic execution.
     * Provides appropriate logging and fallback behavior for different failure scenarios.
     *
     * @param messageText The original message text
     * @param result The result of the business logic execution (if successful)
     * @param throwable The exception that occurred (if any)
     * @return true if successful, false if an error occurred
     */
    private Boolean handleResilienceResult(String messageText, Boolean result, Throwable throwable) {
        if (throwable == null) {
            log.info("Business logic completed successfully for message");
            return result;
        }

        // Log specific error types with appropriate context
        if (throwable instanceof TimeoutException) {
            log.error("Timeout occurred while processing message '{}': {}", messageText, throwable.getMessage());
        } else if (throwable instanceof RejectedExecutionException) {
            log.error("Execution rejected for message '{}': {}", messageText, throwable.getMessage());
        } else {
            log.error("An error occurred while processing message '{}': {}", messageText, throwable.getMessage());
        }

        log.warn("Fallback triggered for message '{}'", messageText);
        return false; // Indicates failure
    }
}
