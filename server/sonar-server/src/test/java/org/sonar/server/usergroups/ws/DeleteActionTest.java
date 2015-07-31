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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.db.user.GroupDao;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class DeleteActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WsTester ws;

  private GroupDao groupDao;

  private UserGroupDao userGroupDao;

  private RoleDao roleDao;

  private PermissionTemplateDao permissionTemplateDao;

  private DbSession session;

  private Long defaultGroupId;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    groupDao = new GroupDao(System2.INSTANCE);
    userGroupDao = new UserGroupDao();
    roleDao = new RoleDao();
    permissionTemplateDao = new PermissionTemplateDao(dbTester.myBatis(), System2.INSTANCE);

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(),
      groupDao, userGroupDao, roleDao, permissionTemplateDao);

    session = dbClient.openSession(false);

    Settings settings = new Settings().setProperty(CoreProperties.CORE_DEFAULT_GROUP, CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    GroupDto defaultGroup = groupDao.insert(session, new GroupDto().setName(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE));
    defaultGroupId = defaultGroup.getId();
    session.commit();

    ws = new WsTester(new UserGroupsWs(new DeleteAction(dbClient, userSession, settings)));
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void delete_simple() throws Exception {
    GroupDto group = groupDao.insert(session, new GroupDto().setName("to-delete"));
    session.commit();

    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();
  }

  @Test
  public void delete_with_members() throws Exception {
    GroupDto group = groupDao.insert(session, new GroupDto().setName("to-delete"));
    userGroupDao.insert(session, new UserGroupDto().setGroupId(group.getId()).setUserId(42L));
    session.commit();

    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(dbTester.select("SELECT group_id FROM groups_users")).isEmpty();
  }

  @Test
  public void delete_with_permissions() throws Exception {
    GroupDto group = groupDao.insert(session, new GroupDto().setName("to-delete"));
    roleDao.insertGroupRole(session, new GroupRoleDto().setGroupId(group.getId()).setResourceId(42L).setRole(UserRole.ADMIN));
    session.commit();

    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(dbTester.select("SELECT group_id FROM group_roles")).isEmpty();
  }

  @Test
  public void delete_with_permission_templates() throws Exception {
    GroupDto group = groupDao.insert(session, new GroupDto().setName("to-delete"));
    permissionTemplateDao.insertGroupPermission(42L, group.getId(), UserRole.ADMIN);
    session.commit();

    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(dbTester.select("SELECT group_id FROM perm_templates_groups")).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void not_found() throws Exception {
    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", "42")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_delete_default_group() throws Exception {
    loginAsAdmin();
    ws.newPostRequest("api/usergroups", "delete")
      .setParam("id", defaultGroupId.toString())
      .execute();
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }
}
