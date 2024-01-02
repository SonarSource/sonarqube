/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.DefaultTemplatesResolver;
import org.sonar.server.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateActionIT {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private final UserSessionRule userSession = UserSessionRule.standalone();
  private final DbClient dbClient = db.getDbClient();
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(PROJECT, APP, VIEW);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(dbClient, resourceTypes);
  private final Configuration configuration = mock(Configuration.class);

  private WsActionTester underTest;

  @Before
  public void setUp() {
    GroupWsSupport groupWsSupport = new GroupWsSupport(dbClient, new DefaultGroupFinder(db.getDbClient()));
    this.underTest = new WsActionTester(new DeleteTemplateAction(dbClient, userSession,
      new PermissionWsSupport(dbClient, configuration, groupWsSupport), defaultTemplatesResolver));
  }

  @Test
  public void delete_template_in_db() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    PermissionTemplateDto projectPermissionTemplate = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto portfolioPermissionTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, portfolioPermissionTemplate);
    loginAsAdmin();

    TestResponse result = newRequestByUuid(underTest, template.getUuid());

    assertThat(result.getInput()).isEmpty();
    assertTemplateDoesNotExist(template);
  }

  @Test
  public void delete_template_by_name_case_insensitive() {
    db.permissionTemplates().setDefaultTemplates(
      db.permissionTemplates().insertTemplate().getUuid(),
      db.permissionTemplates().insertTemplate().getUuid(), db.permissionTemplates().insertTemplate().getUuid());
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    loginAsAdmin();

    newRequestByName(underTest, template);

    assertTemplateDoesNotExist(template);
  }

  @Test
  public void fail_if_uuid_is_not_known() {
    userSession.logIn();

    assertThatThrownBy(() -> newRequestByUuid(underTest, "unknown-template-uuid"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template() {
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions();
    db.permissionTemplates().setDefaultTemplates(projectTemplate,
      null, db.permissionTemplates().insertTemplate());
    loginAsAdmin();

    assertThatThrownBy(() -> newRequestByUuid(underTest, projectTemplate.getUuid()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to delete the default permission template for projects");
  }

  @Test
  public void fail_to_delete_by_name_if_template_is_default_template_for_project() {
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions();
    db.permissionTemplates().setDefaultTemplates(projectTemplate, null, db.permissionTemplates().insertTemplate());
    loginAsAdmin();

    assertThatThrownBy(() -> newRequestByName(underTest, projectTemplate.getName()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to delete the default permission template for projects");
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_portfolios() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    db.permissionTemplates().setDefaultTemplates(db.permissionTemplates().insertTemplate(), null, template);
    loginAsAdmin();

    assertThatThrownBy(() -> newRequestByUuid(this.underTest, template.getUuid()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to delete the default permission template for portfolios");
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_applications() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    db.permissionTemplates().setDefaultTemplates(db.permissionTemplates().insertTemplate(), template, null);
    loginAsAdmin();

    assertThatThrownBy(() -> newRequestByUuid(this.underTest, template.getUuid()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to delete the default permission template for applications");
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_logged_in() {
    assertThatThrownBy(() -> newRequestByUuid(underTest, "uuid"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_to_delete_by_name_if_not_logged_in() {
    assertThatThrownBy(() -> newRequestByName(underTest, "name"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_admin() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    userSession.logIn();

    assertThatThrownBy(() -> newRequestByUuid(underTest, template.getUuid()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_to_delete_by_name_if_not_admin() {
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    userSession.logIn();

    assertThatThrownBy(() -> newRequestByName(underTest, template.getName()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_neither_uuid_nor_name_is_provided() {
    userSession.logIn();

    assertThatThrownBy(() -> newRequestByUuid(underTest, null))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_both_uuid_and_name_are_provided() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      underTest.newRequest().setMethod("POST")
        .setParam(PARAM_TEMPLATE_ID, "uuid")
        .setParam(PARAM_TEMPLATE_NAME, "name")
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  private UserSessionRule loginAsAdmin() {
    return userSession.logIn().addPermission(ADMINISTER);
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions() {
    PermissionTemplateDto dto = db.permissionTemplates().insertTemplate();
    UserDto user = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto().setActive(true));
    GroupDto group = db.getDbClient().groupDao().insert(db.getSession(), GroupTesting.newGroupDto());
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), dto.getUuid(), user.getUuid(), UserRole.ADMIN,
      dto.getName(), user.getLogin());
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), dto.getUuid(), group.getUuid(), UserRole.CODEVIEWER,
      dto.getName(), group.getName());
    db.commit();
    return dto;
  }

  private TestResponse newRequestByUuid(WsActionTester actionTester, @Nullable String id) {
    TestRequest request = actionTester.newRequest().setMethod("POST");
    if (id != null) {
      request.setParam(PARAM_TEMPLATE_ID, id);
    }
    return request.execute();
  }

  private void newRequestByName(WsActionTester actionTester, @Nullable PermissionTemplateDto permissionTemplateDto) {
    newRequestByName(
      actionTester,
      permissionTemplateDto == null ? null : permissionTemplateDto.getName());
  }

  private TestResponse newRequestByName(WsActionTester actionTester, @Nullable String name) {
    TestRequest request = actionTester.newRequest().setMethod("POST");

    if (name != null) {
      request.setParam(PARAM_TEMPLATE_NAME, name);
    }

    return request.execute();
  }

  private void assertTemplateDoesNotExist(PermissionTemplateDto template) {
    assertThat(db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid())).isNull();
  }

}
