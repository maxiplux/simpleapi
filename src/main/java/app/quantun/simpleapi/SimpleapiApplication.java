package app.quantun.simpleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
//@EnableResilience4J
@EnableRetry
public class SimpleapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleapiApplication.class, args);
    }

}
