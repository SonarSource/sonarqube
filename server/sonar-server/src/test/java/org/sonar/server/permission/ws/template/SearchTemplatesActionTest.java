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
package org.sonar.server.permission.ws.template;

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsPermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchTemplatesActionTest extends BasePermissionWsTest<SearchTemplatesAction> {

  private I18nRule i18n = new I18nRule();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  @Override
  protected SearchTemplatesAction buildWsAction() {
    Settings settings = new MapSettings();
    settings.setProperty(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT), UUID_EXAMPLE_01);
    settings.setProperty(defaultRootQualifierTemplateProperty(Qualifiers.VIEW), UUID_EXAMPLE_02);
    settings.setProperty(defaultRootQualifierTemplateProperty("DEV"), UUID_EXAMPLE_03);

    DefaultPermissionTemplateFinder defaultPermissionTemplateFinder = new DefaultPermissionTemplateFinder(settings, resourceTypes);
    SearchTemplatesDataLoader dataLoader = new SearchTemplatesDataLoader(dbClient, defaultPermissionTemplateFinder);
    return new SearchTemplatesAction(dbClient, userSession, i18n, newPermissionWsSupport(), dataLoader);
  }

  @Before
  public void setUp() {
    i18n.setProjectPermissions();
    userSession.login().addOrganizationPermission(db.getDefaultOrganization().getUuid(), SYSTEM_ADMIN);
  }

  @Test
  public void search_project_permissions() {
    PermissionTemplateDto projectTemplate = insertProjectTemplateOnDefaultOrganization();
    PermissionTemplateDto viewsTemplate = insertViewsTemplate();
    PermissionTemplateDto developerTemplate = insertDeveloperTemplate();

    UserDto user1 = insertUser(newUserDto());
    UserDto user2 = insertUser(newUserDto());
    UserDto user3 = insertUser(newUserDto());

    GroupDto group1 = insertGroup(newGroupDto());
    GroupDto group2 = insertGroup(newGroupDto());
    GroupDto group3 = insertGroup(newGroupDto());

    addUserToTemplate(projectTemplate.getId(), user1.getId(), UserRole.ISSUE_ADMIN);
    addUserToTemplate(projectTemplate.getId(), user2.getId(), UserRole.ISSUE_ADMIN);
    addUserToTemplate(projectTemplate.getId(), user3.getId(), UserRole.ISSUE_ADMIN);
    addUserToTemplate(projectTemplate.getId(), user1.getId(), UserRole.CODEVIEWER);
    addGroupToTemplate(projectTemplate.getId(), group1.getId(), UserRole.ADMIN);
    addPermissionTemplateWithProjectCreator(projectTemplate.getId(), UserRole.ADMIN);

    addUserToTemplate(viewsTemplate.getId(), user1.getId(), UserRole.USER);
    addUserToTemplate(viewsTemplate.getId(), user2.getId(), UserRole.USER);
    addGroupToTemplate(viewsTemplate.getId(), group1.getId(), UserRole.ISSUE_ADMIN);
    addGroupToTemplate(viewsTemplate.getId(), group2.getId(), UserRole.ISSUE_ADMIN);
    addGroupToTemplate(viewsTemplate.getId(), group3.getId(), UserRole.ISSUE_ADMIN);

    addGroupToTemplate(developerTemplate.getId(), group1.getId(), UserRole.USER);

    db.commit();

    String result = newRequest().execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("search_templates-example.json"));
  }

  @Test
  public void empty_result() {
    String result = newRequest().execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("permissions")
      .isSimilarTo(getClass().getResource("SearchTemplatesActionTest/empty.json"));
  }

  @Test
  public void search_by_name_in_default_organization() {
    insertProjectTemplateOnDefaultOrganization();
    insertViewsTemplate();
    insertDeveloperTemplate();
    db.commit();

    String result = newRequest()
      .setParam(TEXT_QUERY, "views")
      .execute()
      .getInput();

    assertThat(result).contains("Default template for Views")
      .doesNotContain("projects")
      .doesNotContain("developers");
  }

  @Test
  public void search_in_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    PermissionTemplateDto templateInOrg = insertProjectTemplate(org);
    insertProjectTemplateOnDefaultOrganization();
    db.commit();
    userSession.addOrganizationPermission(org.getUuid(), SYSTEM_ADMIN);

    WsPermissions.SearchTemplatesWsResponse result = WsPermissions.SearchTemplatesWsResponse.parseFrom(newRequest()
      .setParam("organization", org.getKey())
      .setMediaType(MediaTypes.PROTOBUF)
      .execute()
      .getInputStream());

    assertThat(result.getPermissionTemplatesCount()).isEqualTo(1);
    assertThat(result.getPermissionTemplates(0).getId()).isEqualTo(templateInOrg.getUuid());
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest().execute();
  }

  @Test
  public void display_all_project_permissions() {
    String result = newRequest().execute().getInput();

    assertJson(result)
      .withStrictArrayOrder()
      .ignoreFields("defaultTemplates", "permissionTemplates")
      .isSimilarTo(getClass().getResource("SearchTemplatesActionTest/display_all_project_permissions.json"));
  }

  private PermissionTemplateDto insertProjectTemplateOnDefaultOrganization() {
    return insertProjectTemplate(db.getDefaultOrganization());
  }

  private PermissionTemplateDto insertProjectTemplate(OrganizationDto org) {
    return insertTemplate(newPermissionTemplateDto()
      .setOrganizationUuid(org.getUuid())
      .setUuid(UUID_EXAMPLE_01)
      .setName("Default template for Projects")
      .setDescription("Template for new projects")
      .setKeyPattern(null)
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_000_000_000_000L)));
  }

  private PermissionTemplateDto insertViewsTemplate() {
    return insertTemplate(newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .setUuid(UUID_EXAMPLE_02)
      .setName("Default template for Views")
      .setDescription("Template for new views")
      .setKeyPattern(".*sonar.views.*")
      .setCreatedAt(new Date(1_000_000_000_000L))
      .setUpdatedAt(new Date(1_100_000_000_000L)));
  }

  private PermissionTemplateDto insertDeveloperTemplate() {
    return insertTemplate(newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .setUuid(UUID_EXAMPLE_03)
      .setName("Default template for Developers")
      .setKeyPattern(".*sonar.developer.*")
      .setDescription(null)
      .setCreatedAt(new Date(1_100_500_000_000L))
      .setUpdatedAt(new Date(1_100_900_000_000L)));
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(db.getSession(), template);
  }

  private GroupDto insertGroup(GroupDto groupDto) {
    return dbClient.groupDao().insert(db.getSession(), groupDto);
  }

  private UserDto insertUser(UserDto userDto) {
    return dbClient.userDao().insert(db.getSession(), userDto.setActive(true));
  }

  private void addGroupToTemplate(long templateId, @Nullable Long groupId, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(db.getSession(), templateId, groupId, permission);
  }

  private void addUserToTemplate(long templateId, long userId, String permission) {
    dbClient.permissionTemplateDao().insertUserPermission(db.getSession(), templateId, userId, permission);
  }

  private void addPermissionTemplateWithProjectCreator(long templateId, String permission) {
    dbClient.permissionTemplateCharacteristicDao().insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setWithProjectCreator(true)
      .setTemplateId(templateId)
      .setPermission(permission)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));
    db.commit();
  }
}
