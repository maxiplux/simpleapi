package app.quantun.simpleapi.config;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Hashtable;

@Configuration
@Slf4j
public class MQConfig {

    @Value("${ibm.mq.queueManager}")
    private String queueManagerName;

    @Value("${ibm.mq.channel}")
    private String channel;

    @Value("${ibm.mq.connName}")
    private String connName;

    @Value("${ibm.mq.user}")
    private String user;

    @Value("${ibm.mq.password}")
    private String password;

    @Value("${ibm.mq.transportType:CLIENT}")
    private String transportType;

    @Value("${ibm.mq.useIBMCipherMappings:false}")
    private boolean useIBMCipherMappings;

    @Bean(destroyMethod = "disconnect")
    public MQQueueManager mqQueueManager() throws Exception {
        log.info("Creating MQQueueManager bean with queue manager: {}", queueManagerName);
        log.debug("MQ Connection details - Host: {}, Port: {}, Channel: {}, Transport: {}", 
                connName.split("\\(")[0], 
                connName.split("\\(|\\)")[1], 
                channel, 
                transportType);

        // Set system property for IBM cipher mappings before creating the connection
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", String.valueOf(useIBMCipherMappings));
        log.debug("Set useIBMCipherMappings to: {}", useIBMCipherMappings);

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(CMQC.CHANNEL_PROPERTY, channel);
        properties.put(CMQC.HOST_NAME_PROPERTY, connName.split("\\(")[0]);
        properties.put(CMQC.PORT_PROPERTY, Integer.parseInt(connName.split("\\(|\\)")[1]));
        properties.put(CMQC.USER_ID_PROPERTY, user);
        properties.put(CMQC.PASSWORD_PROPERTY, password);

        if ("CLIENT".equalsIgnoreCase(transportType)) {
            properties.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
        }

        try {
            MQQueueManager queueManager = new MQQueueManager(queueManagerName, properties);
            log.info("Successfully created MQQueueManager bean");
            
            // Test connection by checking if queue manager is connected
            if (queueManager.isConnected()) {
                log.info("MQ Queue Manager connection verified successfully");
            } else {
                log.warn("MQ Queue Manager created but connection status is unclear");
            }
            
            return queueManager;
        } catch (MQException mqe) {
            String errorMessage = String.format("Failed to create MQQueueManager - MQJE%03d: Completion Code '%d', Reason '%d'", 
                1, mqe.completionCode, mqe.reasonCode);
            
            if (mqe.reasonCode == 2035) {
                log.error("{} - Authentication failed. Please verify username '{}' and password are correct and user has proper permissions", 
                    errorMessage, user);
            } else if (mqe.reasonCode == 2058) {
                log.error("{} - Queue manager '{}' not available or incorrect name", errorMessage, queueManagerName);
            } else if (mqe.reasonCode == 2059) {
                log.error("{} - Connection failed. Please verify host '{}' and port '{}' are correct and MQ server is running", 
                    errorMessage, connName.split("\\(")[0], connName.split("\\(|\\)")[1]);
            } else {
                log.error("{} - {}", errorMessage, mqe.getMessage());
            }
            throw new RuntimeException(errorMessage, mqe);
        } catch (Exception e) {
            log.error("Unexpected error creating MQQueueManager: {}", e.getMessage(), e);
            throw e;
        }
    }
}