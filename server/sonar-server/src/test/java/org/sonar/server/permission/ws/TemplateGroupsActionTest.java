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
import java.io.InputStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateGroupDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class TemplateGroupsActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  DbClient dbClient;
  DbSession dbSession;
  WsActionTester ws;

  PermissionTemplateDto template1;
  PermissionTemplateDto template2;

  TemplateGroupsAction underTest;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    underTest = new TemplateGroupsAction(dbClient, userSession,
      new PermissionDependenciesFinder(
        dbClient,
        new ComponentFinder(dbClient),
        new UserGroupFinder(dbClient),
        resourceTypes));
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);

    template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    template2 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name").setDescription("group-1-description"));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name").setDescription("group-2-description"));
    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name").setDescription("group-3-description"));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.USER, template1.getId(), group1.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.USER, template1.getId(), group2.getId()));
    // Anyone group
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.USER, template1.getId(), null));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.ADMIN, template1.getId(), group3.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.USER, template2.getId(), group1.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.USER, template2.getId(), group2.getId()));

    commit();
  }

  @Test
  public void template_groups_of_json_example() {
    GroupDto adminGroup = insertGroup(newGroupDto().setName("sonar-administrators").setDescription("System administrators"));
    GroupDto userGroup = insertGroup(newGroupDto().setName("sonar-users").setDescription("Any new users created will automatically join this group"));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.ISSUE_ADMIN, template1.getId(), adminGroup.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.ISSUE_ADMIN, template1.getId(), userGroup.getId()));
    // Anyone group
    addGroupToTemplate(newPermissionTemplateGroup(UserRole.ISSUE_ADMIN, template1.getId(), null));
    commit();

    String response = ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute().getInput();

    assertJson(response)
      .ignoreFields("id")
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("template_groups-example.json"));
  }

  @Test
  public void search_by_template_name() throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name");
  }

  @Test
  public void search_with_admin_permission_does_not_return_anyone() throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name", "group-3-name");
  }

  @Test
  public void search_with_pagination() throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(PAGE, "2")
      .setParam(PAGE_SIZE, "1")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name");
  }

  @Test
  public void search_with_selected() throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name", "group-3-name");
  }

  @Test
  public void search_with_text_query() throws IOException {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(TEXT_QUERY, "-name")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name");
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login();

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();
  }

  @Test
  public void fail_if_template_uuid_nor_name_provided() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .execute();
  }

  @Test
  public void fail_if_template_is_not_found() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .setParam(PARAM_TEMPLATE_ID, "unknown-uuid")
      .execute();
  }

  @Test
  public void fail_if_not_a_project_permission() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermissions.DASHBOARD_SHARING)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();
  }

  private GroupDto insertGroup(GroupDto group) {
    GroupDto result = dbClient.groupDao().insert(dbSession, group);
    commit();

    return result;
  }

  private void addGroupToTemplate(PermissionTemplateGroupDto permissionTemplateGroup) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, permissionTemplateGroup);
  }

  private static PermissionTemplateGroupDto newPermissionTemplateGroup(String permission, long templateId, @Nullable Long groupId) {
    return newPermissionTemplateGroupDto()
      .setPermission(permission)
      .setTemplateId(templateId)
      .setGroupId(groupId);
  }

  private void commit() {
    dbSession.commit();
  }
}
