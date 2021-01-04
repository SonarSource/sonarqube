/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.auth.ldap;

import java.util.Map;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.security.Authenticator;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @author Evgeny Mandrikov
 */
public class LdapAuthenticator extends Authenticator {

  private static final Logger LOG = Loggers.get(LdapAuthenticator.class);
  private final Map<String, LdapContextFactory> contextFactories;
  private final Map<String, LdapUserMapping> userMappings;

  public LdapAuthenticator(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings) {
    this.contextFactories = contextFactories;
    this.userMappings = userMappings;
  }

  @Override
  public boolean doAuthenticate(Context context) {
    return authenticate(context.getUsername(), context.getPassword());
  }

  /**
   * Authenticate the user against LDAP servers until first success.
   * @param login The login to use.
   * @param password The password to use.
   * @return false if specified user cannot be authenticated with specified password on any LDAP server
   */
  public boolean authenticate(String login, String password) {
    for (String ldapKey : userMappings.keySet()) {
      final String principal;
      if (contextFactories.get(ldapKey).isSasl()) {
        principal = login;
      } else {
        final SearchResult result;
        try {
          result = userMappings.get(ldapKey).createSearch(contextFactories.get(ldapKey), login).findUnique();
        } catch (NamingException e) {
          LOG.debug("User {} not found in server {}: {}", login, ldapKey, e.getMessage());
          continue;
        }
        if (result == null) {
          LOG.debug("User {} not found in {}", login, ldapKey);
          continue;
        }
        principal = result.getNameInNamespace();
      }
      boolean passwordValid;
      if (contextFactories.get(ldapKey).isGssapi()) {
        passwordValid = checkPasswordUsingGssapi(principal, password, ldapKey);
      } else {
        passwordValid = checkPasswordUsingBind(principal, password, ldapKey);
      }
      if (passwordValid) {
        return true;
      }
    }
    LOG.debug("User {} not found", login);
    return false;
  }

  private boolean checkPasswordUsingBind(String principal, String password, String ldapKey) {
    if (StringUtils.isEmpty(password)) {
      LOG.debug("Password is blank.");
      return false;
    }
    InitialDirContext context = null;
    try {
      context = contextFactories.get(ldapKey).createUserContext(principal, password);
      return true;
    } catch (NamingException e) {
      LOG.debug("Password not valid for user {} in server {}: {}", principal, ldapKey, e.getMessage());
      return false;
    } finally {
      ContextHelper.closeQuietly(context);
    }
  }

  private boolean checkPasswordUsingGssapi(String principal, String password, String ldapKey) {
    // Use our custom configuration to avoid reliance on external config
    Configuration.setConfiguration(new Krb5LoginConfiguration());
    LoginContext lc;
    try {
      lc = new LoginContext(getClass().getName(), new CallbackHandlerImpl(principal, password));
      lc.login();
    } catch (LoginException e) {
      // Bad username: Client not found in Kerberos database
      // Bad password: Integrity check on decrypted field failed
      LOG.debug("Password not valid for {} in server {}: {}", principal, ldapKey, e.getMessage());
      return false;
    }
    try {
      lc.logout();
    } catch (LoginException e) {
      LOG.warn("Logout fails", e);
    }
    return true;
  }

}
