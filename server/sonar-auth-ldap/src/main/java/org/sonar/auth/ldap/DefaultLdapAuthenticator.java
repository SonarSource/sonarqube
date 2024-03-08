/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class DefaultLdapAuthenticator implements LdapAuthenticator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapAuthenticator.class);
  private final Map<String, LdapContextFactory> contextFactories;
  private final Map<String, LdapUserMapping> userMappings;

  public DefaultLdapAuthenticator(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings) {
    this.contextFactories = contextFactories;
    this.userMappings = userMappings;
  }

  @Override
  public LdapAuthenticationResult doAuthenticate(Context context) {
    return authenticate(context.getUsername(), context.getPassword());
  }

  /**
   * Authenticate the user against LDAP servers until first success.
   *
   * @param login    The login to use.
   * @param password The password to use.
   * @return false if specified user cannot be authenticated with specified password on any LDAP server
   */
  private LdapAuthenticationResult authenticate(String login, String password) {
    for (Map.Entry<String, LdapUserMapping> ldapEntry : userMappings.entrySet()) {
      String ldapKey = ldapEntry.getKey();
      LdapUserMapping ldapUserMapping = ldapEntry.getValue();
      LdapContextFactory ldapContextFactory = contextFactories.get(ldapKey);
      final String principal;
      if (ldapContextFactory.isSasl()) {
        principal = login;
      } else {
        SearchResult result = findUser(login, ldapKey, ldapUserMapping, ldapContextFactory);
        if (result == null) {
          continue;
        }
        principal = result.getNameInNamespace();
      }
      boolean passwordValid = isPasswordValid(password, ldapKey, ldapContextFactory, principal);
      if (passwordValid) {
        return LdapAuthenticationResult.success(ldapKey);
      }
    }
    LOG.debug("User {} not found", login);
    return LdapAuthenticationResult.failed();
  }

  private static SearchResult findUser(String login, String ldapKey, LdapUserMapping ldapUserMapping, LdapContextFactory ldapContextFactory) {
    SearchResult result;
    try {
      result = ldapUserMapping.createSearch(ldapContextFactory, login).findUnique();
    } catch (NamingException e) {
      LOG.debug("User {} not found in server <{}>: {}", login, ldapKey, e.toString());
      return null;
    }
    if (result == null) {
      LOG.debug("User {} not found in <{}>", login, ldapKey);
      return null;
    }
    return result;
  }

  private boolean isPasswordValid(String password, String ldapKey, LdapContextFactory ldapContextFactory, String principal) {
    if (ldapContextFactory.isGssapi()) {
      return checkPasswordUsingGssapi(principal, password, ldapKey);
    }
    return checkPasswordUsingBind(principal, password, ldapKey);
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
