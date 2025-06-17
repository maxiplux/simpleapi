package app.quantun.simpleapi.controller;


import app.quantun.simpleapi.service.impl.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MqMessageTestController {

    private final AuthService authService;
    @Qualifier("jmsTemplateRequest")
    private final JmsTemplate jmsTemplate;


    @Value("${ibm.mq.queue.name.request}")
    private String queueRequest;

    @GetMapping("/")
    public String home() {
        return "Simple API with IBM MQ integration is running!";
    }


    @PostMapping(path = "/send", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String sendMessage(@RequestBody String message) {

        try {
            jmsTemplate.setSessionTransacted(true);

            jmsTemplate.convertAndSend(queueRequest, message, messagePostProcessor -> {
                // Add custom headers to the JMS message
                messagePostProcessor.setStringProperty("customHeader", "myValue");
                messagePostProcessor.setStringProperty("userId", "12345");
                messagePostProcessor.setStringProperty("requestType", "demo");
                return messagePostProcessor;
            });


            return "Message sent to queue: " + queueRequest;
        } catch (JmsException e) {
            log.error("JmsException {}", e.getMessage());
            return "Failed to deliver message: " + e.getMessage();
        }
    }
}
