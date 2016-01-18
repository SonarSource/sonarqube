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
package org.sonar.server.user.ws;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

@Category(DbTests.class)
public class SearchActionTest {

  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  WsTester ws;
  UserIndex index;

  @Before
  public void setUp() {
    esTester.truncateIndices();
    index = new UserIndex(esTester.client());
    ws = new WsTester(new UsersWs(new SearchAction(index, dbClient, new UserJsonWriter(userSession))));
  }

  @Test
  public void search_empty() throws Exception {
    ws.newGetRequest("api/users", "search").execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void search_without_parameters() throws Exception {
    injectUsers(5);

    ws.newGetRequest("api/users", "search").execute().assertJson(getClass(), "five_users.json");
  }

  @Test
  public void search_with_query() throws Exception {
    injectUsers(5);

    ws.newGetRequest("api/users", "search").setParam("q", "user-1").execute().assertJson(getClass(), "user_one.json");
  }

  @Test
  public void search_with_paging() throws Exception {
    injectUsers(10);

    ws.newGetRequest("api/users", "search").setParam(Param.PAGE_SIZE, "5").execute().assertJson(getClass(), "page_one.json");
    ws.newGetRequest("api/users", "search").setParam(Param.PAGE_SIZE, "5").setParam(Param.PAGE, "2").execute().assertJson(getClass(), "page_two.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    injectUsers(1);

    assertThat(ws.newGetRequest("api/users", "search").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "scmAccounts").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .contains("scmAccounts")
      .doesNotContain("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "groups").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .doesNotContain("scmAccounts")
      .doesNotContain("groups");

    loginAsAdmin();

    assertThat(ws.newGetRequest("api/users", "search").execute().outputAsString())
      .contains("login")
      .contains("name")
      .contains("email")
      .contains("scmAccounts")
      .contains("groups");

    assertThat(ws.newGetRequest("api/users", "search").setParam(Param.FIELDS, "groups").execute().outputAsString())
      .contains("login")
      .doesNotContain("name")
      .doesNotContain("email")
      .doesNotContain("scmAccounts")
      .contains("groups");
  }

  @Test
  public void search_with_groups() throws Exception {
    List<UserDto> users = injectUsers(1);

    GroupDto group1 = dbClient.groupDao().insert(dbSession, new GroupDto().setName("sonar-users"));
    GroupDto group2 = dbClient.groupDao().insert(dbSession, new GroupDto().setName("sonar-admins"));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupId(group1.getId()).setUserId(users.get(0).getId()));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupId(group2.getId()).setUserId(users.get(0).getId()));
    dbSession.commit();

    loginAsAdmin();
    ws.newGetRequest("api/users", "search").execute().assertJson(getClass(), "user_with_groups.json");
  }

  private List<UserDto> injectUsers(int numberOfUsers) throws Exception {
    List<UserDto> userDtos = Lists.newArrayList();
    long createdAt = System.currentTimeMillis();
    UserDoc[] users = new UserDoc[numberOfUsers];
    for (int index = 0; index < numberOfUsers; index++) {
      String email = String.format("user-%d@mail.com", index);
      String login = String.format("user-%d", index);
      String name = String.format("User %d", index);
      List<String> scmAccounts = singletonList(String.format("user-%d", index));

      userDtos.add(dbClient.userDao().insert(dbSession, new UserDto()
        .setActive(true)
        .setCreatedAt(createdAt)
        .setEmail(email)
        .setLogin(login)
        .setName(name)
        .setScmAccounts(scmAccounts)
        .setUpdatedAt(createdAt)));

      users[index] = new UserDoc()
        .setActive(true)
        .setCreatedAt(createdAt)
        .setEmail(email)
        .setLogin(login)
        .setName(name)
        .setScmAccounts(scmAccounts)
        .setUpdatedAt(createdAt);

      for (int tokenIndex = 0; tokenIndex < index; tokenIndex++) {
        dbClient.userTokenDao().insert(dbSession, newUserToken()
          .setLogin(login)
          .setName(String.format("%s-%d", login, tokenIndex)));
      }
    }
    dbSession.commit();
    esTester.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, users);
    return userDtos;
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }
}
