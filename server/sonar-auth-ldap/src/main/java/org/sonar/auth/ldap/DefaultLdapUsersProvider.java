/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * @author Evgeny Mandrikov
 */
@ServerSide
public class DefaultLdapUsersProvider implements LdapUsersProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapUsersProvider.class);
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
    return getUserDetails(context.serverKey(), context.username());
  }

  /**
   * @return details for specified user, or null if such user doesn't exist
   * @throws LdapException if unable to retrieve details
   */
  private LdapUserDetails getUserDetails(String serverKey, String username) {
    LOG.debug("Requesting details for user {}", username);
    // If there are no userMappings available, we can not retrieve user details.
    LdapUserMapping ldapUserMapping = userMappings.get(serverKey);
    if (ldapUserMapping == null) {
      String errorMessage = format("Unable to retrieve details for user %s and server key %s: No user mapping found.", username, serverKey);
      LOG.debug(errorMessage);
      throw new LdapException(errorMessage);
    }
    SearchResult searchResult;
    try {
      searchResult = ldapUserMapping.createSearch(contextFactories.get(serverKey), username)
        .returns(ldapUserMapping.getEmailAttribute(), ldapUserMapping.getRealNameAttribute())
        .findUnique();

      if (searchResult != null) {
        return mapUserDetails(ldapUserMapping, searchResult);
      } else {
        LOG.debug("User {} not found in {}", username, serverKey);
        return null;
      }
    } catch (NamingException e) {
      // just in case if Sonar silently swallowed exception
      LOG.debug(e.getMessage(), e);
      throw new LdapException("Unable to retrieve details for user " + username + " in " + serverKey, e);
    }
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
