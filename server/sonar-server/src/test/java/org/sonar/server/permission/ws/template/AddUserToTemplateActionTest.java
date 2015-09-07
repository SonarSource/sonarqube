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

package org.sonar.server.permission.ws.template;

import com.google.common.base.Function;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.WsPermissionParameters;
import org.sonar.server.permission.ws.template.AddUserToTemplateAction;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_NAME;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_USER_LOGIN;

@Category(DbTests.class)
public class AddUserToTemplateActionTest {

  private static final String USER_LOGIN = "user-login";
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;
  UserDto user;
  PermissionTemplateDto permissionTemplate;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));
    ws = new WsActionTester(new AddUserToTemplateAction(dbClient, dependenciesFinder, userSession));

    user = insertUser(newUserDto().setLogin(USER_LOGIN));
    permissionTemplate = insertPermissionTemplate(newPermissionTemplateDto());
    commit();
  }

  @Test
  public void add_user_to_template() {
    newRequest(USER_LOGIN, permissionTemplate.getKee(), CODEVIEWER);

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate.getId(), CODEVIEWER)).containsExactly(USER_LOGIN);
  }

  @Test
  public void add_user_to_template_by_name() {
    ws.newRequest()
      .setParam(PARAM_USER_LOGIN, USER_LOGIN)
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .execute();
    commit();

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate.getId(), CODEVIEWER)).containsExactly(USER_LOGIN);
  }

  @Test
  public void does_not_add_a_user_twice() {
    newRequest(USER_LOGIN, permissionTemplate.getKee(), ISSUE_ADMIN);
    newRequest(USER_LOGIN, permissionTemplate.getKee(), ISSUE_ADMIN);

    assertThat(getLoginsInTemplateAndPermission(permissionTemplate.getId(), ISSUE_ADMIN)).containsExactly(USER_LOGIN);
  }

  @Test
  public void fail_if_not_a_project_permission() {
    expectedException.expect(BadRequestException.class);

    newRequest(USER_LOGIN, permissionTemplate.getKee(), GlobalPermissions.PREVIEW_EXECUTION);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(USER_LOGIN, permissionTemplate.getKee(), CODEVIEWER);
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(USER_LOGIN, permissionTemplate.getKee(), CODEVIEWER);
  }

  @Test
  public void fail_if_user_missing() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(null, permissionTemplate.getKee(), CODEVIEWER);
  }

  @Test
  public void fail_if_permission_missing() {
    expectedException.expect(IllegalArgumentException.class);

    newRequest(USER_LOGIN, permissionTemplate.getKee(), null);
  }

  @Test
  public void fail_if_template_uuid_and_name_are_missing() {
    expectedException.expect(BadRequestException.class);

    newRequest(USER_LOGIN, null, CODEVIEWER);
  }

  @Test
  public void fail_if_user_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' is not found");

    newRequest("unknown-login", permissionTemplate.getKee(), CODEVIEWER);
  }

  @Test
  public void fail_if_template_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-key' is not found");

    newRequest(USER_LOGIN, "unknown-key", CODEVIEWER);
  }

  private void newRequest(@Nullable String userLogin, @Nullable String templateKey, @Nullable String permission) {
    TestRequest request = ws.newRequest();
    if (userLogin != null) {
      request.setParam(PARAM_USER_LOGIN, userLogin);
    }
    if (templateKey != null) {
      request.setParam(WsPermissionParameters.PARAM_TEMPLATE_ID, templateKey);
    }
    if (permission != null) {
      request.setParam(WsPermissionParameters.PARAM_PERMISSION, permission);
    }

    request.execute();
  }

  private void commit() {
    dbSession.commit();
  }

  private UserDto insertUser(UserDto userDto) {
    return dbClient.userDao().insert(dbSession, userDto.setActive(true));
  }

  private PermissionTemplateDto insertPermissionTemplate(PermissionTemplateDto permissionTemplate) {
    return dbClient.permissionTemplateDao().insert(dbSession, permissionTemplate);
  }

  private List<String> getLoginsInTemplateAndPermission(long templateId, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().permission(permission).membership(IN).build();
    return from(dbClient.permissionTemplateDao()
      .selectUsers(dbSession, permissionQuery, templateId, 0, Integer.MAX_VALUE))
      .transform(UserWithPermissionToUserLogin.INSTANCE)
      .toList();
  }

  private enum UserWithPermissionToUserLogin implements Function<UserWithPermissionDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull UserWithPermissionDto userWithPermission) {
      return userWithPermission.getLogin();
    }

  }
}
