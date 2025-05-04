package app.quantun.simpleapi.repository;

import app.quantun.simpleapi.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Client entity.
 * Provides methods to perform CRUD operations on the oauth2_registered_client table.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Find a client by its client ID.
     *
     * @param clientId the client ID
     * @return an Optional containing the client if found, or empty if not found
     */
    Optional<Client> findByClientId(String clientId);

    /**
     * Check if a client with the given client ID exists.
     *
     * @param clientId the client ID
     * @return true if a client with the given ID exists, false otherwise
     */
    boolean existsByClientId(String clientId);
}