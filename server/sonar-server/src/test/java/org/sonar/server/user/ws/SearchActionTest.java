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
package org.sonar.server.user.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.ws.AvatarResolverImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private UserIndex index = new UserIndex(esTester.client(), system2);
  private UserIndexer userIndexer = new UserIndexer(dbClient, esTester.client());
  private WsTester ws = new WsTester(new UsersWs(new SearchAction(userSession, index, dbClient, new AvatarResolverImpl())));

  @Test
  public void test_json_example() throws Exception {
    UserDto fmallet = db.users().insertUser(newUserDto("fmallet", "Freddy Mallet", "f@m.com")
      .setActive(true)
      .setLocal(true)
      .setScmAccounts(emptyList())
      .setExternalIdentity("fmallet")
      .setExternalIdentityProvider("sonarqube"));
    UserDto simon = db.users().insertUser(newUserDto("sbrandhof", "Simon", "s.brandhof@company.tld")
      .setActive(true)
      .setLocal(false)
      .setExternalIdentity("sbrandhof@ldap.com")
      .setExternalIdentityProvider("LDAP")
      .setScmAccounts(newArrayList("simon.brandhof", "s.brandhof@company.tld")));
    GroupDto sonarUsers = db.users().insertGroup(newGroupDto().setName("sonar-users"));
    GroupDto sonarAdministrators = db.users().insertGroup(newGroupDto().setName("sonar-administrators"));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(simon.getId()).setGroupId(sonarUsers.getId()));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(fmallet.getId()).setGroupId(sonarUsers.getId()));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(fmallet.getId()).setGroupId(sonarAdministrators.getId()));

    for (int i = 0; i < 3; i++) {
      dbClient.userTokenDao().insert(dbSession, newUserToken().setLogin(simon.getLogin()));
    }
    dbClient.userTokenDao().insert(dbSession, newUserToken().setLogin(fmallet.getLogin()));
    db.commit();
    userIndexer.indexOnStartup(null);
    loginAsSystemAdministrator();

    String response = ws.newGetRequest("api/users", "search").execute().outputAsString();

    assertJson(response).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void search_empty() throws Exception {
    loginAsSimpleUser();
    ws.newGetRequest("api/users", "search").execute().assertJson("{\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 1,\n" +
      "    \"pageSize\": 50,\n" +
      "    \"total\": 0\n" +
      "  },\n" +
      "  \"users\": []\n" +
      "}");
  }

  @Test
  public void search_without_parameters() throws Exception {
    loginAsSimpleUser();
    injectUsers(5);

    ws.newGetRequest("api/users", "search").execute().assertJson(getClass(), "five_users.json");
  }

  @Test
  public void search_with_query() throws Exception {
    loginAsSimpleUser();
    injectUsers(5);
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));
    userIndexer.indexOnStartup(null);

    ws.newGetRequest("api/users", "search").setParam("q", "user-%_%-").execute().assertJson(getClass(), "user_one.json");
    ws.newGetRequest("api/users", "search").setParam("q", "user@MAIL.com").execute().assertJson(getClass(), "user_one.json");
    ws.newGetRequest("api/users", "search").setParam("q", "user-name").execute().assertJson(getClass(), "user_one.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    loginAsSimpleUser();
    injectUsers(10);

    ws.newGetRequest("api/users", "search").setParam(Param.PAGE_SIZE, "5").execute().assertJson(getClass(), "page_one.json");
    ws.newGetRequest("api/users", "search").setParam(Param.PAGE_SIZE, "5").setParam(Param.PAGE, "2").execute().assertJson(getClass(), "page_two.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    loginAsSimpleUser();
    injectUsers(1);

    assertThat(ws.newGetRequest("api/users", "search").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("avatar")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("avatar")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "scmAccounts").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("avatar")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "groups").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("avatar")
      .doesNotContain("scmAccounts")
      .doesNotContain("groups");

    loginAsSystemAdministrator();

    assertThat(ws.newGetRequest("api/users", "search").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("avatar")
      .contains("scmAccounts")
      .contains("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "groups").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .doesNotContain("avatar")
      .doesNotContain("scmAccounts")
      .contains("groups");
  }

  @Test
  public void search_with_groups() throws Exception {
    loginAsSystemAdministrator();
    injectUsers(1);

    ws.newGetRequest("api/users", "search").execute().assertJson(getClass(), "user_with_groups.json");
  }

  @Test
  public void does_not_return_email_when_not_when_system_administer() throws Exception {
    loginAsSimpleUser();
    insertUser(user -> user.setLogin("john").setName("John").setEmail("john@email.com"));

    ws.newGetRequest("api/users", "search").execute().assertJson(
      "{" +
        "  \"users\": [" +
        "    {" +
        "      \"login\": \"john\"," +
        "      \"name\": \"John\"," +
        "      \"avatar\": \"41193cdbffbf06be0cdf231b28c54b18\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void return_email_and_avatar_when_system_administer() throws Exception {
    loginAsSystemAdministrator();
    insertUser(user -> user.setLogin("john").setName("John").setEmail("john@email.com"));

    ws.newGetRequest("api/users", "search").execute().assertJson(
      "{" +
        "  \"users\": [" +
        "    {" +
        "      \"login\": \"john\"," +
        "      \"name\": \"John\"," +
        "      \"email\": \"john@email.com\"," +
        "      \"avatar\": \"41193cdbffbf06be0cdf231b28c54b18\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void does_not_fail_when_user_has_no_email() throws Exception {
    loginAsSystemAdministrator();
    insertUser(user -> user.setLogin("john").setName("John").setEmail(null));

    ws.newGetRequest("api/users", "search").execute().assertJson(
      "{" +
        "  \"users\": [" +
        "    {" +
        "      \"login\": \"john\"," +
        "      \"name\": \"John\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void only_return_login_and_name_when_not_logged() throws Exception {
    userSession.anonymous();

    dbClient.userDao().insert(dbSession, UserTesting.newUserDto("john", "John", "john@email.com"));
    dbSession.commit();
    userIndexer.indexOnStartup(null);

    ws.newGetRequest("api/users", "search").execute().assertJson(
      "{" +
        "  \"users\": [" +
        "    {" +
        "      \"login\": \"john\"," +
        "      \"name\": \"John\"" +
        "    }" +
        "  ]" +
        "}");
  }

  private List<UserDto> injectUsers(int numberOfUsers) {
    List<UserDto> userDtos = new ArrayList<>();
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("sonar-users"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("sonar-admins"));
    for (int index = 0; index < numberOfUsers; index++) {
      String email = String.format("user-%d@mail.com", index);
      String login = String.format("user-%d", index);
      String name = String.format("User %d", index);
      List<String> scmAccounts = singletonList(String.format("user-%d", index));

      UserDto userDto = dbClient.userDao().insert(dbSession, new UserDto()
        .setActive(true)
        .setEmail(email)
        .setLogin(login)
        .setName(name)
        .setScmAccounts(scmAccounts)
        .setLocal(true)
        .setExternalIdentity(login)
        .setExternalIdentityProvider("sonarqube"));
      userDtos.add(userDto);

      for (int tokenIndex = 0; tokenIndex < index; tokenIndex++) {
        dbClient.userTokenDao().insert(dbSession, newUserToken()
          .setLogin(login)
          .setName(String.format("%s-%d", login, tokenIndex)));
      }
      db.users().insertMember(group1, userDto);
      db.users().insertMember(group2, userDto);
    }
    dbSession.commit();
    userIndexer.indexOnStartup(null);
    return userDtos;
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto) {
    UserDto user = db.users().insertUser(populateUserDto);
    userIndexer.indexOnStartup(null);
    return user;
  }

  private void loginAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void loginAsSimpleUser() {
    userSession.logIn();
  }

}
