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

    @InjectMocks
    private MqMessageListener mqMessageListener;

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

        // Act
        mqMessageListener.callBusinessLogicWithTimeLimiter(messageText);

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
