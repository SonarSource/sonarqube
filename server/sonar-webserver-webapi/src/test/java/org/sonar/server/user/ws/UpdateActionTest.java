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
package org.sonar.server.user.ws;

import java.util.Arrays;
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
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UpdateActionTest {

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final WsActionTester ws = new WsActionTester(new UpdateAction(
    new UserUpdater(mock(NewUserNotifier.class), dbClient, userIndexer, new DefaultGroupFinder(db.getDbClient()), settings.asConfig(), null, localAuthentication),
    userSession, new UserJsonWriter(userSession), dbClient));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup();
  }

  @Test
  public void update_user() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
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
        .setParam("login", "john")
        .setParam("name", "Jean Neige")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Name cannot be updated for a non-local user");
  }

  @Test
  public void fail_on_update_email_non_local_user() {
    createUser(false);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "john")
        .setParam("email", "jean.neige@thegreatw.all")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Email cannot be updated for a non-local user");
  }

  @Test
  public void update_only_name() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("name", "Jon Snow")
      .execute()
      .assertJson(getClass(), "update_name.json");
  }

  @Test
  public void update_only_email() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "jon.snow@thegreatw.all")
      .execute()
      .assertJson(getClass(), "update_email.json");
  }

  @Test
  public void blank_email_is_updated_to_null() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "")
      .execute()
      .assertJson(getClass(), "blank_email_is_updated_to_null.json");

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(userDto.getEmail()).isNull();
  }

  @Test
  public void remove_scm_accounts() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList(""))
      .execute();

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(userDto.getScmAccounts()).isNull();
  }

  @Test
  public void update_only_scm_accounts() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList("jon.snow"))
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon.snow");
  }

  @Test
  public void update_scm_account_having_coma() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList("jon,snow"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon,snow");
  }

  @Test
  public void fail_when_duplicates_characters_in_scm_account_values() {
    createUser();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "john")
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
        .setParam("login", "john")
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
      .setParam("login", "john")
      .setMultiParam("scmAccount", Arrays.asList("jon.3", "Jon.1", "JON.2"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsExactly("Jon.1", "JON.2", "jon.3");
  }

  @Test
  public void fail_on_missing_permission() {
    createUser();
    userSession.logIn("polop");

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "john")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_on_unknown_user() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "john")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'john' doesn't exist");
  }

  @Test
  public void fail_on_disabled_user() {
    db.users().insertUser(u -> u.setLogin("john").setActive(false));

    TestRequest request = ws.newRequest()
      .setParam("login", "john");
    assertThatThrownBy(() -> {
      request.execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'john' doesn't exist");
  }

  @Test
  public void fail_on_invalid_email() {
    createUser();

    TestRequest request = ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "invalid-email");
    assertThatThrownBy(() -> {
      request.execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Email 'invalid-email' is not valid");
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
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setActive(true)
      .setLocal(local)
      .setExternalLogin("jo")
      .setExternalIdentityProvider("sonarqube");
    dbClient.userDao().insert(dbSession, userDto);
    userIndexer.commitAndIndex(dbSession, userDto);
  }
}
