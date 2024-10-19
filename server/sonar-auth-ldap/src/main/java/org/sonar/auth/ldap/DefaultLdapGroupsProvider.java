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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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
public class DefaultLdapGroupsProvider implements LdapGroupsProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLdapGroupsProvider.class);

  private final Map<String, LdapContextFactory> contextFactories;
  private final Map<String, LdapUserMapping> userMappings;
  private final Map<String, LdapGroupMapping> groupMappings;

  public DefaultLdapGroupsProvider(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings, Map<String, LdapGroupMapping> groupMapping) {
    this.contextFactories = contextFactories;
    this.userMappings = userMappings;
    this.groupMappings = groupMapping;
  }

  /**
   * @throws LdapException if unable to retrieve groups
   */
  @Override
  public Collection<String> doGetGroups(Context context) {
    return getGroups(context.serverKey(), context.username());
  }

  private Collection<String> getGroups(String serverKey, String username) {
    checkPrerequisites(username);
    Set<String> groups = new HashSet<>();
    if (groupMappings.containsKey(serverKey)) {
      SearchResult searchResult = searchUserGroups(username, serverKey);
      if (searchResult != null) {
        try {
          NamingEnumeration<SearchResult> result = groupMappings
            .get(serverKey)
            .createSearch(contextFactories.get(serverKey), searchResult).find();
          groups.addAll(mapGroups(serverKey, result));
        } catch (NamingException e) {
          LOG.debug(e.getMessage(), e);
          throw new LdapException(format("Unable to retrieve groups for user %s in server with key <%s>", username, serverKey), e);
        }
      }
    }
    return groups;
  }

  private void checkPrerequisites(String username) {
    if (userMappings.isEmpty() || groupMappings.isEmpty()) {
      throw new LdapException(format("Unable to retrieve details for user %s: No user or group mapping found.", username));
    }
  }

  private SearchResult searchUserGroups(String username, String serverKey) {
    try {
      LOG.debug("Requesting groups for user {}", username);
      return userMappings.get(serverKey).createSearch(contextFactories.get(serverKey), username)
        .returns(groupMappings.get(serverKey).getRequiredUserAttributes())
        .findUnique();
    } catch (NamingException e) {
      // just in case if Sonar silently swallowed exception
      LOG.debug(e.getMessage(), e);
      throw new LdapException(format("Unable to retrieve groups for user %s in server with key <%s>", username, serverKey), e);
    }
  }

  /**
   * Map all the groups.
   *
   * @param serverKey    The index we use to choose the correct {@link LdapGroupMapping}.
   * @param searchResult The {@link SearchResult} from the search for the user.
   * @return A {@link Collection} of groups the user is member of.
   * @throws NamingException
   */
  private Collection<String> mapGroups(String serverKey, NamingEnumeration<SearchResult> searchResult) throws NamingException {
    Set<String> groups = new HashSet<>();
    while (searchResult.hasMoreElements()) {
      SearchResult obj = searchResult.nextElement();
      Attributes attributes = obj.getAttributes();
      String groupId = (String) attributes.get(groupMappings.get(serverKey).getIdAttribute()).get();
      groups.add(groupId);
    }
    return groups;
  }

}
