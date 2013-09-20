/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.user.UserDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class DefaultUserServiceTest {

  UserFinder finder = mock(UserFinder.class);
  UserDao dao = mock(UserDao.class);
  DefaultUserService service = new DefaultUserService(finder, dao);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void parse_query() throws Exception {
    service.find(ImmutableMap.<String, Object> of(
        "logins", "simon,loic",
        "includeDeactivated", "true",
        "s", "sim"
        ));

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
    service.find(Maps.<String, Object> newHashMap());

    verify(finder, times(1)).find(argThat(new ArgumentMatcher<UserQuery>() {
      @Override
      public boolean matches(Object o) {
        UserQuery query = (UserQuery) o;
        return !query.includeDeactivated() && query.logins() == null && query.searchText() == null;
      }
    }));
  }

  @Test
  public void self_deactivation_is_not_possible() throws Exception {
    try {
      MockUserSession.set().setLogin("simon").setPermissions(GlobalPermissions.SYSTEM_ADMIN);
      service.deactivate("simon");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Self-deactivation is not possible");
      verify(dao, never()).deactivateUserByLogin("simon");
    }
  }

  @Test
  public void user_deactivation_requires_admin_permission() throws Exception {
    try {
      MockUserSession.set().setLogin("simon").setPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.deactivate("julien");
      fail();
    } catch (ForbiddenException e) {
      verify(dao, never()).deactivateUserByLogin("simon");
    }
  }

  @Test
  public void deactivate_user() throws Exception {
    MockUserSession.set().setLogin("simon").setPermissions(GlobalPermissions.SYSTEM_ADMIN);
    service.deactivate("julien");
    verify(dao).deactivateUserByLogin("julien");
  }

  @Test
  public void fail_to_deactivate_when_blank_login() throws Exception {
    MockUserSession.set().setLogin("simon").setPermissions(GlobalPermissions.SYSTEM_ADMIN);
    try {
      service.deactivate("");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Login is missing");
      verifyZeroInteractions(dao);
    }
  }
}
