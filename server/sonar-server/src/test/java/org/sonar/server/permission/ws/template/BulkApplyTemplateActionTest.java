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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.OldPermissionQuery;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.user.GroupDbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

public class BulkApplyTemplateActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  UserDbTester userDb = new UserDbTester(db);
  GroupDbTester groupDb = new GroupDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");
  I18nRule i18n = new I18nRule();
  WsActionTester ws;

  UserDto user1;
  UserDto user2;
  GroupDto group1;
  GroupDto group2;
  PermissionTemplateDto template1;
  PermissionTemplateDto template2;
  IssueAuthorizationIndexer issueAuthorizationIndexer = mock(IssueAuthorizationIndexer.class);

  @Before
  public void setUp() {
    PermissionRepository repository = new PermissionRepository(dbClient, new Settings());
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    PermissionService permissionService = new PermissionService(dbClient, repository, issueAuthorizationIndexer, userSession, componentFinder);
    PermissionDependenciesFinder permissionDependenciesFinder = new PermissionDependenciesFinder(dbClient, componentFinder, new UserGroupFinder(dbClient), resourceTypes);

    BulkApplyTemplateAction underTest = new BulkApplyTemplateAction(dbClient, permissionService, permissionDependenciesFinder, i18n, resourceTypes);
    ws = new WsActionTester(underTest);

    user1 = userDb.insertUser(newUserDto().setLogin("user-login-1"));
    user2 = userDb.insertUser(newUserDto().setLogin("user-login-2"));
    group1 = groupDb.insertGroup(newGroupDto().setName("group-name-1"));
    group2 = groupDb.insertGroup(newGroupDto().setName("group-name-2"));

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

    commit();
  }

  @Test
  public void bulk_apply_template_by_template_uuid() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    ComponentDto view = componentDb.insertComponent(newView());
    ComponentDto developer = componentDb.insertComponent(newDeveloper("developer-name"));
    addUserPermissionToProject(user1, developer, UserRole.ADMIN);
    addUserPermissionToProject(user2, developer, UserRole.ADMIN);
    addGroupPermissionToProject(group1, developer, UserRole.ADMIN);
    addGroupPermissionToProject(group2, developer, UserRole.ADMIN);
    db.commit();

    call(ws.newRequest().setParam(PARAM_TEMPLATE_ID, template1.getUuid()));

    assertTemplate1AppliedToProject(project);
    assertTemplate1AppliedToProject(view);
    assertTemplate1AppliedToProject(developer);
  }

  @Test
  public void bulk_apply_template_by_template_name() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());

    call(ws.newRequest().setParam(PARAM_TEMPLATE_NAME, template1.getName()));

    assertTemplate1AppliedToProject(project);
  }

  @Test
  public void apply_template_by_qualifier() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    ComponentDto view = componentDb.insertComponent(newView());

    call(ws.newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_QUALIFIER, project.qualifier()));

    assertTemplate1AppliedToProject(project);
    assertNoPermissionOnProject(view);
  }

  @Test
  public void apply_template_by_query_on_name_and_key() {
    ComponentDto projectFoundByKey = newProjectDto().setKey("sonar");
    componentDb.insertProjectAndSnapshot(projectFoundByKey);
    ComponentDto projectFoundByName = newProjectDto().setName("name-sonar-name");
    componentDb.insertProjectAndSnapshot(projectFoundByName);
    // match must be exact on key
    ComponentDto projectUntouched = newProjectDto().setKey("new-sonar").setName("project-name");
    componentDb.insertProjectAndSnapshot(projectUntouched);
    componentDb.indexAllComponents();

    call(ws.newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "sonar"));

    assertTemplate1AppliedToProject(projectFoundByKey);
    assertTemplate1AppliedToProject(projectFoundByName);
    assertNoPermissionOnProject(projectUntouched);
  }

  @Test
  public void fail_if_no_template_parameter() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Template name or template id must be provided, not both.");

    call(ws.newRequest());
  }

  @Test
  public void fail_if_template_name_is_incorrect() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-template-uuid' is not found");

    call(ws.newRequest().setParam(PARAM_TEMPLATE_ID, "unknown-template-uuid"));
  }

  private void call(TestRequest request) {
    request.execute();
    db.commit();
  }

  private void assertTemplate1AppliedToProject(ComponentDto project) {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).extracting("name").containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).extracting("name").containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).extracting("login").containsExactly(user1.getLogin());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).extracting("login").containsExactly(user2.getLogin());
  }

  private void assertNoPermissionOnProject(ComponentDto project) {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.USER)).isEmpty();
  }

  private PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    return dbClient.permissionTemplateDao().insert(dbSession, template);
  }

  private void addUserToTemplate(UserDto user, PermissionTemplateDto permissionTemplate, String permission) {
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, permissionTemplate.getId(), user.getId(), permission);
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, permissionTemplate.getId(), group.getId(), permission);
  }

  private void addUserPermissionToProject(UserDto user, ComponentDto project, String permission) {
    dbClient.roleDao().insertUserRole(dbSession, new UserPermissionDto()
      .setPermission(permission)
      .setUserId(user.getId())
      .setComponentId(project.getId()));
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

  private static OldPermissionQuery query(String permission) {
    return OldPermissionQuery.builder().membership(IN).permission(permission).build();
  }

  private static class PermissionNotNull implements Predicate<GroupWithPermissionDto> {
    @Override
    public boolean apply(@Nullable GroupWithPermissionDto input) {
      return input.getPermission() != null;
    }
  }
}
