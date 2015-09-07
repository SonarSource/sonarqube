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

import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.ws.DeleteTemplateAction;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateActionTest {

  static final String TEMPLATE_UUID = "permission-template-uuid";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;
  DefaultPermissionTemplateFinder defaultTemplatePermissionFinder;

  PermissionTemplateDto permissionTemplate;

  @Before
  public void setUp() {
    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    dbClient = db.getDbClient();
    dbSession = db.getSession();
    defaultTemplatePermissionFinder = mock(DefaultPermissionTemplateFinder.class);
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(Collections.<String>emptySet());
    PermissionDependenciesFinder finder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));
    ws = new WsActionTester(new DeleteTemplateAction(dbClient, userSession, finder, defaultTemplatePermissionFinder));

    permissionTemplate = insertTemplateAndAssociatedPermissions(newPermissionTemplateDto().setUuid(TEMPLATE_UUID));
    PermissionTemplateDto permissionTemplateInDatabase = dbClient.permissionTemplateDao().selectByUuidWithUserAndGroupPermissions(dbSession, TEMPLATE_UUID);
    assertThat(permissionTemplateInDatabase.getUuid()).isEqualTo(TEMPLATE_UUID);
    assertThat(permissionTemplateInDatabase.getGroupsPermissions()).isNotEmpty();
    assertThat(permissionTemplateInDatabase.getUsersPermissions()).isNotEmpty();
  }

  @Test
  public void delete_template_in_db() {
    TestResponse result = newRequest(TEMPLATE_UUID);

    assertThat(result.getInput()).isEmpty();
    assertThat(dbClient.permissionTemplateDao().selectByUuidWithUserAndGroupPermissions(dbSession, TEMPLATE_UUID)).isNull();
  }

  @Test
  public void delete_template_by_name_case_insensitive() {
    ws.newRequest()
      .setParam(PARAM_TEMPLATE_NAME, permissionTemplate.getName().toUpperCase())
      .execute();
    commit();

    assertThat(dbClient.permissionTemplateDao().selectByUuidWithUserAndGroupPermissions(dbSession, TEMPLATE_UUID)).isNull();
  }

  @Test
  public void fail_if_uuid_is_not_known() {
    expectedException.expect(NotFoundException.class);

    newRequest("unknown-template-uuid");
  }

  @Test
  public void fail_if_template_is_default() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete a default template");
    when(defaultTemplatePermissionFinder.getDefaultTemplateUuids()).thenReturn(newSet(TEMPLATE_UUID));

    newRequest(TEMPLATE_UUID);
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(TEMPLATE_UUID);
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    newRequest(TEMPLATE_UUID);
  }

  @Test
  public void fail_if_uuid_is_not_provided() {
    expectedException.expect(BadRequestException.class);

    newRequest(null);
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions(PermissionTemplateDto template) {
    dbClient.permissionTemplateDao().insert(dbSession, template);
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto().setActive(true));
    GroupDto group = dbClient.groupDao().insert(dbSession, GroupTesting.newGroupDto());
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, template.getId(), user.getId(), UserRole.ADMIN);
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), group.getId(), UserRole.CODEVIEWER);
    commit();

    return template;
  }

  private void commit() {
    dbSession.commit();
  }

  private TestResponse newRequest(@Nullable String id) {
    TestRequest request = ws.newRequest();
    if (id != null) {
      request.setParam(PARAM_TEMPLATE_ID, id);
    }

    TestResponse result = executeRequest(request);
    commit();
    return result;
  }

  private static TestResponse executeRequest(TestRequest request) {
    return request.execute();
  }

}
