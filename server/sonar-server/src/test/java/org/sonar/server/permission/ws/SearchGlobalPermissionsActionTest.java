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
package org.sonar.server.permission.ws;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsPermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchGlobalPermissionsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private WsActionTester ws;
  private I18nRule i18n = new I18nRule();

  @Before
  public void setUp() {
    initI18nMessages();

    ws = new WsActionTester(new SearchGlobalPermissionsAction(db.getDbClient(), userSession, i18n));
    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void search() {
    GroupDto adminGroup = db.users().insertGroup(newGroupDto("sonar-admins", "Administrators"));
    GroupDto userGroup = db.users().insertGroup(newGroupDto("sonar-users", "Users"));
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(userGroup, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(userGroup, PROVISIONING);
    db.users().insertPermissionOnGroup(adminGroup, SYSTEM_ADMIN);
    UserDto user = db.users().insertUser(newUserDto("user", "user-name"));
    UserDto adminUser = db.users().insertUser(newUserDto("admin", "admin-name"));
    db.users().insertPermissionOnUser(user, PROVISIONING);
    db.users().insertPermissionOnUser(user, QUALITY_PROFILE_ADMIN);
    db.users().insertPermissionOnUser(adminUser, QUALITY_PROFILE_ADMIN);
    db.users().insertPermissionOnUser(user, QUALITY_GATE_ADMIN);
    db.users().insertPermissionOnUser(adminUser, QUALITY_GATE_ADMIN);

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_global_permissions-example.json"));
  }

  @Test
  public void protobuf_response() throws IOException {
    WsPermissions.WsSearchGlobalPermissionsResponse result = WsPermissions.WsSearchGlobalPermissionsResponse.parseFrom(
      ws.newRequest()
        .setMediaType(MediaTypes.PROTOBUF)
        .execute().getInputStream());

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.login("login");

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().execute();
  }

  private void initI18nMessages() {
    i18n.put("global_permissions.admin", "Administer System");
    i18n.put("global_permissions.admin.desc", "Ability to perform all administration functions for the instance: " +
      "global configuration and personalization of default dashboards.");
    i18n.put("global_permissions.profileadmin", "Administer Quality Profiles");
    i18n.put("global_permissions.profileadmin.desc", "Ability to perform any action on the quality profiles.");
    i18n.put("global_permissions.gateadmin", "Administer Quality Gates");
    i18n.put("global_permissions.gateadmin.desc", "Ability to perform any action on the quality gates.");
    i18n.put("global_permissions.scan", "Execute Analysis");
    i18n.put("global_permissions.scan.desc", "Ability to execute analyses, and to get all settings required to perform the analysis, " +
      "even the secured ones like the scm account password, the jira account password, and so on.");
    i18n.put("global_permissions.provisioning", "Create Projects");
    i18n.put("global_permissions.provisioning.desc", "Ability to initialize project structure before first analysis.");
  }

  private static UserDto newUserDto(String login, String name) {
    return UserTesting.newUserDto().setLogin(login).setName(name).setActive(true);
  }

  private static GroupDto newGroupDto(String name, String description) {
    return GroupTesting.newGroupDto().setName(name).setDescription(description);
  }
}
