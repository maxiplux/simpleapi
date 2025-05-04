package app.quantun.simpleapi.model.contract.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for home message requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}