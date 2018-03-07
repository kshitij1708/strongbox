package org.carlspring.strongbox.users.security;

import org.carlspring.strongbox.security.Role;
import org.carlspring.strongbox.users.domain.Roles;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Przemyslaw Fusik
 */
@Component
public class AuthoritiesProvider
{

    private static final Logger logger = LoggerFactory.getLogger(AuthoritiesProvider.class);

    @Inject
    AuthorizationConfigProvider authorizationConfigProvider;

    @Transactional
    public Set<GrantedAuthority> getAuthoritiesByRoleName(final String roleName)
    {
        Set<GrantedAuthority> fullAuthorities = new HashSet<>();
        Set<Role> configuredRoles = new HashSet<>();
        populate(fullAuthorities, configuredRoles);

        Set<GrantedAuthority> authorities = new HashSet<>();

        if (roleName.equals("ADMIN"))
        {
            authorities.addAll(fullAuthorities);
        }

        // add all privileges from etc/conf/strongbox-authorization.xml for any role that defines there
        configuredRoles.forEach(role ->
                                {
                                    if (role.getName()
                                            .equalsIgnoreCase(roleName))
                                    {
                                        role.getPrivileges()
                                            .forEach(
                                                    privilegeName -> authorities.add(
                                                            new SimpleGrantedAuthority(privilegeName.toUpperCase())));
                                    }
                                });

        try
        {
            Roles configuredRole = Roles.valueOf(roleName);
            authorities.addAll(configuredRole.getPrivileges());
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Unable to find predefined role by name " + roleName);
        }

        return authorities;
    }

    private void populate(Set<GrantedAuthority> fullAuthorities,
                          Set<Role> configuredRoles)
    {
        authorizationConfigProvider.get()
                                   .ifPresent(
                                           config ->
                                           {
                                               try
                                               {
                                                   config.getRoles()
                                                         .getRoles()
                                                         .forEach(role -> role.getPrivileges()
                                                                              .forEach(
                                                                                      privilegeName -> fullAuthorities.add(
                                                                                              new SimpleGrantedAuthority(
                                                                                                                                privilegeName.toUpperCase()))));

                                                   configuredRoles.addAll(config.getRoles()
                                                                                .getRoles());
                                               }
                                               catch (Exception e)
                                               {
                                                   logger.error("Unable to process authorization config",
                                                                e);
                                               }
                                           });
    }
}
