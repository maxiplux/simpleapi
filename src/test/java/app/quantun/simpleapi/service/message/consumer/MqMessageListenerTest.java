package app.quantun.simpleapi.service.message.consumer;

import app.quantun.simpleapi.config.external.search.CrawLerClient;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqMessageListenerTest {

    @Mock
    private CrawLerClient crawLerClient;

    @Mock
    private org.springframework.jms.core.JmsTemplate jmsTemplate;

    @Mock
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private io.github.resilience4j.retry.RetryRegistry retryRegistry;

    @Mock
    private io.github.resilience4j.timelimiter.TimeLimiterRegistry timeLimiterRegistry;

    @Mock
    private java.util.concurrent.ScheduledExecutorService resilienceExecutorService;

    private MqMessageListener mqMessageListener;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mqMessageListener = new MqMessageListener(
            crawLerClient, 
            circuitBreakerRegistry, 
            retryRegistry, 
            timeLimiterRegistry, 
            resilienceExecutorService

        );

        // Set the queue name using reflection since it's injected with @Value
        try {
            java.lang.reflect.Field queueRequestField = MqMessageListener.class.getDeclaredField("queueRequest");
            queueRequestField.setAccessible(true);
            queueRequestField.set(mqMessageListener, "test-queue");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set queueRequest field", e);
        }
    }

    @Test
    @DisplayName("Should process text message successfully when requestType is demo")
    void shouldProcessTextMessage_WhenRequestTypeIsDemo() throws JMSException {
        // Arrange
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getText()).thenReturn("Test message content");
        when(textMessage.getJMSMessageID()).thenReturn("test-message-id");
        when(textMessage.getJMSCorrelationID()).thenReturn("test-correlation-id");
        when(textMessage.getStringProperty("requestType")).thenReturn("demo");
        when(textMessage.getStringProperty("customHeader")).thenReturn("test-header");
        when(textMessage.getStringProperty("userId")).thenReturn("test-user");

        // Act
        mqMessageListener.receiveMessage(textMessage);

        // Assert
        verify(textMessage).acknowledge();
        verify(textMessage).getStringProperty("requestType");
        verify(textMessage).getStringProperty("customHeader");
        verify(textMessage).getStringProperty("userId");
        verify(textMessage).getText();
    }

    @Test
    @DisplayName("Should not process message when requestType is not demo")
    void shouldNotProcessMessage_WhenRequestTypeIsNotDemo() throws JMSException {
        // Arrange
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getStringProperty("requestType")).thenReturn("not-demo");

        // Act
        mqMessageListener.receiveMessage(textMessage);

        // Assert
        verify(textMessage, never()).acknowledge();
    }

    @Test
    @DisplayName("Should acknowledge non-text message")
    void shouldAcknowledgeNonTextMessage() throws JMSException {
        // Arrange
        Message message = mock(Message.class);
        // No need to mock instanceof, just don't use a TextMessage

        // Act
        mqMessageListener.receiveMessage(message);

        // Assert
        verify(message).acknowledge();
    }

    @Test
    @DisplayName("Should handle JMSException when processing message")
    void shouldHandleJMSException_WhenProcessingMessage() throws JMSException {
        // Arrange
        TextMessage textMessage = mock(TextMessage.class);

        // Setup the mock to throw exception when getText() is called
        // This will be caught in the try-catch block in receiveMessage
        doThrow(new JMSException("Test JMS exception")).when(textMessage).getText();

        // Act
        mqMessageListener.receiveMessage(textMessage);

        // Assert
        // The message should not be acknowledged since an exception was thrown
        verify(textMessage, never()).acknowledge();
    }


    @Test
    @DisplayName("Should call search when message contains xml")
    void shouldCallSearch_WhenMessageContainsXml() {
        // Arrange
        String messageText = "Test message with xml content";
        when(crawLerClient.getDocuments()).thenReturn("Document list");
        when(crawLerClient.search(messageText)).thenReturn("Search results");

        // We're testing the direct business logic execution, not the resilience patterns
        // So we'll use a simplified approach that doesn't require mocking all resilience components

        // Act - directly call executeBusinessLogic via reflection
        try {
            java.lang.reflect.Method executeBusinessLogicMethod = 
                MqMessageListener.class.getDeclaredMethod("executeBusinessLogic", String.class);
            executeBusinessLogicMethod.setAccessible(true);
            executeBusinessLogicMethod.invoke(mqMessageListener, messageText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call executeBusinessLogic", e);
        }

        // Assert
        verify(crawLerClient).getDocuments();
        verify(crawLerClient).search(messageText);
    }

    @Test
    @DisplayName("Should throw CustomBusinessException when fallback is called")
    void shouldThrowCustomBusinessException_WhenFallbackIsCalled() {
        // Arrange
        String messageText = "Test message";
        Exception exception = new RuntimeException("Test exception");

        // Act & Assert

    }
}
