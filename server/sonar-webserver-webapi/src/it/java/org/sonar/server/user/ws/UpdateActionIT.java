/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UpdateActionIT {

  public static final String USER_LOGIN = "john";
  public static final String USER_INITIAL_NAME = "John";
  public static final String USER_INITIAL_EMAIL = "john@email.com";
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final ManagedInstanceService managedInstanceService = mock();
  private final WsActionTester ws = new WsActionTester(new UpdateAction(
    new UserUpdater(mock(NewUserNotifier.class), dbClient, new DefaultGroupFinder(db.getDbClient()), settings.asConfig(), null, localAuthentication),
    userSession, new UserJsonWriter(userSession), dbClient, managedInstanceService));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup();
  }

  @Test
  public void update_user() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("name", "Jon Snow")
      .setParam("email", "jon.snow@thegreatw.all")
      .execute()
      .assertJson(getClass(), "update_user.json");
  }

  @Test
  public void fail_on_update_name_non_local_user() {
    createUser(false);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .setParam("name", "Jean Neige")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("It is not allowed to update name for this user");
  }

  @Test
  public void fail_on_update_email_non_local_user() {
    createUser(false);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .setParam("email", "jean.neige@thegreatw.all")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("It is not allowed to update email for this user");
  }

  @Test
  public void update_only_name() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("name", "Jon Snow")
      .execute()
      .assertJson(getClass(), "update_name.json");
  }

  @Test
  public void update_only_email() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("email", "jon.snow@thegreatw.all")
      .execute()
      .assertJson(getClass(), "update_email.json");
  }

  @Test
  public void blank_email_is_updated_to_null() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("email", "")
      .execute()
      .assertJson(getClass(), "blank_email_is_updated_to_null.json");

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto.getEmail()).isNull();
  }

  @Test
  public void remove_scm_accounts() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setMultiParam("scmAccount", singletonList(""))
      .execute();

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto.getSortedScmAccounts()).isEmpty();
  }

  @Test
  public void update_only_scm_accounts() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setMultiParam("scmAccount", singletonList("jon.snow"))
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");

    UserDto user = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(user.getSortedScmAccounts()).containsOnly("jon.snow");
  }

  @Test
  public void update_scm_account() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setMultiParam("scmAccount", List.of("jon", "snow"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(user.getSortedScmAccounts()).containsExactly("jon", "snow");
  }

  @Test
  public void fail_when_duplicates_characters_in_scm_account_values() {
    createUser();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .setMultiParam("scmAccount", Arrays.asList("jon.snow", "jon.snow", "jon.jon", "jon.snow"))
        .execute();
    }).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Duplicate SCM account: 'jon.snow'");
    ;

  }

  @Test
  public void fail_when_whitespace_characters_in_scm_account_values() {
    createUser();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .setMultiParam("scmAccount", Arrays.asList("jon.snow", "jon.snow", "jon.jon", "   jon.snow  "))
        .execute();
    }).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("SCM account cannot start or end with whitespace: '   jon.snow  '");
    ;

  }

  @Test
  public void update_scm_account_ordered_case_insensitive() {
    createUser();

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setMultiParam("scmAccount", Arrays.asList("jon.3", "Jon.1", "JON.2"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(user.getSortedScmAccounts()).containsExactly("jon.1", "jon.2", "jon.3");
  }

  @Test
  public void fail_on_missing_permission() {
    createUser();
    userSession.logIn("polop");

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_on_unknown_user() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", USER_LOGIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'john' doesn't exist");
  }

  @Test
  public void fail_on_disabled_user() {
    db.users().insertUser(u -> u.setLogin(USER_LOGIN).setActive(false));

    TestRequest request = ws.newRequest()
      .setParam("login", USER_LOGIN);
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'john' doesn't exist");
  }

  @Test
  public void fail_on_invalid_email() {
    createUser();

    TestRequest request = ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("email", "invalid-email");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Email 'invalid-email' is not valid");
  }

  @Test
  public void handle_whenInstanceManagedAndNameUpdate_shouldThrow() {
    createUser();
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    TestRequest updateRequest = ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("name", "Jon Snow");

    assertThatThrownBy(updateRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("It is not allowed to update name for this user");
  }

  @Test
  public void handle_whenInstanceManagedAndEmailUpdate_shouldThrow() {
    createUser();
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    TestRequest updateRequest = ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("email", "john@new-email.com");

    assertThatThrownBy(updateRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("It is not allowed to update email for this user");
  }

  @Test
  public void handle_whenInstanceManagedAndSCMAccountUpdate_shouldUpdate() {
    createUser();
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setMultiParam("scmAccount", List.of("jon", "snow"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, USER_LOGIN);
    assertThat(user.getSortedScmAccounts()).containsExactly("jon", "snow");
  }

  @Test
  public void handle_whenInstanceManagedAndNotAnonymous_shouldThrow() {
    userSession.anonymous();
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    TestRequest updateRequest = ws.newRequest();

    assertThatThrownBy(updateRequest::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(4);
  }

  private void createUser() {
    createUser(true);
  }

  private void createUser(boolean local) {
    UserDto userDto = newUserDto()
      .setEmail(USER_INITIAL_EMAIL)
      .setLogin(USER_LOGIN)
      .setName(USER_INITIAL_NAME)
      .setScmAccounts(newArrayList("jn"))
      .setActive(true)
      .setLocal(local)
      .setExternalLogin("jo")
      .setExternalIdentityProvider("sonarqube");
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
  }
}
