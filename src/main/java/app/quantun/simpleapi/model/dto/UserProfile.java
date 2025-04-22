package app.quantun.simpleapi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private String id;
    private String displayName;
    private String email;
    private String objectId;
    private String tenantId;
    private List<String> roles;
}