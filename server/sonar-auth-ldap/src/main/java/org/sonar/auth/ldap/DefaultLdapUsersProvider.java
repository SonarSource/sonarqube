/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class DefaultLdapUsersProvider implements LdapUsersProvider {

  private static final Logger LOG = Loggers.get(DefaultLdapUsersProvider.class);
  private final Map<String, LdapContextFactory> contextFactories;
  private final Map<String, LdapUserMapping> userMappings;

  public DefaultLdapUsersProvider(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings) {
    this.contextFactories = contextFactories;
    this.userMappings = userMappings;
  }

  private static String getAttributeValue(@Nullable Attribute attribute) throws NamingException {
    if (attribute == null) {
      return "";
    }
    return (String) attribute.get();
  }

  @Override
  public LdapUserDetails doGetUserDetails(Context context) {
    return getUserDetails(context.getUsername());
  }

  /**
   * @return details for specified user, or null if such user doesn't exist
   * @throws LdapException if unable to retrieve details
   */
  public LdapUserDetails getUserDetails(String username) {
    LOG.debug("Requesting details for user {}", username);
    // If there are no userMappings available, we can not retrieve user details.
    if (userMappings.isEmpty()) {
      String errorMessage = format("Unable to retrieve details for user %s: No user mapping found.", username);
      LOG.debug(errorMessage);
      throw new LdapException(errorMessage);
    }
    LdapUserDetails details = null;
    LdapException exception = null;
    for (Map.Entry<String, LdapUserMapping> serverEntry : userMappings.entrySet()) {
      String serverKey = serverEntry.getKey();
      LdapUserMapping ldapUserMapping = serverEntry.getValue();

      SearchResult searchResult = null;
      try {
        searchResult = ldapUserMapping.createSearch(contextFactories.get(serverKey), username)
          .returns(ldapUserMapping.getEmailAttribute(), ldapUserMapping.getRealNameAttribute())
          .findUnique();
      } catch (NamingException e) {
        // just in case if Sonar silently swallowed exception
        LOG.debug(e.getMessage(), e);
        exception = new LdapException("Unable to retrieve details for user " + username + " in " + serverKey, e);
      }
      if (searchResult != null) {
        try {
          details = mapUserDetails(ldapUserMapping, searchResult);
          // if no exceptions occur, we found the user and mapped his details.
          break;
        } catch (NamingException e) {
          // just in case if Sonar silently swallowed exception
          LOG.debug(e.getMessage(), e);
          exception = new LdapException("Unable to retrieve details for user " + username + " in " + serverKey, e);
        }
      } else {
        // user not found
        LOG.debug("User {} not found in {}", username, serverKey);
      }
    }
    if (details == null && exception != null) {
      // No user found and there is an exception so there is a reason the user could not be found.
      throw exception;
    }
    return details;
  }

  private static LdapUserDetails mapUserDetails(LdapUserMapping ldapUserMapping, SearchResult searchResult) throws NamingException {
    Attributes attributes = searchResult.getAttributes();
    LdapUserDetails details;
    details = new LdapUserDetails();
    details.setName(getAttributeValue(attributes.get(ldapUserMapping.getRealNameAttribute())));
    details.setEmail(getAttributeValue(attributes.get(ldapUserMapping.getEmailAttribute())));
    return details;
  }

}
