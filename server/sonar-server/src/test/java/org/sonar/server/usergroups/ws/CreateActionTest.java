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

package org.sonar.server.usergroups.ws;

import java.net.HttpURLConnection;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.db.user.GroupDao;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class CreateActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsTester tester;

  private GroupDao groupDao;

  private DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    groupDao = new GroupDao(System2.INSTANCE);

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), groupDao);

    tester = new WsTester(new UserGroupsWs(new CreateAction(dbClient, userSession, new GroupUpdater(dbClient))));

    session = dbClient.openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void create_nominal() throws Exception {
    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"some-product-bu\"," +
        "    \"description\": \"Business Unit for Some Awesome Product\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");
  }

  @Test(expected = ForbiddenException.class)
  public void require_admin_permission() throws Exception {
    userSession.login("not-admin");
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void name_too_short() throws Exception {
    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", "")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void name_too_long() throws Exception {
    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", StringUtils.repeat("a", 255 + 1))
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void forbidden_name() throws Exception {
    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", "AnYoNe")
      .execute();
  }

  @Test
  public void non_unique_name() throws Exception {
    String groupName = "conflicting-name";
    groupDao.insert(session, new GroupDto()
      .setName(groupName));
    session.commit();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("already taken");

    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", groupName)
      .execute().assertStatus(HttpURLConnection.HTTP_CONFLICT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void description_too_long() throws Exception {
    loginAsAdmin();
    tester.newPostRequest("api/usergroups", "create")
      .setParam("name", "long-group-description-is-looooooooooooong")
      .setParam("description", StringUtils.repeat("a", 200 + 1))
      .execute();
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }
}
