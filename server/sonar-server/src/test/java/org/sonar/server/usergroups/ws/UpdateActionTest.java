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
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;


public class UpdateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsTester ws;
  private DbSession dbSession;
  private GroupDao groupDao;
  private UserGroupDao userGroupDao;

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    dbSession = db.getSession();
    groupDao = dbClient.groupDao();
    userGroupDao = dbClient.userGroupDao();

    ws = new WsTester(new UserGroupsWs(new UpdateAction(dbClient, userSession, new UserGroupUpdater(dbClient))));
  }

  @Test
  public void update_nominal() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    userGroupDao.insert(dbSession, new UserGroupDto().setGroupId(existingGroup.getId()).setUserId(42L));

    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", "new-name")
      .setParam("description", "New Description")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"new-name\"," +
        "    \"description\": \"New Description\"," +
        "    \"membersCount\": 1" +
        "  }" +
        "}");
  }

  @Test
  public void update_only_name() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", "new-name")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"new-name\"," +
        "    \"description\": \"Old Description\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");
  }

  @Test
  public void update_only_description() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("description", "New Description")
      .execute().assertJson("{" +
        "  \"group\": {" +
        "    \"name\": \"old-name\"," +
        "    \"description\": \"New Description\"," +
        "    \"membersCount\": 0" +
        "  }" +
        "}");
  }

  @Test
  public void require_admin_permission() throws Exception {
    expectedException.expect(ForbiddenException.class);

    userSession.login("not-admin");
    newRequest()
      .setParam("id", "42")
      .setParam("name", "some-product-bu")
      .setParam("description", "Business Unit for Some Awesome Product")
      .execute();
  }

  @Test
  public void name_too_short() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", "")
      .execute();
  }

  @Test
  public void name_too_long() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", StringUtils.repeat("a", 255 + 1))
      .execute();
  }

  @Test
  public void forbidden_name() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", "AnYoNe")
      .execute();
  }

  @Test
  public void non_unique_name() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    String groupName = "conflicting-name";
    groupDao.insert(dbSession, new GroupDto()
      .setName(groupName));
    dbSession.commit();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("already taken");

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", groupName)
      .execute().assertStatus(HttpURLConnection.HTTP_CONFLICT);
  }

  @Test
  public void description_too_long() throws Exception {
    GroupDto existingGroup = groupDao.insert(dbSession, new GroupDto().setName("old-name").setDescription("Old Description"));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", existingGroup.getId().toString())
      .setParam("name", "long-group-description-is-looooooooooooong")
      .setParam("description", StringUtils.repeat("a", 200 + 1))
      .execute();
  }

  @Test
  public void unknown_group() throws Exception {
    expectedException.expect(NotFoundException.class);

    loginAsAdmin();
    newRequest()
      .setParam("id", "42")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "update");
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }
}
