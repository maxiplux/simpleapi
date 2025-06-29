package app.quantun.simpleapi.config.metric;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MQQueueHealthIndicator implements HealthIndicator {

    private final MQQueueManager mqQueueManager;

    @Value("${ibm.mq.queue.name.response}")
    private String responseQueueName;

    @Value("${ibm.mq.queue.name.request}")
    private String requestQueueName;


    @Value("${ibm.mq.queue.dlq.name.response}")
    private String responseQueueDLQName;

    @Value("${ibm.mq.queue.dlq.name.request}")
    private String requestQueueDLQName;

    public int getQueueDepth(String queueName) {
        MQQueue queue = null;
        try {
            // Open queue for inquiry only
            int openOptions = CMQC.MQOO_INQUIRE | CMQC.MQOO_FAIL_IF_QUIESCING;
            queue = mqQueueManager.accessQueue(queueName, openOptions);

            // Get current queue depth
            int depth = queue.getCurrentDepth();

            log.debug("Queue '{}' current depth: {}", queueName, depth);
            return depth;

        } catch (MQException mqe) {
            if (mqe.reasonCode == 2035) { // MQRC_NOT_AUTHORIZED
                log.warn("Access denied to queue '{}'. Reason: {}. Check user permissions and channel authentication.",
                        queueName, mqe.getMessage());
                return -1; // Return -1 to indicate auth error
            } else if (mqe.reasonCode == 2085) { // MQRC_UNKNOWN_OBJECT_NAME
                log.warn("Queue '{}' does not exist or is not accessible", queueName);
                return -1;
            } else {
                log.error("Error getting queue depth for {}: MQJE{}: Completion Code '{}', Reason '{}'.",
                        queueName, String.format("%03d", mqe.getClass().getSimpleName().equals("MQException") ? 1 : 0),
                        mqe.completionCode, mqe.reasonCode, mqe);
                return -1;
            }
        } catch (Exception e) {
            log.error("Unexpected error accessing queue '{}': {}", queueName, e.getMessage(), e);
            return -1;
        } finally {
            if (queue != null) {
                try {
                    queue.close();
                } catch (MQException e) {
                    log.warn("Error closing queue '{}': {}", queueName, e.getMessage());
                }
            }
        }
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();

        try {
            // Check connection to queue manager
            if (!mqQueueManager.isConnected()) {
                return Health.down()
                        .withDetail("error", "Queue Manager not connected")
                        .build();
            }


            createHealthEntry(this.requestQueueName, healthBuilder);
            createHealthEntry(this.requestQueueDLQName, healthBuilder);


            createHealthEntry(this.responseQueueName, healthBuilder);
            createHealthEntry(this.responseQueueDLQName, healthBuilder);

            healthBuilder.withDetail("queueManager", mqQueueManager.getName());
            return healthBuilder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    private  void createHealthEntry(String requestQueueNameEntry, Health.Builder healthBuilder) {
        int requestDepth = getQueueDepth(requestQueueNameEntry);
        if (requestDepth >= 0) {
            healthBuilder.withDetail(requestQueueNameEntry, requestDepth);
        } else {
            healthBuilder.withDetail(requestQueueNameEntry, "ACCESS_DENIED_OR_ERROR");
            // Don't fail health check for auth issues, just report the problem
            healthBuilder.withDetail("warning", "Authentication or permission issues detected");
        }
    }


}
