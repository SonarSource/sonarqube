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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * @author Evgeny Mandrikov
 */
public class LdapGroupsProvider extends ExternalGroupsProvider {

  private static final Logger LOG = Loggers.get(LdapGroupsProvider.class);

  private final Map<String, LdapContextFactory> contextFactories;
  private final Map<String, LdapUserMapping> userMappings;
  private final Map<String, LdapGroupMapping> groupMappings;

  public LdapGroupsProvider(Map<String, LdapContextFactory> contextFactories, Map<String, LdapUserMapping> userMappings, Map<String, LdapGroupMapping> groupMapping) {
    this.contextFactories = contextFactories;
    this.userMappings = userMappings;
    this.groupMappings = groupMapping;
  }

  @Override
  public Collection<String> doGetGroups(Context context) {
    return getGroups(context.getUsername());
  }

  /**
   * @throws LdapException if unable to retrieve groups
   */
  public Collection<String> getGroups(String username) {
    checkPrerequisites(username);
    Set<String> groups = new HashSet<>();
    List<LdapException> exceptions = new ArrayList<>();
    for (String serverKey : userMappings.keySet()) {
      if (!groupMappings.containsKey(serverKey)) {
        // No group mapping for this ldap instance.
        continue;
      }
      SearchResult searchResult = searchUserGroups(username, exceptions, serverKey);

      if (searchResult != null) {
        try {
          NamingEnumeration<SearchResult> result = groupMappings
            .get(serverKey)
            .createSearch(contextFactories.get(serverKey), searchResult).find();
          groups.addAll(mapGroups(serverKey, result));
          // if no exceptions occur, we found the user and his groups and mapped his details.
          break;
        } catch (NamingException e) {
          // just in case if Sonar silently swallowed exception
          LOG.debug(e.getMessage(), e);
          exceptions.add(new LdapException(format("Unable to retrieve groups for user %s in %s", username, serverKey), e));
        }
      } else {
        // user not found
        continue;
      }
    }
    checkResults(groups, exceptions);
    return groups;
  }

  private static void checkResults(Set<String> groups, List<LdapException> exceptions) {
    if (groups.isEmpty() && !exceptions.isEmpty()) {
      // No groups found and there is an exception so there is a reason the user could not be found.
      throw exceptions.iterator().next();
    }
  }

  private void checkPrerequisites(String username) {
    if (userMappings.isEmpty() || groupMappings.isEmpty()) {
      throw new LdapException(format("Unable to retrieve details for user %s: No user or group mapping found.", username));
    }
  }

  private SearchResult searchUserGroups(String username, List<LdapException> exceptions, String serverKey) {
    SearchResult searchResult = null;
    try {
      LOG.debug("Requesting groups for user {}", username);

      searchResult = userMappings.get(serverKey).createSearch(contextFactories.get(serverKey), username)
        .returns(groupMappings.get(serverKey).getRequiredUserAttributes())
        .findUnique();
    } catch (NamingException e) {
      // just in case if Sonar silently swallowed exception
      LOG.debug(e.getMessage(), e);
      exceptions.add(new LdapException(format("Unable to retrieve groups for user %s in %s", username, serverKey), e));
    }
    return searchResult;
  }

  /**
   * Map all the groups.
   *
   * @param serverKey The index we use to choose the correct {@link LdapGroupMapping}.
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
