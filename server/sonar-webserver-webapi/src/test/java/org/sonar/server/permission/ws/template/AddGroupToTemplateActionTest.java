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
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class AddGroupToTemplateActionTest extends BasePermissionWsTest<AddGroupToTemplateAction> {

  private PermissionTemplateDto template;
  private GroupDto group;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);

  @Override
  protected AddGroupToTemplateAction buildWsAction() {
    return new AddGroupToTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession, wsParameters);
  }

  @Before
  public void setUp() {
    template = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
    group = db.users().insertGroup(db.getDefaultOrganization(), "group-name");
  }

  @Test
  public void add_group_to_template() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest(group.getName(), template.getUuid(), CODEVIEWER);

    assertThat(getGroupNamesInTemplateAndPermission(template, CODEVIEWER)).containsExactly(group.getName());
  }

  @Test
  public void add_group_to_template_by_name() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();

    assertThat(getGroupNamesInTemplateAndPermission(template, CODEVIEWER)).containsExactly(group.getName());
  }

  @Test
  public void add_with_group_id() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_GROUP_ID, String.valueOf(group.getId()))
      .execute();

    assertThat(getGroupNamesInTemplateAndPermission(template, CODEVIEWER)).containsExactly(group.getName());
  }

  @Test
  public void does_not_add_a_group_twice() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest(group.getName(), template.getUuid(), ISSUE_ADMIN);
    newRequest(group.getName(), template.getUuid(), ISSUE_ADMIN);

    assertThat(getGroupNamesInTemplateAndPermission(template, ISSUE_ADMIN)).containsExactly(group.getName());
  }

  @Test
  public void add_anyone_group_to_template() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest(ANYONE, template.getUuid(), CODEVIEWER);

    assertThat(getGroupNamesInTemplateAndPermission(template, CODEVIEWER)).containsExactly(ANYONE);
  }

  @Test
  public void fail_if_add_anyone_group_to_admin_permission() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("It is not possible to add the '%s' permission to the group 'Anyone'", UserRole.ADMIN));

    newRequest(ANYONE, template.getUuid(), ADMIN);
  }

  @Test
  public void fail_if_not_a_project_permission() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(group.getName(), template.getUuid(), GlobalPermissions.PROVISIONING);
  }

  @Test
  public void fail_if_not_admin_of_default_organization() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(group.getName(), template.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_group_params_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest(null, template.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_permission_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(group.getName(), template.getUuid(), null);
  }

  @Test
  public void fail_if_template_uuid_and_name_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest(group.getName(), null, CODEVIEWER);
  }

  @Test
  public void fail_if_group_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with name 'unknown-group-name'");

    newRequest("unknown-group-name", template.getUuid(), CODEVIEWER);
  }

  @Test
  public void fail_if_template_key_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(group.getName(), "unknown-key", CODEVIEWER);
  }

  private void newRequest(@Nullable String groupName, @Nullable String templateKey, @Nullable String permission) {
    TestRequest request = newRequest();
    if (groupName != null) {
      request.setParam(PARAM_GROUP_NAME, groupName);
    }
    if (templateKey != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateKey);
    }
    if (permission != null) {
      request.setParam(PARAM_PERMISSION, permission);
    }

    request.execute();
  }

  private List<String> getGroupNamesInTemplateAndPermission(PermissionTemplateDto template, String permission) {
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(template.getOrganizationUuid()).setPermission(permission).build();
    return db.getDbClient().permissionTemplateDao()
      .selectGroupNamesByQueryAndTemplate(db.getSession(), query, template.getId());
  }
}
