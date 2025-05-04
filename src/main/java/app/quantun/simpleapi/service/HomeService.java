package app.quantun.simpleapi.service;

import app.quantun.simpleapi.model.contract.request.HomeRequest;
import app.quantun.simpleapi.model.contract.response.HomeResponse;

/**
 * Service interface for home message operations.
 */
public interface HomeService {

    /**
     * Get the welcome message.
     *
     * @return the welcome message response
     */
    HomeResponse getWelcomeMessage();

    /**
     * Create a new message.
     *
     * @param homeRequest the message request
     * @return the created message response
     */
    HomeResponse createMessage(HomeRequest homeRequest);

    /**
     * Update the message.
     *
     * @param homeRequest the message request
     * @return the updated message response
     */
    HomeResponse updateMessage(HomeRequest homeRequest);
}