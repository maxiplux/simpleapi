package app.quantun.simpleapi.config.oauth2;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GroupIdToNameConverter {
    private final Map<String, String> groupMappings = new HashMap<>();

    public GroupIdToNameConverter() {
        // Populate with your group ID to name mappings
        groupMappings.put("6e87ccfa-4277-4892-8bb1-61df8bf4d0dd", "ROLE_ADMIN");
    }

    public String convertToGroupName(String groupId) {
        return groupMappings.getOrDefault(groupId, groupId);
    }
}
