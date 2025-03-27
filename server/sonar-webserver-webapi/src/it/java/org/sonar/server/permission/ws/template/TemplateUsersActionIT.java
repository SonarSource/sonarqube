/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.BasePermissionWsIT;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.ws.TestRequest;
import org.sonarqube.ws.Permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.db.permission.ProjectPermission.CODEVIEWER;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateUserDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class TemplateUsersActionIT extends BasePermissionWsIT<TemplateUsersAction> {

  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final RequestValidator requestValidator = new RequestValidator(permissionService);

  @Override
  protected TemplateUsersAction buildWsAction() {
    return new TemplateUsersAction(db.getDbClient(), userSession, newPermissionWsSupport(), new AvatarResolverImpl(), wsParameters, requestValidator);
  }

  @Test
  public void define_template_users() {
    WebService.Action action = wsTester.getDef();

    assertThat(action).isNotNull();
    assertThat(action.key()).isEqualTo("template_users");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("5.2");
    WebService.Param permissionParam = action.param(PARAM_PERMISSION);
    assertThat(permissionParam).isNotNull();
    assertThat(permissionParam.isRequired()).isFalse();
  }

  @Test
  public void search_for_users_with_response_example() {
    UserDto user1 = insertUser(newUserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    UserDto user2 = insertUser(newUserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));

    PermissionTemplateDto template1 = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(CODEVIEWER, template1, user1), template1.getName());
    addUserToTemplate(newPermissionTemplateUser(CODEVIEWER, template1, user2), template1.getName());
    addUserToTemplate(newPermissionTemplateUser(ADMIN, template1, user2), template1.getName());
    loginAsAdmin();

    String result = newRequest((String) null, template1.getUuid()).execute().getInput();
    assertJson(result).isSimilarTo(getClass().getResource("template_users-example.json"));
  }

  @Test
  public void search_for_users_by_template_name() {
    loginAsAdmin();

    UserDto user1 = insertUser(newUserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user3), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate, user1), anotherTemplate.getName());

    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2", "login-3");
    assertThat(response.getUsers(0).getPermissionsList()).containsOnly("issueadmin", "user");
    assertThat(response.getUsers(1).getPermissionsList()).containsOnly("user");
    assertThat(response.getUsers(2).getPermissionsList()).containsOnly("issueadmin");
  }

  @Test
  public void search_using_text_query() {
    loginAsAdmin();

    UserDto user1 = insertUser(newUserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user3), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate, user1), anotherTemplate.getName());

    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(WebService.Param.TEXT_QUERY, "ame-1")
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).extracting("login").containsOnly("login-1");
  }

  @Test
  public void search_using_text_query_with_email() {
    loginAsAdmin();

    UserDto user1 = insertUser(newUserDto().setLogin("orange").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(newUserDto().setLogin("crANBerry").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(newUserDto().setLogin("apple").setName("name-3").setEmail("email-3"));

    String templateName = addUsersToSomeTemplate(user1, user2, user3);

    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, templateName)
      .setParam(WebService.Param.TEXT_QUERY, "ran")
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).hasSize(2);
    assertThat(response.getUsersList()).extracting("login").containsExactlyInAnyOrder("orange", "crANBerry");
  }

  @Test
  public void search_using_text_query_with_login() {
    loginAsAdmin();

    UserDto user1 = insertUser(newUserDto().setLogin("login-1").setName("name-1").setEmail("xYZ@1984.com"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-2").setName("name-2").setEmail("xyz2@1984.com"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-3").setName("name-3").setEmail("hello@1984.com"));

    String templateName = addUsersToSomeTemplate(user1, user2, user3);

    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, templateName)
      .setParam(WebService.Param.TEXT_QUERY, "xyz")
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).hasSize(2);
    assertThat(response.getUsersList()).extracting("email").containsExactlyInAnyOrder("xYZ@1984.com", "xyz2@1984.com");
  }

  private String addUsersToSomeTemplate(UserDto user1, UserDto user2, UserDto user3) {
    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user3), template.getName());
    return template.getName();
  }

  @Test
  public void search_using_permission() {
    UserDto user1 = insertUser(newUserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user3), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate, user1), anotherTemplate.getName());

    loginAsAdmin();
    Permissions.UsersWsResponse response = newRequest(USER, template.getUuid())
      .executeProtobuf(Permissions.UsersWsResponse.class);
    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2");
    assertThat(response.getUsers(0).getPermissionsList()).containsOnly("issueadmin", "user");
    assertThat(response.getUsers(1).getPermissionsList()).containsOnly("user");
  }

  @Test
  public void search_with_pagination() {
    UserDto user1 = insertUser(newUserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));

    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user3), template.getName());

    PermissionTemplateDto anotherTemplate = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, anotherTemplate, user1), anotherTemplate.getName());

    loginAsAdmin();
    Permissions.UsersWsResponse response = newRequest(USER, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(WebService.Param.PAGE, "2")
      .setParam(WebService.Param.PAGE_SIZE, "1")
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).extracting("login").containsOnly("login-2");
  }

  @Test
  public void users_are_sorted_by_name() {
    UserDto user1 = insertUser(newUserDto().setLogin("login-2").setName("name-2"));
    UserDto user2 = insertUser(newUserDto().setLogin("login-3").setName("name-3"));
    UserDto user3 = insertUser(newUserDto().setLogin("login-1").setName("name-1"));

    PermissionTemplateDto template = addTemplate();
    addUserToTemplate(newPermissionTemplateUser(USER, template, user1), template.getName());
    addUserToTemplate(newPermissionTemplateUser(USER, template, user2), template.getName());
    addUserToTemplate(newPermissionTemplateUser(ISSUE_ADMIN, template, user3), template.getName());

    loginAsAdmin();
    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList()).extracting("login").containsExactly("login-1", "login-2", "login-3");
  }

  @Test
  public void search_ignores_other_template_and_is_ordered_by_users_with_permission_when_many_users() {
    PermissionTemplateDto template = addTemplate();
    // Add another template having some users with permission to make sure it's correctly ignored
    PermissionTemplateDto otherTemplate = db.permissionTemplates().insertTemplate();
    IntStream.rangeClosed(1, DEFAULT_PAGE_SIZE + 1).forEach(i -> {
      UserDto user = db.users().insertUser("User-" + i);
      db.permissionTemplates().addUserToTemplate(otherTemplate, user, ProjectPermission.USER);
    });
    String lastLogin = "User-" + (DEFAULT_PAGE_SIZE + 1);
    db.permissionTemplates().addUserToTemplate(template, db.users().selectUserByLogin(lastLogin).get(), ProjectPermission.USER);
    loginAsAdmin();

    Permissions.UsersWsResponse response = newRequest((String) null, null)
      .setParam(PARAM_TEMPLATE_NAME, template.getName())
      .executeProtobuf(Permissions.UsersWsResponse.class);

    assertThat(response.getUsersList())
      .extracting("login")
      .hasSize(DEFAULT_PAGE_SIZE)
      .startsWith(lastLogin);
  }

  @Test
  public void fail_if_not_a_project_permission() {
    PermissionTemplateDto template = addTemplate();
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest(GlobalPermission.PROVISION_PROJECTS.getKey(), template.getUuid())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_no_template_param() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest((String) null, null)
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_template_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest((String) null, "unknown-template-uuid")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_template_uuid_and_name_provided() {
    PermissionTemplateDto template = addTemplate();
    loginAsAdmin();

    assertThatThrownBy(() ->  {
      newRequest((String) null, template.getUuid())
        .setParam(PARAM_TEMPLATE_NAME, template.getName())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_not_logged_in() {
    PermissionTemplateDto template = addTemplate();
    userSession.anonymous();

    assertThatThrownBy(() ->  {
      newRequest((String) null, template.getUuid()).execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    PermissionTemplateDto template = addTemplate();
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() ->  {
      newRequest((String) null, template.getUuid()).execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  private UserDto insertUser(UserDto userDto) {
    db.users().insertUser(userDto);
    return userDto;
  }

  private void addUserToTemplate(PermissionTemplateUserDto dto, String templateName) {
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), dto.getTemplateUuid(), dto.getUserUuid(),
      dto.getPermission(), templateName, dto.getUserLogin());
    db.commit();
  }

  private static PermissionTemplateUserDto newPermissionTemplateUser(ProjectPermission permission, PermissionTemplateDto template, UserDto user) {
    return newPermissionTemplateUser(permission.getKey(), template, user);
  }

  private static PermissionTemplateUserDto newPermissionTemplateUser(String permission, PermissionTemplateDto template, UserDto user) {
    return newPermissionTemplateUserDto()
      .setPermission(permission)
      .setTemplateUuid(template.getUuid())
      .setUserUuid(user.getUuid());
  }

  private TestRequest newRequest(@Nullable ProjectPermission permission, @Nullable String templateUuid) {
    return newRequest(permission != null ? permission.getKey() : null, templateUuid);
  }

  private TestRequest newRequest(@Nullable GlobalPermission permission, @Nullable String templateUuid) {
    return newRequest(permission != null ? permission.getKey() : null, templateUuid);
  }

  private TestRequest newRequest(@Nullable String permission, @Nullable String templateUuid) {
    TestRequest request = newRequest();
    if (permission != null) {
      request.setParam(PARAM_PERMISSION, permission);
    }
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    return request;
  }

}
