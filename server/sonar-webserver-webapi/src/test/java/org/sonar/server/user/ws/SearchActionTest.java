/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.Users.SearchWsResponse;
import org.sonarqube.ws.Users.SearchWsResponse.User;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private UserIndex index = new UserIndex(es.client(), System2.INSTANCE);
  private UserIndexer userIndexer = new UserIndexer(db.getDbClient(), es.client());
  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, index, db.getDbClient(), new AvatarResolverImpl()));

  @Test
  public void search_for_all_users() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    userIndexer.indexOnStartup(null);
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getName)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()),
        tuple(user2.getLogin(), user2.getName()));
  }

  @Test
  public void search_with_query() {
    userSession.logIn();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));
    userIndexer.indexOnStartup(null);

    assertThat(ws.newRequest()
      .setParam("q", "user-%_%-")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
    assertThat(ws.newRequest()
      .setParam("q", "user@MAIL.com")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
    assertThat(ws.newRequest()
      .setParam("q", "user-name")
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin)
        .containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void return_avatar() {
    UserDto user = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    userIndexer.indexOnStartup(null);
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getAvatar)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), "6a6c19fea4a3676970167ce51f39e6ee"));
  }

  @Test
  public void return_scm_accounts() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(asList("john1", "john2")));
    userIndexer.indexOnStartup(null);
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, u -> u.getScmAccounts().getScmAccountsList())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList("john1", "john2")));
  }

  @Test
  public void return_tokens_count_when_system_administer() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);
    userIndexer.indexOnStartup(null);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getTokensCount)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), 2));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasTokensCount)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_email_only_when_system_administer() {
    UserDto user = db.users().insertUser();
    userIndexer.indexOnStartup(null);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getEmail)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getEmail()));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasEmail)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_user_not_having_email() {
    UserDto user = db.users().insertUser(u -> u.setEmail(null));
    userIndexer.indexOnStartup(null);
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::hasEmail)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_groups_only_when_system_administer() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup(db.getDefaultOrganization(), "group1");
    GroupDto group2 = db.users().insertGroup(db.getDefaultOrganization(), "group2");
    GroupDto group3 = db.users().insertGroup(db.getDefaultOrganization(), "group3");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);
    userIndexer.indexOnStartup(null);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, u -> u.getGroups().getGroupsList())
        .containsExactlyInAnyOrder(tuple(user.getLogin(), asList(group1.getName(), group2.getName())));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasGroups)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void return_external_information() {
    UserDto user = db.users().insertUser();
    userIndexer.indexOnStartup(null);
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getExternalIdentity, User::getExternalProvider)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin(), user.getExternalIdentityProvider()));
  }

  @Test
  public void return_external_identity_only_when_system_administer() {
    UserDto user = db.users().insertUser();
    userIndexer.indexOnStartup(null);

    userSession.logIn().setSystemAdministrator();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getExternalIdentity)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin()));

    userSession.logIn();
    assertThat(ws.newRequest()
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::hasExternalIdentity)
        .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void only_return_login_and_name_when_not_logged() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization());
    db.users().insertMember(group, user);
    userIndexer.indexOnStartup(null);
    userSession.anonymous();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::getName, User::hasTokensCount, User::hasScmAccounts, User::hasAvatar, User::hasGroups)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getName(), false, false, false, false));
  }

  @Test
  public void return_last_connection_date_when_system_administer() {
    UserDto userWithLastConnectionDate = db.users().insertUser();
    db.users().updateLastConnectionDate(userWithLastConnectionDate, 10_000_000_000L);
    UserDto userWithoutLastConnectionDate = db.users().insertUser();
    userIndexer.indexOnStartup(null);
    userSession.logIn().setSystemAdministrator();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList())
      .extracting(User::getLogin, User::hasLastConnectionDate, User::getLastConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(userWithLastConnectionDate.getLogin(), true, formatDateTime(10_000_000_000L)),
        tuple(userWithoutLastConnectionDate.getLogin(), false, ""));
  }

  @Test
  public void return_all_fields_for_logged_user() {
    UserDto user = db.users().insertUser();
    db.users().updateLastConnectionDate(user, 10_000_000_000L);
    db.users().insertToken(user);
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    UserDto otherUser = db.users().insertUser();
    userIndexer.indexOnStartup(null);

    userSession.logIn(user);
    assertThat(ws.newRequest().setParam("q", user.getLogin())
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalIdentity, User::getExternalProvider,
          User::hasScmAccounts, User::hasAvatar, User::hasGroups, User::getTokensCount, User::hasLastConnectionDate)
        .containsExactlyInAnyOrder(
          tuple(user.getLogin(), user.getName(), user.getEmail(), user.getExternalLogin(), user.getExternalIdentityProvider(), true, true, true, 2, true));

    userSession.logIn(otherUser);
    assertThat(ws.newRequest().setParam("q", user.getLogin())
      .executeProtobuf(SearchWsResponse.class).getUsersList())
        .extracting(User::getLogin, User::getName, User::hasEmail, User::hasExternalIdentity, User::hasExternalProvider,
          User::hasScmAccounts, User::hasAvatar, User::hasGroups, User::hasTokensCount, User::hasLastConnectionDate)
        .containsExactlyInAnyOrder(
          tuple(user.getLogin(), user.getName(), false, false, true, true, true, false, false, false));
  }

  @Test
  public void search_with_paging() {
    userSession.logIn();
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));
    userIndexer.indexOnStartup(null);

    SearchWsResponse response = ws.newRequest()
      .setParam(Param.PAGE_SIZE, "5")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getUsersList())
      .extracting(User::getLogin)
      .containsExactly("user-0", "user-1", "user-2", "user-3", "user-4");
    assertThat(response.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(1, 5, 10);

    response = ws.newRequest()
      .setParam(Param.PAGE_SIZE, "5")
      .setParam(Param.PAGE, "2")
      .executeProtobuf(SearchWsResponse.class);
    assertThat(response.getUsersList())
      .extracting(User::getLogin)
      .containsExactly("user-5", "user-6", "user-7", "user-8", "user-9");
    assertThat(response.getPaging())
      .extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal)
      .containsExactly(2, 5, 10);
  }

  @Test
  public void return_empty_result_when_no_user() {
    userSession.logIn();

    SearchWsResponse response = ws.newRequest()
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getUsersList()).isEmpty();
    assertThat(response.getPaging().getTotal()).isZero();
  }

  @Test
  public void test_json_example() {
    UserDto fmallet = db.users().insertUser(u -> u.setLogin("fmallet").setName("Freddy Mallet").setEmail("f@m.com")
      .setLocal(true)
      .setScmAccounts(emptyList())
      .setExternalLogin("fmallet")
      .setExternalIdentityProvider("sonarqube"));
    UserDto simon = db.users().insertUser(u -> u.setLogin("sbrandhof").setName("Simon").setEmail("s.brandhof@company.tld")
      .setLocal(false)
      .setExternalLogin("sbrandhof@ldap.com")
      .setExternalIdentityProvider("LDAP")
      .setScmAccounts(asList("simon.brandhof", "s.brandhof@company.tld")));
    GroupDto sonarUsers = db.users().insertGroup(db.getDefaultOrganization(), "sonar-users");
    GroupDto sonarAdministrators = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    db.users().insertMember(sonarUsers, simon);
    db.users().insertMember(sonarUsers, fmallet);
    db.users().insertMember(sonarAdministrators, fmallet);
    db.users().insertToken(simon);
    db.users().insertToken(simon);
    db.users().insertToken(simon);
    db.users().insertToken(fmallet);
    userIndexer.indexOnStartup(null);
    userSession.logIn().setSystemAdministrator();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }

}
