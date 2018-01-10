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
package org.sonar.server.permission.ws.template;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveGroupFromTemplateActionTest extends BasePermissionWsTest<RemoveGroupFromTemplateAction> {

  private static final String PERMISSION = CODEVIEWER;

  private GroupDto group;
  private PermissionTemplateDto template;

  @Override
  protected RemoveGroupFromTemplateAction buildWsAction() {
    return new RemoveGroupFromTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession);
  }

  @Before
  public void setUp() {
    loginAsAdmin(db.getDefaultOrganization());

    group = db.users().insertGroup(db.getDefaultOrganization(), "group-name");
    template = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
    addGroupToTemplate(template, group.getId(), PERMISSION);
  }

  @Test
  public void remove_group_from_template() throws Exception {
    newRequest(group.getName(), template.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(template, PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_from_template_by_name_case_insensitive() {
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, PERMISSION)
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();

    assertThat(getGroupNamesInTemplateAndPermission(template, PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_with_group_id() {
    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(PARAM_PERMISSION, PERMISSION)
      .setParam(PARAM_GROUP_ID, String.valueOf(group.getId()))
      .execute();

    assertThat(getGroupNamesInTemplateAndPermission(template, PERMISSION)).isEmpty();
  }

  @Test
  public void remove_group_twice_without_error() throws Exception {
    newRequest(group.getName(), template.getUuid(), PERMISSION);
    newRequest(group.getName(), template.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(template, PERMISSION)).isEmpty();
  }

  @Test
  public void remove_anyone_group_from_template() throws Exception {
    addGroupToTemplate(template, null, PERMISSION);

    newRequest(ANYONE, template.getUuid(), PERMISSION);

    assertThat(getGroupNamesInTemplateAndPermission(template, PERMISSION)).containsExactly(group.getName());
  }

  @Test
  public void fail_if_not_a_project_permission() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(group.getName(), template.getUuid(), GlobalPermissions.PROVISIONING);
  }

  @Test
  public void fail_if_insufficient_privileges() throws Exception {
    userSession.logIn().addPermission(SCAN, db.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);

    newRequest(group.getName(), template.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(group.getName(), template.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_group_params_missing() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest(null, template.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_permission_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(group.getName(), template.getUuid(), null);
  }

  @Test
  public void fail_if_template_missing() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest(group.getName(), null, PERMISSION);
  }

  @Test
  public void fail_if_group_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with name 'unknown-group-name'");

    newRequest("unknown-group-name", template.getUuid(), PERMISSION);
  }

  @Test
  public void fail_if_template_key_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(group.getName(), "unknown-key", PERMISSION);
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

  private void addGroupToTemplate(PermissionTemplateDto template, @Nullable Integer groupId, String permission) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), template.getId(), groupId, permission);
    db.commit();
  }

  private List<String> getGroupNamesInTemplateAndPermission(PermissionTemplateDto template, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().setOrganizationUuid(template.getOrganizationUuid()).setPermission(permission).build();
    return db.getDbClient().permissionTemplateDao()
      .selectGroupNamesByQueryAndTemplate(db.getSession(), permissionQuery, template.getId());
  }
}
