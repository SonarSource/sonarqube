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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsPermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateUserDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class TemplateUsersActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient), new UserGroupFinder(dbClient), resourceTypes);

  TemplateUsersAction underTest = new TemplateUsersAction(dbClient, userSession, dependenciesFinder);
  WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void search_for_users_with_response_example() {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    UserDto user2 = insertUser(new UserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));

    PermissionTemplateDto template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    addUserToTemplate(newPermissionTemplateUser(CODEVIEWER, template1.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(CODEVIEWER, template1.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ADMIN, template1.getId(), user2.getId()));

    commit();

    String result = newRequest(null, template1.getUuid()).execute().getInput();
    assertJson(result).isSimilarTo(getClass().getResource("template_users-example.json"));
  }

  @Test
  public void search_for_users_by_template_name() throws IOException {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user3.getId()));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate.getId(), user1.getId()));
    commit();

    InputStream responseStream = newRequest(null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2", "login-3");
    assertThat(response.getUsers(0).getPermissionsList()).containsOnly("issueadmin", "user");
    assertThat(response.getUsers(1).getPermissionsList()).containsOnly("user");
    assertThat(response.getUsers(2).getPermissionsList()).containsOnly("issueadmin");
  }

  @Test
  public void search_using_text_query() throws IOException {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user3.getId()));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate.getId(), user1.getId()));
    commit();

    InputStream responseStream = newRequest(null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(WebService.Param.TEXT_QUERY, "ame-1")
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).extracting("login").containsOnly("login-1");
  }

  @Test
  public void search_using_permission() throws IOException {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user3.getId()));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate.getId(), user1.getId()));
    commit();

    InputStream responseStream = newRequest(USER, template.getUuid())
      .setMediaType(PROTOBUF)
      .execute().getInputStream();
    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2");
    assertThat(response.getUsers(0).getPermissionsList()).containsOnly("issueadmin", "user");
    assertThat(response.getUsers(1).getPermissionsList()).containsOnly("user");
  }

  @Test
  public void search_with_pagination() throws IOException {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user3.getId()));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate.getId(), user1.getId()));
    commit();

    InputStream responseStream = newRequest(USER, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(WebService.Param.PAGE, "2")
      .setParam(WebService.Param.PAGE_SIZE, "1")
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).extracting("login").containsOnly("login-2");
  }

  @Test
  public void users_are_sorted_by_name() throws IOException {
    setSysAdminUser();

    UserDto user1 = insertUser(new UserDto().setLogin("login-2").setName("name-2"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-3").setName("name-3"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-1").setName("name-1"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(USER, template.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template.getId(), user3.getId()));
    commit();

    InputStream responseStream = newRequest(null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2", "login-3");
  }

  @Test
  public void empty_result_when_no_user_on_template() throws IOException {
    setSysAdminUser();

    UserDto user = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));

    PermissionTemplateDto anotherTemplate = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate.getId(), user.getId()));
    commit();

    InputStream responseStream = newRequest(null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsPermissions.UsersWsResponse response = WsPermissions.UsersWsResponse.parseFrom(responseStream);
    assertThat(response.getUsersList()).isEmpty();
  }

  @Test
  public void fail_if_not_a_project_permission() throws IOException {
    setSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    commit();

    expectedException.expect(BadRequestException.class);
    newRequest(GlobalPermissions.PROVISIONING, template.getUuid())
      .execute();
  }

  @Test
  public void fail_if_no_template_param() {
    setSysAdminUser();

    expectedException.expect(BadRequestException.class);
    newRequest(null, null)
      .execute();
  }

  @Test
  public void fail_if_template_does_not_exist() {
    setSysAdminUser();

    expectedException.expect(NotFoundException.class);
    newRequest(null, "unknown-template-uuid")
      .execute();
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    setSysAdminUser();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    commit();

    expectedException.expect(BadRequestException.class);
    newRequest(null, template.getUuid())
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    commit();

    expectedException.expect(UnauthorizedException.class);
    newRequest(null, template.getUuid()).execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.login("login");

    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    commit();

    expectedException.expect(ForbiddenException.class);
    newRequest(null, template.getUuid()).execute();
  }

  private UserDto insertUser(UserDto userDto) {
    return dbClient.userDao().insert(dbSession, userDto.setActive(true));
  }

  private void addUserToTemplate(PermissionTemplateUserDto userRoleDto) {
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, userRoleDto);
  }

  private void commit() {
    dbSession.commit();
  }

  private static PermissionTemplateUserDto newPermissionTemplateUser(String permission, long permissionTemplateId, long userId) {
    return newPermissionTemplateUserDto()
      .setPermission(permission)
      .setTemplateId(permissionTemplateId)
      .setUserId(userId);
  }

  private TestRequest newRequest(@Nullable String permission, @Nullable String templateUuid) {
    TestRequest request = ws.newRequest();
    if (permission != null) {
      request.setParam(PARAM_PERMISSION, permission);
    }
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    return request;
  }

  private void setSysAdminUser() {
    userSession.login("login").setGlobalPermissions(ADMIN);
  }
}
