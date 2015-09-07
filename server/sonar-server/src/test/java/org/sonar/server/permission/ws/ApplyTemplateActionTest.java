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

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.permission.ws.Parameters.PARAM_PROJECT_ID;
import static org.sonar.server.permission.ws.Parameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_NAME;

@Category(DbTests.class)
public class ApplyTemplateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;

  UserDto user1;
  UserDto user2;
  GroupDto group1;
  GroupDto group2;
  ComponentDto project;
  PermissionTemplateDto template1;
  PermissionTemplateDto template2;
  IssueAuthorizationIndexer issueAuthorizationIndexer = mock(IssueAuthorizationIndexer.class);

  @Before
  public void setUp() {
    userSession.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    dbClient = db.getDbClient();
    dbSession = db.getSession();

    PermissionRepository repository = new PermissionRepository(dbClient, new Settings());
    PermissionFinder permissionFinder = new PermissionFinder(dbClient);
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    PermissionService permissionService = new PermissionService(dbClient, repository, issueAuthorizationIndexer, userSession, componentFinder);
    PermissionDependenciesFinder permissionDependenciesFinder = new PermissionDependenciesFinder(dbClient, componentFinder);

    ApplyTemplateAction underTest = new ApplyTemplateAction(dbClient, permissionService, permissionDependenciesFinder);
    ws = new WsActionTester(underTest);

    user1 = insertUser(newUserDto().setLogin("user-login-1"));
    user2 = insertUser(newUserDto().setLogin("user-login-2"));
    group1 = insertGroup(newGroupDto().setName("group-name-1"));
    group2 = insertGroup(newGroupDto().setName("group-name-2"));

    // template 1
    template1 = insertTemplate(newPermissionTemplateDto().setUuid("permission-template-uuid-1"));
    addUserToTemplate(user1, template1, UserRole.CODEVIEWER);
    addUserToTemplate(user2, template1, UserRole.ISSUE_ADMIN);
    addGroupToTemplate(group1, template1, UserRole.ADMIN);
    addGroupToTemplate(group2, template1, UserRole.USER);
    // template 2
    template2 = insertTemplate(newPermissionTemplateDto().setUuid("permission-template-uuid-2"));
    addUserToTemplate(user1, template2, UserRole.USER);
    addUserToTemplate(user2, template2, UserRole.USER);
    addGroupToTemplate(group1, template2, UserRole.USER);
    addGroupToTemplate(group2, template2, UserRole.USER);

    project = insertProject(newProjectDto("project-uuid-1"));
    addUserPermissionToProject(user1, project, UserRole.ADMIN);
    addUserPermissionToProject(user2, project, UserRole.ADMIN);
    addGroupPermissionToProject(group1, project, UserRole.ADMIN);
    addGroupPermissionToProject(group2, project, UserRole.ADMIN);

    commit();
  }

  @Test
  public void apply_template_with_project_uuid() {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).hasSize(2);
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).hasSize(2);

    newRequest(template1.getUuid(), project.uuid(), null);

    assertTemplate1AppliedToProject();
    verify(issueAuthorizationIndexer).index();
  }

  @Test
  public void apply_template_with_project_uuid_by_template_name() {
    ws.newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template1.getName().toUpperCase())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute();
    commit();

    assertTemplate1AppliedToProject();
  }

  @Test
  public void apply_template_with_project_key() {
    newRequest(template1.getUuid(), null, project.key());

    assertTemplate1AppliedToProject();
  }

  @Test
  public void fail_when_unknown_template() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-template-uuid' is not found");

    newRequest("unknown-template-uuid", project.uuid(), null);
  }

  @Test
  public void fail_when_unknown_project_uuid() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project id 'unknown-project-uuid' not found");

    newRequest(template1.getUuid(), "unknown-project-uuid", null);
  }

  @Test
  public void fail_when_unknown_project_key() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'unknown-project-key' not found");

    newRequest(template1.getUuid(), null, "unknown-project-key");
  }

  @Test
  public void fail_when_template_is_not_provided() {
    expectedException.expect(BadRequestException.class);

    newRequest(null, project.uuid(), null);
  }

  @Test
  public void fail_when_project_uuid_and_key_not_provided() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key must be provided, not both.");

    newRequest(template1.getUuid(), null, null);
  }

  @Test
  public void fail_when_anonymous() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest(template1.getUuid(), project.uuid(), null);
  }

  @Test
  public void fail_when_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    newRequest(template1.getUuid(), project.uuid(), null);
  }

  private void assertTemplate1AppliedToProject() {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).extracting("name").containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).extracting("name").containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).extracting("login").containsExactly(user1.getLogin());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).extracting("login").containsExactly(user2.getLogin());
  }

  private TestResponse newRequest(@Nullable String templateUuid, @Nullable String projectUuid, @Nullable String projectKey) {
    TestRequest request = ws.newRequest();
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    if (projectUuid != null) {
      request.setParam(PARAM_PROJECT_ID, projectUuid);
    }
    if (projectKey != null) {
      request.setParam(PARAM_PROJECT_KEY, projectKey);
    }

    TestResponse result = request.execute();
    commit();

    return result;
  }

  private ComponentDto insertProject(ComponentDto project) {
    dbClient.componentDao().insert(dbSession, project);
    return dbClient.componentDao().selectOrFailByUuid(dbSession, project.uuid());
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(dbSession, template);
  }

  private UserDto insertUser(UserDto userDto) {
    return dbClient.userDao().insert(dbSession, userDto.setActive(true));
  }

  private GroupDto insertGroup(GroupDto group) {
    return dbClient.groupDao().insert(dbSession, group);
  }

  private void addUserToTemplate(UserDto user, PermissionTemplateDto permissionTemplate, String permission) {
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, permissionTemplate.getId(), user.getId(), permission);
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, permissionTemplate.getId(), group.getId(), permission);
  }

  private void addUserPermissionToProject(UserDto user, ComponentDto project, String permission) {
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(permission)
      .setUserId(user.getId())
      .setResourceId(project.getId()));
  }

  private void addGroupPermissionToProject(GroupDto group, ComponentDto project, String permission) {
    dbClient.roleDao().insertGroupRole(dbSession, new GroupRoleDto()
      .setRole(permission)
      .setResourceId(project.getId())
      .setGroupId(group.getId()));
  }

  private List<GroupWithPermissionDto> selectProjectPermissionGroups(ComponentDto project, String permission) {
    return FluentIterable.from(dbClient.permissionDao().selectGroups(dbSession, query(permission), project.getId()))
      .filter(new PermissionNotNull())
      .toList();
  }

  private List<UserWithPermissionDto> selectProjectPermissionUsers(ComponentDto project, String permission) {
    return dbClient.permissionDao().selectUsers(dbSession, query(permission), project.getId(), 0, Integer.MAX_VALUE);
  }

  private void commit() {
    dbSession.commit();
  }

  private static PermissionQuery query(String permission) {
    return PermissionQuery.builder().membership(IN).permission(permission).build();
  }

  private static class PermissionNotNull implements Predicate<GroupWithPermissionDto> {
    @Override
    public boolean apply(@Nullable GroupWithPermissionDto input) {
      return input.getPermission() != null;
    }
  }
}
