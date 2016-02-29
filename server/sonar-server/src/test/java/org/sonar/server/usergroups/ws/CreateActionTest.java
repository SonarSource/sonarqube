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
package org.sonar.server.usergroups.ws;

import java.net.HttpURLConnection;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;


public class CreateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsTester ws;
  private GroupDao groupDao;
  private DbSession dbSession;

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    groupDao = dbClient.groupDao();
    dbSession = db.getSession();

    ws = new WsTester(new UserGroupsWs(new CreateAction(dbClient, userSession, new UserGroupUpdater(dbClient))));
  }

  @Test
  public void create_nominal() throws Exception {
    loginAsAdmin();
    newRequest()
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
    newRequest()
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void name_too_short() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("name", "")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void name_too_long() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("name", StringUtils.repeat("a", 255 + 1))
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void forbidden_name() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("name", "AnYoNe")
      .execute();
  }

  @Test
  public void non_unique_name() throws Exception {
    String groupName = "conflicting-name";
    groupDao.insert(dbSession, new GroupDto()
      .setName(groupName));
    db.commit();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("already taken");

    loginAsAdmin();
    newRequest()
      .setParam("name", groupName)
      .execute().assertStatus(HttpURLConnection.HTTP_CONFLICT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void description_too_long() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("name", "long-group-description-is-looooooooooooong")
      .setParam("description", StringUtils.repeat("a", 200 + 1))
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "create");
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }
}
