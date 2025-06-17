package app.quantun.simpleapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnableAspectJAutoProxy
@EnableAsync
class SimpleapiApplicationTests {

    @Test
    void contextLoads() {
    }

}
