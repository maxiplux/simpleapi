package app.quantun.simpleapi;

import org.springframework.boot.SpringApplication;

public class TestSimpleApplication {

    public static void main(String[] args) {
        SpringApplication.from(SimpleapiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
