/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefaultUserServiceTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  UserIndex userIndex = mock(UserIndex.class);
  UserFinder finder = mock(UserFinder.class);
  UserUpdater userUpdater = mock(UserUpdater.class);
  DefaultUserService service = new DefaultUserService(userIndex, userUpdater, finder, userSessionRule);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void parse_query() {
    service.find(ImmutableMap.<String, Object>of(
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
    service.find(Maps.<String, Object>newHashMap());

    verify(finder, times(1)).find(argThat(new ArgumentMatcher<UserQuery>() {
      @Override
      public boolean matches(Object o) {
        UserQuery query = (UserQuery) o;
        return !query.includeDeactivated() && query.logins() == null && query.searchText() == null;
      }
    }));
  }

  @Test
  public void self_deactivation_is_not_possible() {
    try {
      userSessionRule.login("simon").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
      service.deactivate("simon");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Self-deactivation is not possible");
      verify(userUpdater, never()).deactivateUserByLogin("simon");
    }
  }

  @Test
  public void user_deactivation_requires_admin_permission() {
    try {
      userSessionRule.login("simon").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.deactivate("julien");
      fail();
    } catch (ForbiddenException e) {
      verify(userUpdater, never()).deactivateUserByLogin("simon");
    }
  }

  @Test
  public void deactivate_user() {
    userSessionRule.login("simon").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    service.deactivate("julien");
    verify(userUpdater).deactivateUserByLogin("julien");
  }

  @Test
  public void fail_to_deactivate_when_blank_login() {
    userSessionRule.login("simon").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    try {
      service.deactivate("");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Login is missing");
    }
  }

  @Test
  public void create() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("name", "John");
    params.put("email", "john@email.com");
    params.put("scm_accounts", newArrayList("jn"));
    service.create(params);

    ArgumentCaptor<NewUser> newUserCaptor = ArgumentCaptor.forClass(NewUser.class);
    verify(userUpdater).create(newUserCaptor.capture());
    assertThat(newUserCaptor.getValue().login()).isEqualTo("john");
    assertThat(newUserCaptor.getValue().name()).isEqualTo("John");
    assertThat(newUserCaptor.getValue().email()).isEqualTo("john@email.com");
    assertThat(newUserCaptor.getValue().scmAccounts()).containsOnly("jn");
  }

  @Test
  public void update() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("name", "John");
    params.put("email", "john@email.com");
    params.put("scm_accounts", newArrayList("jn"));
    params.put("password", "1234");
    params.put("password_confirmation", "1234");

    service.update(params);

    ArgumentCaptor<UpdateUser> userCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().login()).isEqualTo("john");
    assertThat(userCaptor.getValue().name()).isEqualTo("John");
    assertThat(userCaptor.getValue().email()).isEqualTo("john@email.com");
    assertThat(userCaptor.getValue().scmAccounts()).containsOnly("jn");
    assertThat(userCaptor.getValue().password()).isEqualTo("1234");
    assertThat(userCaptor.getValue().passwordConfirmation()).isEqualTo("1234");
  }

  @Test
  public void update_only_name() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("name", "John");
    service.update(params);

    ArgumentCaptor<UpdateUser> userCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isTrue();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_email() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("email", "john@email.com");
    service.update(params);

    ArgumentCaptor<UpdateUser> userCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isTrue();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_scm_accounts() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("scm_accounts", newArrayList("jn"));
    service.update(params);

    ArgumentCaptor<UpdateUser> userCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isTrue();
    assertThat(userCaptor.getValue().isPasswordChanged()).isFalse();
  }

  @Test
  public void update_only_password() {
    Map<String, Object> params = newHashMap();
    params.put("login", "john");
    params.put("password", "1234");
    params.put("password_confirmation", "1234");
    service.update(params);

    ArgumentCaptor<UpdateUser> userCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(userCaptor.capture());
    assertThat(userCaptor.getValue().isNameChanged()).isFalse();
    assertThat(userCaptor.getValue().isEmailChanged()).isFalse();
    assertThat(userCaptor.getValue().isScmAccountsChanged()).isFalse();
    assertThat(userCaptor.getValue().isPasswordChanged()).isTrue();
  }

  @Test
  public void get_by_login() {
    service.getByLogin("john");
    verify(userIndex).getNullableByLogin("john");
  }

  @Test
  public void index() {
    service.index();
    verify(userUpdater).index();
  }
}
