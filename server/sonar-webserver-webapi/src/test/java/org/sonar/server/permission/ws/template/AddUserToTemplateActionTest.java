/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserToTemplateActionTest extends BasePermissionWsTest<AddUserToTemplateAction> {

  private UserDto user;
  private PermissionTemplateDto permissionTemplate;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);

  @Override
  protected AddUserToTemplateAction buildWsAction() {
    return new AddUserToTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession, wsParameters);
  }

  @Before
  public void setUp() {
    user = db.users().insertUser("user-login");
    db.organizations().addMember(db.getDefaultOrganization(), user);
    permissionTemplate = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
  }

  @Test
  public void add_user_to_template() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest(user.getLogin(), permissionTemplate.getUuid(), CODEVIEWER);

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate, CODEVIEWER)).containsExactly(user.getLogin());
  }

  @Test
  public void add_user_to_template_by_name() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .execute();

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate, CODEVIEWER)).containsExactly(user.getLogin());
  }

  @Test
  public void add_user_to_template_by_name_and_organization() {
    OrganizationDto organizationDto = db.organizations().insert();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate(organizationDto);
    addUserAsMemberOfOrganization(organizationDto);
    loginAsAdmin(organizationDto);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .setParam(PARAM_ORGANIZATION, organizationDto.getKey())
      .execute();

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate, CODEVIEWER)).containsExactly(user.getLogin());
  }

  @Test
  public void does_not_add_a_user_twice() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest(user.getLogin(), permissionTemplate.getUuid(), ISSUE_ADMIN);
    newRequest(user.getLogin(), permissionTemplate.getUuid(), ISSUE_ADMIN);

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate, ISSUE_ADMIN)).containsExactly(user.getLogin());
  }

  @Test
  public void fail_if_not_a_project_permission() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(user.getLogin(), permissionTemplate.getUuid(), GlobalPermissions.PROVISIONING);
  }

  @Test
  public void fail_if_not_admin_of_default_organization() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);

    newRequest(user.getLogin(), permissionTemplate.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_user_missing() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, permissionTemplate.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_permission_missing() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(user.getLogin(), permissionTemplate.getUuid(), null);
  }

  @Test
  public void fail_if_template_uuid_and_name_are_missing() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest(user.getLogin(), null, CODEVIEWER);
  }

  @Test
  public void fail_if_user_does_not_exist() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' is not found");

    newRequest("unknown-login", permissionTemplate.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_template_key_does_not_exist() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(user.getLogin(), "unknown-key", CODEVIEWER);
  }

  @Test
  public void fail_if_organization_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'Unknown'");

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .setParam(PARAM_ORGANIZATION, "Unknown")
      .execute();  }

  @Test
  public void fail_to_add_permission_when_user_is_not_member_of_given_organization() {
    // User is not member of given organization
    OrganizationDto otherOrganization = db.organizations().insert();
    addUserAsMemberOfOrganization(otherOrganization);
    OrganizationDto organization = db.organizations().insert(organizationDto -> organizationDto.setKey("Organization key"));
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate(organization);
    loginAsAdmin(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User 'user-login' is not member of organization 'Organization key'");

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  private void newRequest(@Nullable String userLogin, @Nullable String templateKey, @Nullable String permission) {
    TestRequest request = newRequest();
    if (userLogin != null) {
      request.setParam(PARAM_USER_LOGIN, userLogin);
    }
    if (templateKey != null) {
      request.setParam(org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID, templateKey);
    }
    if (permission != null) {
      request.setParam(PARAM_PERMISSION, permission);
    }

    request.execute();
  }

  private List<String> getLoginsInTemplateAndPermission(PermissionTemplateDto template, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().setOrganizationUuid(template.getOrganizationUuid()).setPermission(permission).build();
    return db.getDbClient().permissionTemplateDao()
      .selectUserLoginsByQueryAndTemplate(db.getSession(), permissionQuery, template.getId());
  }

  private void addUserAsMemberOfOrganization(OrganizationDto organization) {
    db.organizations().addMember(organization, user);
  }
}
