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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsPermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchGlobalPermissionsActionTest extends BasePermissionWsTest<SearchGlobalPermissionsAction> {

  private I18nRule i18n = new I18nRule();

  @Override
  protected SearchGlobalPermissionsAction buildWsAction() {
    return new SearchGlobalPermissionsAction(db.getDbClient(), userSession, i18n, newPermissionWsSupport());
  }

  @Before
  public void setUp() {
    initI18nMessages();
  }

  @Test
  public void search_in_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org);
    GroupDto adminGroup = db.users().insertGroup(newGroup(org, "sonar-admins", "Administrators"));
    GroupDto userGroup = db.users().insertGroup(newGroup(org, "sonar-users", "Users"));
    db.users().insertPermissionOnAnyone(org, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(userGroup, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(userGroup, PROVISIONING);
    db.users().insertPermissionOnGroup(adminGroup, SYSTEM_ADMIN);
    UserDto user = db.users().insertUser(newUserDto("user", "user-name"));
    UserDto adminUser = db.users().insertUser(newUserDto("admin", "admin-name"));
    db.users().insertPermissionOnUser(org, user, PROVISIONING);
    db.users().insertPermissionOnUser(org, user, QUALITY_PROFILE_ADMIN);
    db.users().insertPermissionOnUser(org, adminUser, QUALITY_PROFILE_ADMIN);
    db.users().insertPermissionOnUser(org, user, QUALITY_GATE_ADMIN);
    db.users().insertPermissionOnUser(org, adminUser, QUALITY_GATE_ADMIN);

    // to be excluded, permission on another organization (the default one)
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), adminUser, QUALITY_GATE_ADMIN);

    String result = newRequest()
      .setParam("organization", org.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_global_permissions-example.json"));
  }

  @Test
  public void search_in_default_organization_by_default() throws Exception {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org, db.getDefaultOrganization());

    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SCAN_EXECUTION);

    // to be ignored, by default organization is used when searching for permissions
    db.users().insertPermissionOnUser(org, user, QUALITY_GATE_ADMIN);

    WsPermissions.WsSearchGlobalPermissionsResponse result = WsPermissions.WsSearchGlobalPermissionsResponse.parseFrom(
      newRequest()
        .setMediaType(MediaTypes.PROTOBUF)
        .execute()
        .getInputStream());

    assertThat(result.getPermissionsCount()).isEqualTo(GlobalPermissions.ALL.size());
    for (WsPermissions.Permission permission : result.getPermissionsList()) {
      if (permission.getKey().equals(SCAN_EXECUTION)) {
        assertThat(permission.getUsersCount()).isEqualTo(1);
      } else {
        assertThat(permission.getUsersCount()).isEqualTo(0);
      }
    }
  }

  @Test
  public void supports_protobuf_response() throws Exception {
    loginAsAdminOnDefaultOrganization();

    WsPermissions.WsSearchGlobalPermissionsResponse result = WsPermissions.WsSearchGlobalPermissionsResponse.parseFrom(
      newRequest()
        .setMediaType(MediaTypes.PROTOBUF)
        .execute()
        .getInputStream());

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_if_not_admin_of_default_organization() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .execute();
  }

  @Test
  public void fail_if_not_admin_of_specified_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("organization", org.getKey())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest().execute();
  }

  @Test
  public void fail_if_organization_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam("organization", "does_not_exist")
      .execute();
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

  private static GroupDto newGroup(OrganizationDto org, String name, String description) {
    return GroupTesting.newGroupDto().setName(name).setDescription(description).setOrganizationUuid(org.getUuid());
  }
}
