package app.quantun.simpleapi.config.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class AzureGroupsJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final GroupIdToNameConverter groupConverter;
    private  final AzureGroupResolver azureGroupResolver;

    public AzureGroupsJwtAuthenticationConverter(GroupIdToNameConverter groupConverter,AzureGroupResolver azureGroupResolver) {
        this.groupConverter = groupConverter;
        this.azureGroupResolver = azureGroupResolver;
    }


    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<String> groups = jwt.getClaim("groups");
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        if (groups != null) {
            for (String group : groups) {
                // IF YOU DON'T HAVE A PROXY IN YOUR ENTERPRISE, YOU NEED TO IMPLEMENT A TRANSLATOR
                //groupConverter.convertToGroupName(group);
                azureGroupResolver.enrichUserProfileWithGraphData(jwt);
                String groupName = azureGroupResolver.resolveGroupName(group);
                authorities.add(new SimpleGrantedAuthority(groupName));
            }

        }

        return authorities;
    }

}