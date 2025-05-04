package app.quantun.simpleapi.config;

import app.quantun.simpleapi.entity.Client;
import app.quantun.simpleapi.entity.User;
import app.quantun.simpleapi.repository.ClientRepository;
import app.quantun.simpleapi.repository.UserRepository;
import app.quantun.simpleapi.service.ClientService;
import app.quantun.simpleapi.service.UserService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public DataInitializer(UserService userService, ClientService clientService, 
                          UserRepository userRepository, ClientRepository clientRepository) {
        this.userService = userService;
        this.clientService = clientService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @PostConstruct
    public void init() {
        initializeUsers();
        initializeClients();
    }

    private void initializeUsers() {
        // Create admin user if not exists
        if (!userExists("admin")) {
            try {
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword("admin");
                adminUser.setEmail("admin@example.com");
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setRoles(Set.of("ADMIN"));
                userService.createUser(adminUser);
                logger.info("Created admin user");
            } catch (Exception e) {
                logger.warn("Error creating admin user: {}", e.getMessage());
            }
        }

        // Create regular user if not exists
        if (!userExists("user")) {
            try {
                User regularUser = new User();
                regularUser.setUsername("user");
                regularUser.setPassword("user");
                regularUser.setEmail("user@example.com");
                regularUser.setFirstName("Regular");
                regularUser.setLastName("User");
                regularUser.setRoles(Set.of("USER"));
                userService.createUser(regularUser);
                logger.info("Created regular user");
            } catch (Exception e) {
                logger.warn("Error creating regular user: {}", e.getMessage());
            }
        }
    }

    private boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    private void initializeClients() {
        // Create the mcp-client if not exists
        if (!clientExists("mcp-client")) {
            Client client1 = new Client();
            client1.setClientId("mcp-client");
            client1.setClientSecret("{noop}secret");
            client1.setClientAuthenticationMethods(Set.of("client_secret_basic"));
            client1.setAuthorizationGrantTypes(Set.of("client_credentials", "refresh_token", "authorization_code", "password"));
            client1.setScopes(Set.of("read", "write"));
            client1.setRoles(Set.of("USER", "ADMIN"));
            client1.setRedirectUris(Set.of("http://localhost:8080/authorized", "http://127.0.0.1:8080/authorized"));
            clientService.createClient(client1);
            logger.info("Created mcp-client");
        }

        // Create customer1 client if not exists
        if (!clientExists("customer1")) {
            Client client2 = new Client();
            client2.setClientId("customer1");
            client2.setClientSecret("{noop}customer1");
            client2.setClientAuthenticationMethods(Set.of("client_secret_basic"));
            client2.setAuthorizationGrantTypes(Set.of("client_credentials", "refresh_token", "authorization_code", "password"));
            client2.setScopes(Set.of("read", "write"));
            client2.setRedirectUris(Set.of("http://localhost:8080/customer1/callback", "http://127.0.0.1:8080/customer1/callback"));
            client2.setRoles(Set.of("USER", "ADMIN"));
            clientService.createClient(client2);
            logger.info("Created customer1 client");
        }

        // Create customer2 client if not exists
        if (!clientExists("customer2")) {
            Client client3 = new Client();
            client3.setClientId("customer2");
            client3.setClientSecret("{noop}customer2");
            client3.setClientAuthenticationMethods(Set.of("client_secret_basic"));
            client3.setAuthorizationGrantTypes(Set.of("client_credentials", "refresh_token", "authorization_code", "password"));
            client3.setScopes(Set.of("read", "write"));
            client3.setRedirectUris(Set.of("http://localhost:8080/customer2/callback", "http://127.0.0.1:8080/customer2/callback"));
            client3.setRoles(Set.of("USER", "ADMIN"));
            clientService.createClient(client3);
            logger.info("Created customer2 client");
        }
    }

    private boolean clientExists(String clientId) {
        return clientRepository.findByClientId(clientId).isPresent();
    }
}
