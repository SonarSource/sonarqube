/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultUserServiceTest {

  private UserFinder finder = mock(UserFinder.class);
  private DefaultUserService service = new DefaultUserService(finder);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void parse_query() {
    service.find(ImmutableMap.of(
      "logins", "simon,loic",
      "includeDeactivated", "true",
      "s", "sim"));

    verify(finder, times(1)).find(argThat(new ArgumentMatcher<UserQuery>() {
      @Override
      public boolean matches(Object o) {
        UserQuery query = (UserQuery) o;
        return query.includeDeactivated() &&
          query.logins().contains("simon") && query.logins().contains("loic") && query.logins().size() == 2 &&
          query.searchText().equals("sim");
      }
    }));
  }

  @Test
  public void test_empty_query() throws Exception {
    service.find(Maps.newHashMap());

    verify(finder, times(1)).find(argThat(new ArgumentMatcher<UserQuery>() {
      @Override
      public boolean matches(Object o) {
        UserQuery query = (UserQuery) o;
        return !query.includeDeactivated() && query.logins() == null && query.searchText() == null;
      }
    }));
  }
}
