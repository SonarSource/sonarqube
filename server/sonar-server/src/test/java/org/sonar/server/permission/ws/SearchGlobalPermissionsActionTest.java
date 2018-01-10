/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.sonarqube.ws.Permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
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
  public void search_in_organization() {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org);
    GroupDto adminGroup = db.users().insertGroup(newGroup(org, "sonar-admins", "Administrators"));
    GroupDto userGroup = db.users().insertGroup(newGroup(org, "sonar-users", "Users"));
    db.users().insertPermissionOnAnyone(org, SCAN);
    db.users().insertPermissionOnGroup(userGroup, SCAN);
    db.users().insertPermissionOnGroup(userGroup, PROVISIONING);
    db.users().insertPermissionOnGroup(adminGroup, ADMINISTER);
    UserDto user = db.users().insertUser(newUserDto("user", "user-name"));
    UserDto adminUser = db.users().insertUser(newUserDto("admin", "admin-name"));
    db.organizations().addMember(org, user);
    db.organizations().addMember(org, adminUser);
    db.users().insertPermissionOnUser(org, user, PROVISION_PROJECTS);
    db.users().insertPermissionOnUser(org, user, ADMINISTER_QUALITY_PROFILES);
    db.users().insertPermissionOnUser(org, adminUser, ADMINISTER_QUALITY_PROFILES);
    db.users().insertPermissionOnUser(org, user, ADMINISTER_QUALITY_GATES);
    db.users().insertPermissionOnUser(org, adminUser, ADMINISTER_QUALITY_GATES);

    // to be excluded, permission on another organization (the default one)
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), adminUser, ADMINISTER_QUALITY_GATES);

    String result = newRequest()
      .setParam("organization", org.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_global_permissions-example.json"));
  }

  @Test
  public void search_in_default_organization_by_default() {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org, db.getDefaultOrganization());

    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SCAN);
    db.organizations().addMember(db.getDefaultOrganization(), user);

    // to be ignored, by default organization is used when searching for permissions
    db.users().insertPermissionOnUser(org, user, ADMINISTER_QUALITY_GATES);
    db.organizations().addMember(org, user);

    Permissions.WsSearchGlobalPermissionsResponse result = newRequest()
      .executeProtobuf(Permissions.WsSearchGlobalPermissionsResponse.class);

    assertThat(result.getPermissionsCount()).isEqualTo(GlobalPermissions.ALL.size());
    for (Permissions.Permission permission : result.getPermissionsList()) {
      if (permission.getKey().equals(SCAN_EXECUTION)) {
        assertThat(permission.getUsersCount()).isEqualTo(1);
      } else {
        assertThat(permission.getUsersCount()).isEqualTo(0);
      }
    }
  }

  @Test
  public void supports_protobuf_response() {
    loginAsAdmin(db.getDefaultOrganization());

    Permissions.WsSearchGlobalPermissionsResponse result = newRequest()
      .executeProtobuf(Permissions.WsSearchGlobalPermissionsResponse.class);

    assertThat(result).isNotNull();
  }

  @Test
  public void fail_if_not_admin_of_default_organization() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .execute();
  }

  @Test
  public void fail_if_not_admin_of_specified_organization() {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam("organization", org.getKey())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest().execute();
  }

  @Test
  public void fail_if_organization_does_not_exist() {
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
