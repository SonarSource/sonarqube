/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.security;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ExternalGroupsProviderTest {
  @Test
  public void doGetGroupsNoOverride() {
    ExternalGroupsProvider groupsProvider = new ExternalGroupsProvider() {
    };

    String userName = "foo";
    assertThat(groupsProvider.doGetGroups(userName)).isNull();
    assertThat(groupsProvider.doGetGroups(new ExternalGroupsProvider.Context(userName,
      mock(HttpServletRequest.class)))).isNull();
  }

  @Test
  public void doGetGroupsTests() {
    final Map<String, Collection<String>> userGroupsMap = getTestUserGroupMapping();

    ExternalGroupsProvider groupsProvider = new ExternalGroupsProvider() {
      @Override
      public Collection<String> doGetGroups(Context context) {
        Preconditions.checkNotNull(context.getUsername());
        Preconditions.checkNotNull(context.getRequest());

        return userGroupsMap.get(context.getUsername());
      }
    };

    runDoGetGroupsTests(groupsProvider, userGroupsMap);
  }

  @Test
  public void doGetGroupsDeprecatedApi() {
    final Map<String, Collection<String>> userGroupsMap = getTestUserGroupMapping();

    ExternalGroupsProvider groupsProvider = new ExternalGroupsProvider() {
      @Override
      public Collection<String> doGetGroups(String username) {
        Preconditions.checkNotNull(username);

        return userGroupsMap.get(username);
      }
    };

    runDoGetGroupsTests(groupsProvider, userGroupsMap);
  }

  private static void runDoGetGroupsTests(ExternalGroupsProvider groupsProvider, Map<String, Collection<String>> userGroupsMap) {
    for (Map.Entry<String, Collection<String>> userGroupMapEntry : userGroupsMap.entrySet()) {
      Collection<String> groups = groupsProvider.doGetGroups(new ExternalGroupsProvider.Context(
        userGroupMapEntry.getKey(), mock(HttpServletRequest.class)));
      assertThat(groups).isEqualTo(userGroupMapEntry.getValue());
    }
  }

  private static Map<String, Collection<String>> getTestUserGroupMapping() {
    Map<String, Collection<String>> userGroupsMap = new HashMap<>();
    addUserGroupMapping(userGroupsMap, "userWithOneGroups", new String[] {"group1"});
    addUserGroupMapping(userGroupsMap, "userWithTwoGroups", new String[] {"group1", "group2"});
    addUserGroupMapping(userGroupsMap, "userWithNoGroup", new String[] {});
    addUserGroupMapping(userGroupsMap, "userWithNullGroup", null);

    return userGroupsMap;
  }

  private static void addUserGroupMapping(Map<String, Collection<String>> userGroupsMap, String user, @Nullable String[] groups) {
    Collection<String> groupsCollection = null;
    if (groups != null) {
      groupsCollection = new ArrayList<>();
      groupsCollection.addAll(Arrays.asList(groups));
    }

    userGroupsMap.put(user, groupsCollection);
  }
}
