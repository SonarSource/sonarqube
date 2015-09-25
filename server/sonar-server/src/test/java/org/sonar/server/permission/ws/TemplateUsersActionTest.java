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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.PermissionTemplateUserDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonarqube.ws.WsPermissions.WsTemplateUsersResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateUserDto;
import static org.sonar.server.plugins.MimeTypes.PROTOBUF;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.WsPermissions.WsTemplateUsersResponse.parseFrom;

@Category(DbTests.class)
public class TemplateUsersActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  WsActionTester ws;

  TemplateUsersAction underTest;

  PermissionTemplateDto template1;
  PermissionTemplateDto template2;

  @Before
  public void setUp() {
    PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));
    underTest = new TemplateUsersAction(dbClient, userSession, dependenciesFinder);
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(ADMIN);

    template1 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-1"));
    template2 = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setUuid("template-uuid-2"));

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    addUserToTemplate(newPermissionTemplateUser(UserRole.USER, template1.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.USER, template1.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.ISSUE_ADMIN, template1.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.ISSUE_ADMIN, template1.getId(), user3.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.USER, template2.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.USER, template2.getId(), user2.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.USER, template2.getId(), user3.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.ISSUE_ADMIN, template2.getId(), user1.getId()));

    commit();
  }

  @Test
  public void search_for_users_with_response_example() {
    UserDto user1 = insertUser(new UserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    UserDto user2 = insertUser(new UserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));
    addUserToTemplate(newPermissionTemplateUser(UserRole.CODEVIEWER, template1.getId(), user1.getId()));
    addUserToTemplate(newPermissionTemplateUser(UserRole.CODEVIEWER, template1.getId(), user2.getId()));
    commit();

    String result = newRequest(UserRole.CODEVIEWER, template1.getUuid()).execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("template_users-example.json"));
  }

  @Test
  public void search_for_users_by_template_name() throws IOException {
    InputStream responseStream = newRequest(UserRole.USER, null)
      .setParam(WsPermissionParameters.PARAM_TEMPLATE_NAME, template1.getName())
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsTemplateUsersResponse response = parseFrom(responseStream);

    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2");
  }

  @Test
  public void search_using_text_query() throws IOException {
    InputStream responseStream = newRequest(UserRole.USER, null)
      .setParam(WsPermissionParameters.PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(WebService.Param.TEXT_QUERY, "ame-1")
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsTemplateUsersResponse response = parseFrom(responseStream);

    assertThat(response.getUsersList()).extracting("login").containsOnly("login-1");
  }

  @Test
  public void search_using_selected() throws IOException {
    InputStream responseStream = newRequest(UserRole.USER, null)
      .setParam(WsPermissionParameters.PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsTemplateUsersResponse response = parseFrom(responseStream);

    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2", "login-3");
    assertThat(response.getUsers(2).getSelected()).isFalse();
  }

  @Test
  public void search_with_pagination() throws IOException {
    InputStream responseStream = newRequest(UserRole.USER, null)
      .setParam(WsPermissionParameters.PARAM_TEMPLATE_NAME, template1.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(WebService.Param.PAGE, "2")
      .setParam(WebService.Param.PAGE_SIZE, "1")
      .setMediaType(PROTOBUF)
      .execute().getInputStream();

    WsTemplateUsersResponse response = parseFrom(responseStream);

    assertThat(response.getUsersList()).extracting("login").containsOnly("login-2");
  }

  @Test
  public void fail_if_not_a_project_permission() throws IOException {
    expectedException.expect(BadRequestException.class);

    newRequest(GlobalPermissions.PREVIEW_EXECUTION, template1.getUuid())
      .execute();
  }

  @Test
  public void fail_if_template_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    newRequest(UserRole.USER, "unknown-template-uuid")
      .execute();
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    expectedException.expect(BadRequestException.class);

    newRequest(UserRole.USER, template1.getUuid())
      .setParam(WsPermissionParameters.PARAM_TEMPLATE_NAME, template1.getName())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(UserRole.USER, template1.getUuid()).execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");

    newRequest(UserRole.USER, template1.getUuid()).execute();
  }

  private UserDto insertUser(UserDto userDto) {
    UserDto user = dbClient.userDao().insert(dbSession, userDto.setActive(true));
    return user;
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

  private TestRequest newRequest(String permission, @Nullable String templateUuid) {
    TestRequest request = ws.newRequest();
    request.setParam(WsPermissionParameters.PARAM_PERMISSION, permission);
    if (templateUuid != null) {
      request.setParam(WsPermissionParameters.PARAM_TEMPLATE_ID, templateUuid);
    }

    return request;
  }
}
