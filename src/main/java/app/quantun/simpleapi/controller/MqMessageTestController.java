package app.quantun.simpleapi.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MqMessageTestController {


    private JmsTemplate jmsTemplate;



    @Value("${ibm.mq.queue}")
    private String queue;

    @GetMapping("/")
    public String home() {
        return "Simple API with IBM MQ integration is running!";
    }

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        jmsTemplate.convertAndSend(queue, message);
        return "Message sent to queue: " + queue;
    }
}
