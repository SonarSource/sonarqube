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
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.permission.ws.RequestValidator;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserFromTemplateActionTest extends BasePermissionWsTest<RemoveUserFromTemplateAction> {

  private static final String DEFAULT_PERMISSION = CODEVIEWER;

  private UserDto user;
  private PermissionTemplateDto template;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);
  private RequestValidator requestValidator = new RequestValidator(permissionService);


  @Override
  protected RemoveUserFromTemplateAction buildWsAction() {
    return new RemoveUserFromTemplateAction(db.getDbClient(), newPermissionWsSupport(), userSession, wsParameters, requestValidator);
  }

  @Before
  public void setUp() {
    user = db.users().insertUser("user-login");
    db.organizations().addMember(db.getDefaultOrganization(), user);
    template = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
    addUserToTemplate(user, template, DEFAULT_PERMISSION);
  }

  @Test
  public void remove_user_from_template() {
    loginAsAdmin(db.getDefaultOrganization());
    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);

    assertThat(getLoginsInTemplateAndPermission(template, DEFAULT_PERMISSION)).isEmpty();
  }

  @Test
  public void remove_user_from_template_by_name_case_insensitive() {
    loginAsAdmin(db.getDefaultOrganization());
    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, DEFAULT_PERMISSION)
      .setParam(PARAM_TEMPLATE_NAME, template.getName().toUpperCase())
      .execute();

    assertThat(getLoginsInTemplateAndPermission(template, DEFAULT_PERMISSION)).isEmpty();
  }

  @Test
  public void remove_user_from_template_twice_without_failing() {
    loginAsAdmin(db.getDefaultOrganization());
    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);
    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);

    assertThat(getLoginsInTemplateAndPermission(template, DEFAULT_PERMISSION)).isEmpty();
  }

  @Test
  public void keep_user_permission_not_removed() {
    addUserToTemplate(user, template, ISSUE_ADMIN);

    loginAsAdmin(db.getDefaultOrganization());
    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);

    assertThat(getLoginsInTemplateAndPermission(template, DEFAULT_PERMISSION)).isEmpty();
    assertThat(getLoginsInTemplateAndPermission(template, ISSUE_ADMIN)).containsExactly(user.getLogin());
  }

  @Test
  public void keep_other_users_when_one_user_removed() {
    UserDto newUser = db.users().insertUser("new-login");
    db.organizations().addMember(db.getDefaultOrganization(), newUser);
    addUserToTemplate(newUser, template, DEFAULT_PERMISSION);

    loginAsAdmin(db.getDefaultOrganization());
    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);

    assertThat(getLoginsInTemplateAndPermission(template, DEFAULT_PERMISSION)).containsExactly("new-login");
  }

  @Test
  public void fail_if_not_a_project_permission() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(user.getLogin(), template.getUuid(), GlobalPermissions.PROVISIONING);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(user.getLogin(), template.getUuid(), DEFAULT_PERMISSION);
  }

  @Test
  public void fail_if_user_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, template.getUuid(), DEFAULT_PERMISSION);
  }

  @Test
  public void fail_if_permission_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest(user.getLogin(), template.getUuid(), null);
  }

  @Test
  public void fail_if_template_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest(user.getLogin(), null, DEFAULT_PERMISSION);
  }

  @Test
  public void fail_if_user_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' is not found");

    newRequest("unknown-login", template.getUuid(), DEFAULT_PERMISSION);
  }

  @Test
  public void fail_if_template_key_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(user.getLogin(), "unknown-key", DEFAULT_PERMISSION);
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

  private void addUserToTemplate(UserDto user, PermissionTemplateDto template, String permission) {
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), template.getId(), user.getId(), permission);
    db.commit();
  }

}
