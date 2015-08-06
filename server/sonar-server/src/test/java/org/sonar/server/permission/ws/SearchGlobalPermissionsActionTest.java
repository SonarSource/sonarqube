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

package org.sonar.server.permission.ws;

import com.google.common.io.Resources;
import java.util.Locale;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.PREVIEW_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class SearchGlobalPermissionsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  WsActionTester ws;

  @Before
  public void setUp() {
    I18n i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), eq("global_permissions.admin"), any(String.class))).thenReturn("admin");
    when(i18n.message(any(Locale.class), eq("global_permissions.admin.desc"), any(String.class))).thenReturn("admin-description");
    when(i18n.message(any(Locale.class), eq("global_permissions.profileadmin"), any(String.class))).thenReturn("profileadmin");
    when(i18n.message(any(Locale.class), eq("global_permissions.profileadmin.desc"), any(String.class))).thenReturn("profileadmine-description");
    when(i18n.message(any(Locale.class), eq("global_permissions.shareDashboard"), any(String.class))).thenReturn("shareDashboard");
    when(i18n.message(any(Locale.class), eq("global_permissions.shareDashboard.desc"), any(String.class))).thenReturn("shareDashboard-description");
    when(i18n.message(any(Locale.class), eq("global_permissions.scan"), any(String.class))).thenReturn("scan");
    when(i18n.message(any(Locale.class), eq("global_permissions.scan.desc"), any(String.class))).thenReturn("scan-description");
    when(i18n.message(any(Locale.class), eq("global_permissions.dryRunScan"), any(String.class))).thenReturn("dryRunScan");
    when(i18n.message(any(Locale.class), eq("global_permissions.dryRunScan.desc"), any(String.class))).thenReturn("dryRunScan-description");
    when(i18n.message(any(Locale.class), eq("global_permissions.provisioning"), any(String.class))).thenReturn("provisioning");
    when(i18n.message(any(Locale.class), eq("global_permissions.provisioning.desc"), any(String.class))).thenReturn("provisioning-description");

    ws = new WsActionTester(new SearchGlobalPermissionsAction(db.getDbClient(), userSession, i18n));
    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void search() {
    RoleDao roleDao = db.getDbClient().roleDao();
    GroupDao groupDao = db.getDbClient().groupDao();

    GroupDto adminGroup = groupDao.insert(db.getSession(), new GroupDto().setName("sonar-admins").setDescription("Administrators"));
    GroupDto userGroup = groupDao.insert(db.getSession(), new GroupDto().setName("sonar-users").setDescription("Users"));
    roleDao.insertGroupRole(db.getSession(), newGroupRoleDto(SCAN_EXECUTION, userGroup.getId()));
    roleDao.insertGroupRole(db.getSession(), newGroupRoleDto(SYSTEM_ADMIN, adminGroup.getId()));
    roleDao.insertGroupRole(db.getSession(), newGroupRoleDto(SCAN_EXECUTION, null));

    UserDto user = db.getDbClient().userDao().insert(db.getSession(), new UserDto().setLogin("user").setName("user-name").setActive(true));
    UserDto adminUser = db.getDbClient().userDao().insert(db.getSession(), new UserDto().setLogin("admin").setName("admin-name").setActive(true));
    roleDao.insertUserRole(db.getSession(), newUserRoleDto(PROVISIONING, user.getId()));
    roleDao.insertUserRole(db.getSession(), newUserRoleDto(QUALITY_PROFILE_ADMIN, adminUser.getId()));
    roleDao.insertUserRole(db.getSession(), newUserRoleDto(PREVIEW_EXECUTION, adminUser.getId()));
    roleDao.insertUserRole(db.getSession(), newUserRoleDto(PREVIEW_EXECUTION, user.getId()));

    db.getSession().commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(Resources.getResource(getClass(), "SearchGlobalPermissionsActionTest/permissions.json"));
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest().execute();
  }

  private static GroupRoleDto newGroupRoleDto(String role, @Nullable Long groupId) {
    GroupRoleDto groupRole = new GroupRoleDto().setRole(role);
    if (groupId != null) {
      groupRole.setGroupId(groupId);
    }

    return groupRole;
  }

  private static UserRoleDto newUserRoleDto(String role, long userId) {
    return new UserRoleDto()
      .setRole(role)
      .setUserId(userId);
  }
}
