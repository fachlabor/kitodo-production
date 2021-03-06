/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.security;

import de.sub.goobi.config.ConfigCore;

import java.util.Objects;

import org.kitodo.config.Parameters;
import org.kitodo.data.database.beans.LdapGroup;
import org.kitodo.data.database.beans.LdapServer;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.services.ServiceManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

public class DynamicAuthenticationProvider implements AuthenticationProvider {

    private static DynamicAuthenticationProvider instance = null;
    private AuthenticationProvider authenticationProvider;
    private boolean ldapAuthentication = false;
    private ServiceManager serviceManager = new ServiceManager();

    /**
     * Package-private Constructor for DynamicAuthenticationProvider which also
     * sets instance variable for singleton usage.
     */
    DynamicAuthenticationProvider() {
        if (Objects.equals(instance, null)) {
            synchronized (DynamicAuthenticationProvider.class) {
                if (Objects.equals(instance, null)) {
                    instance = this;
                }
            }
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (ldapAuthentication) {
            try {
                User user = serviceManager.getUserService().getByLogin(authentication.getName());
                configureAndActivateLdapAuthentication(user.getLdapGroup());
            } catch (DAOException e) {
                // getByLogin() throws DAOExeption, it must be converted in
                // UsernameNotFoundException
                // in order to match interface method signature
                throw new UsernameNotFoundException("Could not read Username from database!");
            }
        }
        return authenticationProvider.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authenticationProvider.supports(authentication);
    }

    /**
     * This method activates ldap authentication and configures ldap url and
     * userDn pattern.
     *
     * @param ldapGroup
     *            The ldapGroup Object.
     */
    public void configureAndActivateLdapAuthentication(LdapGroup ldapGroup) {

        LdapServer ldapServer = ldapGroup.getLdapServer();

        if (ldapServer != null) {
            DefaultSpringSecurityContextSource ldapContextSource = new DefaultSpringSecurityContextSource(
                    ldapServer.getUrl());
            ldapContextSource.afterPropertiesSet();

            BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource);
            String userDn = convertKitodoLdapUserDnToSpringSecurityPattern(ldapGroup.getUserDN());
            authenticator.setUserDnPatterns(new String[] {userDn });

            this.authenticationProvider = new LdapAuthenticationProvider(authenticator,
                    new CustomLdapAuthoritiesPopulator());
        } else {
            throw new AuthenticationServiceException("No ldap-server specified on users ldap-group");
        }
    }

    /**
     * This method activates database authentication.
     */
    public void activateDatabaseAuthentication() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(serviceManager.getUserService());
        daoAuthenticationProvider.setPasswordEncoder(new SecurityPasswordEncoder());
        this.authenticationProvider = daoAuthenticationProvider;
    }

    /**
     * Return singleton variable of type DynamicAuthenticationProvider.
     *
     * @return unique instance of DynamicAuthenticationProvider
     */
    public static DynamicAuthenticationProvider getInstance() {
        return instance;
    }

    /**
     * This method reads local config and sets authentication flag.
     */
    public void readLocalConfig() {
        ldapAuthentication = ConfigCore.getBooleanParameter(Parameters.LDAP_USE);
    }

    /**
     * This method initializes the authentication provider with database
     * authentication.
     */
    public void initializeAuthenticationProvider() {
        activateDatabaseAuthentication();
    }

    private String convertKitodoLdapUserDnToSpringSecurityPattern(String userDn) {
        return userDn.replaceFirst("\\{login}", "{0}");
    }

    /**
     * Gets ldapAuthentication.
     *
     * @return The ldapAuthentication.
     */
    public boolean isLdapAuthentication() {
        return ldapAuthentication;
    }

    /**
     * Sets ldapAuthentication.
     *
     * @param ldapAuthentication
     *            The ldapAuthentication.
     */
    public void setLdapAuthentication(boolean ldapAuthentication) {
        this.ldapAuthentication = ldapAuthentication;
    }
}
