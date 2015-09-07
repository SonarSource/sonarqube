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

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchProjectPermissionsActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  WsActionTester ws;
  I18nRule i18n = new I18nRule();
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  ResourceTypes resourceTypes = mock(ResourceTypes.class);
  SearchProjectPermissionsDataLoader dataLoader;

  SearchProjectPermissionsAction underTest;

  @Before
  public void setUp() {
    resourceTypes = mock(ResourceTypes.class);
    when(resourceTypes.getRoots()).thenReturn(rootResourceTypes());
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    PermissionDependenciesFinder finder = new PermissionDependenciesFinder(dbClient, componentFinder);
    i18n.setProjectPermissions();

    dataLoader = new SearchProjectPermissionsDataLoader(dbClient, finder, resourceTypes);
    underTest = new SearchProjectPermissionsAction(dbClient, userSession, i18n, dataLoader);

    ws = new WsActionTester(underTest);

    userSession.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Test
  public void search_project_permissions() {
    UserDto user1 = insertUser(newUserDto());
    UserDto user2 = insertUser(newUserDto());
    UserDto user3 = insertUser(newUserDto());

    ComponentDto jdk7 = insertJdk7();
    ComponentDto project2 = insertClang();
    ComponentDto dev = insertDeveloper();
    ComponentDto view = insertView();
    insertProjectInView(jdk7, view);

    insertUserRole(UserRole.ISSUE_ADMIN, user1.getId(), jdk7.getId());
    insertUserRole(UserRole.ADMIN, user1.getId(), jdk7.getId());
    insertUserRole(UserRole.ADMIN, user2.getId(), jdk7.getId());
    insertUserRole(UserRole.ADMIN, user3.getId(), jdk7.getId());
    insertUserRole(UserRole.ISSUE_ADMIN, user1.getId(), project2.getId());
    insertUserRole(UserRole.ISSUE_ADMIN, user1.getId(), dev.getId());
    insertUserRole(UserRole.ISSUE_ADMIN, user1.getId(), view.getId());
    // global permission
    insertUserRole(GlobalPermissions.SYSTEM_ADMIN, user1.getId(), null);

    GroupDto group1 = insertGroup(newGroupDto());
    GroupDto group2 = insertGroup(newGroupDto());
    GroupDto group3 = insertGroup(newGroupDto());

    insertGroupRole(UserRole.ADMIN, jdk7.getId(), null);
    insertGroupRole(UserRole.ADMIN, jdk7.getId(), group1.getId());
    insertGroupRole(UserRole.ADMIN, jdk7.getId(), group2.getId());
    insertGroupRole(UserRole.ADMIN, jdk7.getId(), group3.getId());
    insertGroupRole(UserRole.ADMIN, dev.getId(), group2.getId());
    insertGroupRole(UserRole.ADMIN, view.getId(), group2.getId());

    commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_project_permissions-example.json"));
  }

  @Test
  public void empty_result() {
    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("SearchProjectPermissionsActionTest/empty.json"));
  }

  @Test
  public void search_project_permissions_with_project_permission() {
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, "project-uuid");
    insertComponent(newProjectDto("project-uuid"));
    commit();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains("project-uuid");
  }

  @Test
  public void has_projects_ordered_by_name() {
    for (int i = 9; i >= 1; i--) {
      insertComponent(newProjectDto()
        .setName("project-name-" + i));
    }
    commit();

    String result = ws.newRequest()
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "3")
      .execute().getInput();

    assertThat(result)
      .contains("project-name-1", "project-name-2", "project-name-3")
      .doesNotContain("project-name-4");
  }

  @Test
  public void search_by_query_on_name() {
    insertComponent(newProjectDto().setName("project-name"));
    insertComponent(newProjectDto().setName("another-name"));
    commit();

    String result = ws.newRequest()
      .setParam(TEXT_QUERY, "project")
      .execute().getInput();

    assertThat(result).contains("project-name")
      .doesNotContain("another-name");
  }

  @Test
  public void search_by_query_on_key() {
    insertComponent(newProjectDto().setKey("project-key"));
    insertComponent(newProjectDto().setKey("another-key"));
    commit();

    String result = ws.newRequest()
      .setParam(TEXT_QUERY, "project")
      .execute().getInput();

    assertThat(result).contains("project-key")
      .doesNotContain("another-key");
  }

  @Test
  public void handle_more_than_1000_projects() {
    for (int i = 1; i <= 1001; i++) {
      insertComponent(newProjectDto("project-uuid-" + i));
    }
    commit();

    String result = ws.newRequest()
      .setParam(TEXT_QUERY, "project")
      .setParam(PAGE_SIZE, "1001")
      .execute().getInput();

    assertThat(result).contains("project-uuid-1", "project-uuid-999", "project-uuid-1001");
  }

  @Test
  public void result_depends_of_root_types() {
    ResourceType projectResourceType = ResourceType.builder(Qualifiers.PROJECT).build();
    when(resourceTypes.getRoots()).thenReturn(asList(projectResourceType));
    insertComponent(newView("view-uuid"));
    insertComponent(newDeveloper("developer-name"));
    insertComponent(newProjectDto("project-uuid"));
    commit();
    dataLoader = new SearchProjectPermissionsDataLoader(dbClient, new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient)), resourceTypes);
    underTest = new SearchProjectPermissionsAction(dbClient, userSession, i18n, dataLoader);
    ws = new WsActionTester(underTest);

    String result = ws.newRequest().execute().getInput();

    assertThat(result).contains("project-uuid")
      .doesNotContain("view-uuid")
      .doesNotContain("developer-name");
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_not_admin() {
    expectedException.expect(ForbiddenException.class);
    userSession.login();

    ws.newRequest().execute();
  }

  private ComponentDto insertView() {
    return insertComponent(newView()
      .setUuid("752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
      .setName("Java")
      .setKey("Java"));
  }

  private ComponentDto insertProjectInView(ComponentDto project, ComponentDto view) {
    return insertComponent(newProjectCopy("project-in-view-uuid", project, view));
  }

  private ComponentDto insertDeveloper() {
    return insertComponent(newDeveloper("Simon Brandhof")
      .setUuid("4e607bf9-7ed0-484a-946d-d58ba7dab2fb")
      .setKey("simon-brandhof"));
  }

  private ComponentDto insertClang() {
    return insertComponent(newProjectDto("project-uuid-2")
      .setName("Clang")
      .setKey("clang")
      .setUuid("ce4c03d6-430f-40a9-b777-ad877c00aa4d"));
  }

  private ComponentDto insertJdk7() {
    return insertComponent(newProjectDto("project-uuid-1")
      .setName("JDK 7")
      .setKey("net.java.openjdk:jdk7")
      .setUuid("0bd7b1e7-91d6-439e-a607-4a3a9aad3c6a"));
  }

  private UserDto insertUser(UserDto user) {
    return dbClient.userDao().insert(dbSession, user.setActive(true));
  }

  private void insertUserRole(String permission, long userId, @Nullable Long resourceId) {
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(permission)
      .setUserId(userId)
      .setResourceId(resourceId));
  }

  private GroupDto insertGroup(GroupDto group) {
    return dbClient.groupDao().insert(dbSession, group);
  }

  private void insertGroupRole(String permission, @Nullable Long resourceId, @Nullable Long groupId) {
    dbClient.roleDao().insertGroupRole(dbSession, new GroupRoleDto().setRole(permission).setResourceId(resourceId).setGroupId(groupId));
  }

  private ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component.setEnabled(true));
    return dbClient.componentDao().selectOrFailByUuid(dbSession, component.uuid());
  }

  private void commit() {
    dbSession.commit();
  }

  private static List<ResourceType> rootResourceTypes() {
    ResourceType project = ResourceType.builder(Qualifiers.PROJECT).build();
    ResourceType view = ResourceType.builder(Qualifiers.VIEW).build();
    ResourceType dev = ResourceType.builder("DEV").build();

    return asList(project, view, dev);
  }
}
