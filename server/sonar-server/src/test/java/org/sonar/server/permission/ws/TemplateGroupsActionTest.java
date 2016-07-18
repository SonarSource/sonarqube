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
import java.io.InputStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateGroupDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class TemplateGroupsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  // PermissionTemplateDto template1;
  // PermissionTemplateDto template2;

  TemplateGroupsAction underTest = new TemplateGroupsAction(dbClient, userSession,
    new PermissionDependenciesFinder(
      dbClient,
      new ComponentFinder(dbClient),
      new UserGroupFinder(dbClient),
      resourceTypes));

  WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void template_groups_of_json_example() {
    logAsSysAdminUser();

    GroupDto adminGroup = insertGroup(newGroupDto().setName("sonar-administrators").setDescription("System administrators"));
    GroupDto userGroup = insertGroup(newGroupDto().setName("sonar-users").setDescription("Any new users created will automatically join this group"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getId(), adminGroup.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getId(), userGroup.getId()));
    // Anyone group
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), null));
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getId(), null));
    commit();

    String response = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute().getInput();

    assertJson(response)
      .ignoreFields("id")
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("template_groups-example.json"));
  }

  @Test
  public void return_all_permissions_of_matching_groups() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name"));
    addGroupToTemplate(newPermissionTemplateGroup(CODEVIEWER, template.getId(), group1.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getId(), group1.getId()));

    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group2.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getId(), group2.getId()));

    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name"));

    // Anyone
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), null));
    addGroupToTemplate(newPermissionTemplateGroup(ISSUE_ADMIN, template.getId(), null));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, anotherTemplate.getId(), group3.getId()));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name");
    assertThat(response.getGroups(0).getPermissionsList()).containsOnly("user", "issueadmin");
    assertThat(response.getGroups(1).getPermissionsList()).containsOnly("codeviewer", "admin");
    assertThat(response.getGroups(2).getPermissionsList()).containsOnly("user", "admin");
  }

  @Test
  public void search_by_permission() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group1.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(CODEVIEWER, template.getId(), group1.getId()));

    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name"));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getId(), group2.getId()));

    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name"));

    // Anyone
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), null));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, anotherTemplate.getId(), group3.getId()));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name");
    assertThat(response.getGroups(0).getPermissionsList()).containsOnly("user");
    assertThat(response.getGroups(1).getPermissionsList()).containsOnly("user", "codeviewer");
  }

  @Test
  public void search_by_template_name() throws IOException {
    logAsSysAdminUser();

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name").setDescription("group-1-description"));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name").setDescription("group-2-description"));
    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name").setDescription("group-3-description"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group1.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(ADMIN, template.getId(), group2.getId()));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), null));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, anotherTemplate.getId(), group1.getId()));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone", "group-1-name", "group-2-name");
  }

  @Test
  public void search_with_pagination() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group1.getId()));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group2.getId()));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(PAGE, "2")
      .setParam(PAGE_SIZE, "1")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-2-name");
  }

  @Test
  public void search_with_text_query() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group1.getId()));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name"));
    GroupDto group3 = insertGroup(new GroupDto().setName("group-3"));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(TEXT_QUERY, "-nam")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name");
  }

  @Test
  public void search_with_text_query_return_all_groups_even_when_no_permission_set() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    insertGroup(new GroupDto().setName("group-1-name"));
    insertGroup(new GroupDto().setName("group-2-name"));
    insertGroup(new GroupDto().setName("group-3-name"));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(TEXT_QUERY, "-name")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("group-1-name", "group-2-name", "group-3-name");
    assertThat(response.getGroups(0).getPermissionsList()).isEmpty();
    assertThat(response.getGroups(1).getPermissionsList()).isEmpty();
    assertThat(response.getGroups(2).getPermissionsList()).isEmpty();
  }

  @Test
  public void search_with_text_query_return_anyone_group_even_when_no_permission_set() throws IOException {
    logAsSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    GroupDto group = insertGroup(new GroupDto().setName("group"));
    addGroupToTemplate(newPermissionTemplateGroup(USER, template.getId(), group.getId()));
    commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam(PARAM_TEMPLATE_ID, template.getUuid())
      .setParam(TEXT_QUERY, "nyo")
      .execute()
      .getInputStream();
    WsGroupsResponse response = WsGroupsResponse.parseFrom(responseStream);

    assertThat(response.getGroupsList()).extracting("name").containsExactly("Anyone");
    assertThat(response.getGroups(0).getPermissionsList()).isEmpty();
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    PermissionTemplateDto template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    expectedException.expect(UnauthorizedException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.login();

    PermissionTemplateDto template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    logAsSysAdminUser();

    PermissionTemplateDto template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    expectedException.expect(BadRequestException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();
  }

  @Test
  public void fail_if_template_uuid_nor_name_provided() {
    logAsSysAdminUser();

    expectedException.expect(BadRequestException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .execute();
  }

  @Test
  public void fail_if_template_is_not_found() {
    logAsSysAdminUser();

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, USER)
      .setParam(PARAM_TEMPLATE_ID, "unknown-uuid")
      .execute();
  }

  @Test
  public void fail_if_not_a_project_permission() {
    logAsSysAdminUser();

    PermissionTemplateDto template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    expectedException.expect(BadRequestException.class);
    ws.newRequest()
      .setParam(PARAM_PERMISSION, DASHBOARD_SHARING)
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

  private void logAsSysAdminUser() {
    userSession.login("login").setGlobalPermissions(ADMIN);
  }
}
