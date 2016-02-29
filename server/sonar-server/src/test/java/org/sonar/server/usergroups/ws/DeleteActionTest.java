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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_NAME;


public class DeleteActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WsTester ws;
  private GroupDao groupDao;
  private UserGroupDao userGroupDao;
  private RoleDao roleDao;
  private PermissionTemplateDao permissionTemplateDao;

  private DbSession dbSession;
  private Long defaultGroupId;

  @Before
  public void setUp() {

    DbClient dbClient = db.getDbClient();
    groupDao = dbClient.groupDao();
    userGroupDao = dbClient.userGroupDao();
    roleDao = dbClient.roleDao();
    permissionTemplateDao = dbClient.permissionTemplateDao();
    dbSession = db.getSession();

    Settings settings = new Settings().setProperty(CoreProperties.CORE_DEFAULT_GROUP, CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    GroupDto defaultGroup = groupDao.insert(dbSession, new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE));
    defaultGroupId = defaultGroup.getId();
    dbSession.commit();

    ws = new WsTester(new UserGroupsWs(
      new DeleteAction(
        dbClient,
        new UserGroupFinder(dbClient),
        userSession,
        settings)));
  }

  @Test
  public void delete_simple() throws Exception {
    GroupDto group = groupDao.insert(dbSession, new GroupDto().setName("to-delete"));
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();
  }

  @Test
  public void delete_with_group_name() throws Exception {
    GroupDto group = groupDao.insert(dbSession, newGroupDto().setName("group_name"));
    assertThat(groupDao.selectById(dbSession, group.getId())).isNotNull();
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute().assertNoContent();

    assertThat(groupDao.selectById(dbSession, group.getId())).isNull();
  }

  @Test
  public void delete_with_members() throws Exception {
    GroupDto group = groupDao.insert(dbSession, new GroupDto().setName("to-delete"));
    userGroupDao.insert(dbSession, new UserGroupDto().setGroupId(group.getId()).setUserId(42L));
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(db.select("SELECT group_id FROM groups_users")).isEmpty();
  }

  @Test
  public void delete_with_permissions() throws Exception {
    GroupDto group = groupDao.insert(dbSession, new GroupDto().setName("to-delete"));
    roleDao.insertGroupRole(dbSession, new GroupRoleDto().setGroupId(group.getId()).setResourceId(42L).setRole(UserRole.ADMIN));
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(db.select("SELECT group_id FROM group_roles")).isEmpty();
  }

  @Test
  public void delete_with_permission_templates() throws Exception {
    GroupDto group = groupDao.insert(dbSession, new GroupDto().setName("to-delete"));
    permissionTemplateDao.insertGroupPermission(42L, group.getId(), UserRole.ADMIN);
    dbSession.commit();

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(db.select("SELECT group_id FROM perm_templates_groups")).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void not_found() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("id", String.valueOf(defaultGroupId + 1L))
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_delete_default_group() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("id", defaultGroupId.toString())
      .execute();
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "delete");
  }
}
