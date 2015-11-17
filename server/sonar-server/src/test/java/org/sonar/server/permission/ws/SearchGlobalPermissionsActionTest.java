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

import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsPermissions;

import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
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
  I18nRule i18n = new I18nRule();

  @Before
  public void setUp() {
    initI18nMessages();

    ws = new WsActionTester(new SearchGlobalPermissionsAction(db.getDbClient(), userSession, i18n));
    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void search() {
    GroupDto adminGroup = insertGroup(newGroupDto("sonar-admins", "Administrators"));
    GroupDto userGroup = insertGroup(newGroupDto("sonar-users", "Users"));
    insertGroupRole(newGroupRole(SCAN_EXECUTION, null));
    insertGroupRole(newGroupRole(SCAN_EXECUTION, userGroup.getId()));
    insertGroupRole(newGroupRole(SYSTEM_ADMIN, adminGroup.getId()));
    insertGroupRole(newGroupRole(PROVISIONING, userGroup.getId()));
    insertGroupRole(newGroupRole(DASHBOARD_SHARING, null));

    UserDto user = insertUser(newUserDto("user", "user-name"));
    UserDto adminUser = insertUser(newUserDto("admin", "admin-name"));
    insertUserRole(newUserRoleDto(PROVISIONING, user.getId()));
    insertUserRole(newUserRoleDto(QUALITY_PROFILE_ADMIN, user.getId()));
    insertUserRole(newUserRoleDto(QUALITY_PROFILE_ADMIN, adminUser.getId()));
    insertUserRole(newUserRoleDto(PREVIEW_EXECUTION, adminUser.getId()));
    insertUserRole(newUserRoleDto(PREVIEW_EXECUTION, user.getId()));

    db.getSession().commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_global_permissions-example.json"));
  }

  @Test
  public void protobuf_response() throws IOException {
    WsPermissions.WsSearchGlobalPermissionsResponse wsSearchGlobalPermissionsResponse = WsPermissions.WsSearchGlobalPermissionsResponse.parseFrom(
      ws.newRequest()
        .setMediaType(MediaTypes.PROTOBUF)
        .execute().getInputStream());
    System.out.println(wsSearchGlobalPermissionsResponse.getPermissionsList());
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

  private void initI18nMessages() {
    i18n.put("global_permissions.admin", "Administer System");
    i18n.put("global_permissions.admin.desc", "Ability to perform all administration functions for the instance: " +
      "global configuration and personalization of default dashboards.");
    i18n.put("global_permissions.profileadmin", "Administer Quality Profiles and Gates");
    i18n.put("global_permissions.profileadmin.desc", "Ability to perform any action on the quality profiles and gates.");
    i18n.put("global_permissions.shareDashboard", "Share Dashboards And Filters");
    i18n.put("global_permissions.shareDashboard.desc", "Ability to share dashboards, issue filters and measure filters.");
    i18n.put("global_permissions.scan", "Execute Analysis");
    i18n.put("global_permissions.scan.desc", "Ability to execute analyses, and to get all settings required to perform the analysis, " +
      "even the secured ones like the scm account password, the jira account password, and so on.");
    i18n.put("global_permissions.dryRunScan", "Execute Preview Analysis");
    i18n.put("global_permissions.dryRunScan.desc", "Ability to execute preview analysis (results are not pushed to the server). " +
      "This permission does not include the ability to access secured settings such as the scm account password, the jira account password, and so on. " +
      "This permission is required to execute preview analysis in Eclipse or via the Issues Report plugin.");
    i18n.put("global_permissions.provisioning", "Provision Projects");
    i18n.put("global_permissions.provisioning.desc", "Ability to initialize project structure before first analysis.");
  }

  private UserDto insertUser(UserDto user) {
    return db.getDbClient().userDao().insert(db.getSession(), user);
  }

  private void insertUserRole(UserRoleDto userRole) {
    db.getDbClient().roleDao().insertUserRole(db.getSession(), userRole);
  }

  private GroupDto insertGroup(GroupDto groupDto) {
    return db.getDbClient().groupDao().insert(db.getSession(), groupDto);
  }

  private void insertGroupRole(GroupRoleDto group) {
    db.getDbClient().roleDao().insertGroupRole(db.getSession(), group);
  }

  private static UserDto newUserDto(String login, String name) {
    return new UserDto().setLogin(login).setName(name).setActive(true);
  }

  private static GroupDto newGroupDto(String name, String description) {
    return new GroupDto().setName(name).setDescription(description);
  }

  private static GroupRoleDto newGroupRole(String role, @Nullable Long groupId) {
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
