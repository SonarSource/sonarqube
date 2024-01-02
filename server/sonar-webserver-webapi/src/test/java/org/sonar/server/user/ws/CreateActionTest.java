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

import java.util.List;
import java.util.Optional;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.ws.CreateAction.CreateRequest;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Users.CreateWsResponse;
import org.sonarqube.ws.Users.CreateWsResponse.User;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_EMAIL;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_LOGIN;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_NAME;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_SCM_ACCOUNTS;

public class CreateActionTest {

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private GroupDto defaultGroup;
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final WsActionTester tester = new WsActionTester(new CreateAction(db.getDbClient(), new UserUpdater(mock(NewUserNotifier.class),
    db.getDbClient(), userIndexer, new DefaultGroupFinder(db.getDbClient()), settings.asConfig(), new NoOpAuditPersister(), localAuthentication), userSessionRule));

  @Before
  public void setUp() {
    defaultGroup = db.users().insertDefaultGroup();
  }

  @Test
  public void create_user() {
    logInAsSystemAdministrator();

    CreateWsResponse response = call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("jn"))
      .setPassword("1234")
      .build());

    assertThat(response.getUser())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getScmAccountsList, User::getLocal)
      .containsOnly("john", "John", "john@email.com", singletonList("jn"), true);

    // exists in index
    assertThat(es.client().search(EsClient.prepareSearch(UserIndexDefinition.TYPE_USER)
      .source(new SearchSourceBuilder()
        .query(boolQuery()
          .must(termQuery(FIELD_LOGIN, "john"))
          .must(termQuery(FIELD_NAME, "John"))
          .must(termQuery(FIELD_EMAIL, "john@email.com"))
          .must(termQuery(FIELD_SCM_ACCOUNTS, "jn")))))
      .getHits().getHits()).hasSize(1);

    // exists in db
    Optional<UserDto> dbUser = db.users().selectUserByLogin("john");
    assertThat(dbUser).isPresent();

    // member of default group
    assertThat(db.users().selectGroupUuidsOfUser(dbUser.get())).containsOnly(defaultGroup.getUuid());
  }

  @Test
  public void create_local_user() {
    logInAsSystemAdministrator();

    call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setPassword("1234")
      .setLocal(true)
      .build());

    assertThat(db.users().selectUserByLogin("john").get())
      .extracting(UserDto::isLocal, UserDto::getExternalIdentityProvider, UserDto::getExternalLogin)
      .containsOnly(true, "sonarqube", "john");
  }

  @Test
  public void create_none_local_user() {
    logInAsSystemAdministrator();

    call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setLocal(false)
      .build());

    assertThat(db.users().selectUserByLogin("john").get())
      .extracting(UserDto::isLocal, UserDto::getExternalIdentityProvider, UserDto::getExternalLogin)
      .containsOnly(false, "sonarqube", "john");
  }

  @Test
  public void create_user_with_comma_in_scm_account() {
    logInAsSystemAdministrator();

    CreateWsResponse response = call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("j,n"))
      .setPassword("1234")
      .build());

    assertThat(response.getUser().getScmAccountsList()).containsOnly("j,n");
  }

  @Test
  public void fail_when_whitespace_characters_in_scm_account_values() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin("john")
        .setName("John")
        .setEmail("john@email.com")
        .setScmAccounts(List.of("admin", "  admin  "))
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("SCM account cannot start or end with whitespace: '  admin  '");
  }

  @Test
  public void fail_when_duplicates_characters_in_scm_account_values() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin("john")
        .setName("John")
        .setEmail("john@email.com")
        .setScmAccounts(List.of("admin", "admin"))
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Duplicate SCM account: 'admin'");
  }

  @Test
  public void create_user_with_empty_email() {
    logInAsSystemAdministrator();

    call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setPassword("1234")
      .setEmail("")
      .build());

    assertThat(db.users().selectUserByLogin("john").get())
      .extracting(UserDto::getExternalLogin)
      .isEqualTo("john");
  }

  @Test
  public void reactivate_user() {
    logInAsSystemAdministrator();

    db.users().insertUser(newUserDto("john", "John", "john@email.com").setActive(false));
    userIndexer.indexAll();

    call(CreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("jn"))
      .setPassword("1234")
      .build());

    assertThat(db.users().selectUserByLogin("john").get().isActive()).isTrue();
  }

  @Test
  public void fail_to_reactivate_user_when_active_user_exists() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin(user.getLogin())
        .setName("John")
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("An active user with login '%s' already exists", user.getLogin()));
  }

  @Test
  public void fail_when_missing_login() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin(null)
        .setName("John")
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Login is mandatory and must not be empty");
  }

  @Test
  public void fail_when_login_is_too_short() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin("a")
        .setName("John")
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'login' length (1) is shorter than the minimum authorized (2)");
  }

  @Test
  public void fail_when_missing_name() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin("john")
        .setName(null)
        .setPassword("1234")
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Name is mandatory and must not be empty");
  }

  @Test
  public void fail_when_missing_password() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      call(CreateRequest.builder()
        .setLogin("john")
        .setName("John")
        .setPassword(null)
        .build());
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password is mandatory and must not be empty");
  }

  @Test
  public void fail_when_password_is_set_on_none_local_user() {
    logInAsSystemAdministrator();

    TestRequest request =tester.newRequest()
      .setParam("login","john")
      .setParam("name","John")
      .setParam("password", "1234")
      .setParam("local", "false");

    assertThatThrownBy(() -> {
      request.execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password should only be set on local user");
  }

  @Test
  public void fail_when_email_is_invalid() {
    logInAsSystemAdministrator();

    CreateRequest request = CreateRequest.builder()
      .setLogin("pipo")
      .setName("John")
      .setPassword("1234")
      .setEmail("invalid-email")
      .build();

    assertThatThrownBy(() -> {
      call(request);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Email 'invalid-email' is not valid");
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> executeRequest("john"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(6);
  }

  private CreateWsResponse executeRequest(String login) {
    return call(CreateRequest.builder()
      .setLogin(login)
      .setName("name of " + login)
      .setEmail(login + "@email.com")
      .setScmAccounts(singletonList(login.substring(0, 2)))
      .setPassword("pwd_" + login)
      .build());
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }

  private CreateWsResponse call(CreateRequest createRequest) {
    TestRequest request = tester.newRequest();
    ofNullable(createRequest.getLogin()).ifPresent(e4 -> request.setParam("login", e4));
    ofNullable(createRequest.getName()).ifPresent(e3 -> request.setParam("name", e3));
    ofNullable(createRequest.getEmail()).ifPresent(e2 -> request.setParam("email", e2));
    ofNullable(createRequest.getPassword()).ifPresent(e1 -> request.setParam("password", e1));
    ofNullable(createRequest.getScmAccounts()).ifPresent(e -> request.setMultiParam("scmAccount", e));
    request.setParam("local", Boolean.toString(createRequest.isLocal()));
    return request.executeProtobuf(CreateWsResponse.class);
  }

}
