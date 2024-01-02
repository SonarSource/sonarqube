/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.project.ws;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;

public class UpdateVisibilityActionTest {
  private static final String PARAM_VISIBILITY = "visibility";
  private static final String PARAM_PROJECT = "project";
  private static final String PUBLIC = "public";
  private static final String PRIVATE = "private";

  private static final Set<String> GLOBAL_PERMISSIONS_NAME_SET = stream(GlobalPermission.values()).map(GlobalPermission::getKey)
    .collect(MoreCollectors.toSet(GlobalPermission.values().length));

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public final EsTester es = EsTester.createCustom(new FooIndexDefinition());
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final Set<String> PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER = permissionService.getAllProjectPermissions().stream()
    .filter(perm -> !perm.equals(UserRole.USER) && !perm.equals(UserRole.CODEVIEWER))
    .collect(MoreCollectors.toSet(permissionService.getAllProjectPermissions().size() - 2));

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private final Configuration configuration = mock(Configuration.class);

  private final UpdateVisibilityAction underTest = new UpdateVisibilityAction(dbClient, TestComponentFinder.from(dbTester),
    userSessionRule, projectIndexers, new SequenceUuidFactory(), configuration);
  private final WsActionTester ws = new WsActionTester(underTest);

  private final Random random = new Random();
  private final String randomVisibility = random.nextBoolean() ? PUBLIC : PRIVATE;
  private final TestRequest request = ws.newRequest();

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("update_visibility");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.since()).isEqualTo("6.4");
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project", "visibility");
  }

  @Test
  public void execute_fails_if_user_is_not_logged_in() {
    userSessionRule.anonymous();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void execute_fails_with_IAE_when_project_parameter_is_not_provided() {
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'project' parameter is missing");
  }

  @Test
  public void execute_fails_with_IAE_when_project_parameter_is_not_empty() {
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'project' parameter is missing");
  }

  @Test
  public void execute_fails_with_IAE_when_parameter_visibility_is_not_provided() {
    request.setParam(PARAM_PROJECT, "foo");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'visibility' parameter is missing");
  }

  @Test
  public void execute_fails_with_IAE_when_parameter_visibility_is_empty() {
    request.setParam(PARAM_PROJECT, "foo")
      .setParam(PARAM_VISIBILITY, "");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter '" + PARAM_VISIBILITY + "' () must be one of: [private, public]");
  }

  @Test
  public void execute_fails_with_IAE_when_value_of_parameter_visibility_is_not_lowercase() {
    request.setParam(PARAM_PROJECT, "foo");

    Stream.of("PUBLIC", "pUBliC", "PRIVATE", "PrIVAtE")
      .forEach(visibility -> {
        try {
          request.setParam(PARAM_VISIBILITY, visibility).execute();
          fail("An exception should have been raised");
        } catch (IllegalArgumentException e) {
          assertThat(e.getMessage()).isEqualTo(format("Value of parameter '%s' (%s) must be one of: [private, public]", PARAM_VISIBILITY, visibility));
        }
      });
  }

  @Test
  public void execute_fails_with_NotFoundException_when_specified_component_does_not_exist() {
    request.setParam(PARAM_PROJECT, "foo")
      .setParam(PARAM_VISIBILITY, randomVisibility);

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'foo' not found");
  }

  @Test
  public void execute_fails_with_BadRequestException_if_specified_component_is_neither_a_project_a_portfolio_nor_an_application() {
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto dir = ComponentTesting.newDirectory(project, "path");
    ComponentDto file = ComponentTesting.newFileDto(project);
    dbTester.components().insertComponents(module, dir, file);
    ComponentDto application = dbTester.components().insertPublicApplication();
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio();
    ComponentDto subView = ComponentTesting.newSubPortfolio(portfolio);
    ComponentDto projectCopy = newProjectCopy("foo", project, subView);
    dbTester.components().insertComponents(subView, projectCopy);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project, portfolio, application);

    Stream.of(project, portfolio, application).forEach(c -> request
      .setParam(PARAM_PROJECT, c.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility)
      .execute());

    Stream.of(module, dir, file, subView, projectCopy)
      .forEach(nonRootComponent -> {
        request.setParam(PARAM_PROJECT, nonRootComponent.getKey())
          .setParam(PARAM_VISIBILITY, randomVisibility);

        try {
          request.execute();
          fail("a BadRequestException should have been raised");
        } catch (BadRequestException e) {
          assertThat(e.getMessage()).isEqualTo("Component must be a project, a portfolio or an application");
        }
      });
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_no_permission_on_specified_component() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_all_permissions_but_ADMIN_on_specified_component() {
    ComponentDto project = dbTester.components().insertPublicProject();
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ISSUE_ADMIN, project);
    Arrays.stream(GlobalPermission.values())
      .forEach(userSessionRule::addPermission);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_ADMIN_permission_but_sonar_allowPermissionManagementForProjectAdmins_is_set_to_false() {
    when(configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)).thenReturn(of(false));
    ComponentDto project = dbTester.components().insertPublicProject();
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_global_ADMIN_permission_even_if_sonar_allowPermissionManagementForProjectAdmins_is_set_to_false() {
    when(configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)).thenReturn(of(false));
    ComponentDto project = dbTester.components().insertPublicProject();
    userSessionRule.setSystemAdministrator().addProjectPermission(UserRole.ADMIN, project);
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, "private");

    request.execute();

    assertThat(isPrivateInDb(project)).isTrue();
  }

  @Test
  public void execute_throws_BadRequestException_if_specified_component_has_pending_tasks() {
    ComponentDto project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertPendingTask(project));
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Test
  public void execute_throws_BadRequestException_if_main_component_of_specified_component_has_in_progress_tasks() {
    ComponentDto project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertInProgressTask(project));
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Test
  public void execute_changes_private_flag_of_specified_project_and_all_children_to_specified_new_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();

    BranchDto branchDto = ComponentTesting.newBranchDto(project);
    dbClient.branchDao().insert(dbSession, branchDto);

    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto dir = ComponentTesting.newDirectory(project, "path");
    ComponentDto file = ComponentTesting.newFileDto(project);

    dbTester.components().insertComponents(branch, module, dir, file);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PUBLIC : PRIVATE)
      .execute();

    assertThat(isPrivateInDb(project)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(branch)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(module)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(dir)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(file)).isEqualTo(!initiallyPrivate);
  }

  @Test
  public void execute_has_no_effect_if_specified_project_already_has_specified_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();

    BranchDto branchDto = ComponentTesting.newBranchDto(project);
    dbClient.branchDao().insert(dbSession, branchDto);

    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto)
      .setPrivate(initiallyPrivate);
    ComponentDto module = ComponentTesting.newModuleDto(project)
      .setPrivate(initiallyPrivate);
    ComponentDto dir = ComponentTesting.newDirectory(project, "path")
      // child is inconsistent with root (should not occur) and won't be fixed
      .setPrivate(!initiallyPrivate);
    ComponentDto file = ComponentTesting.newFileDto(project)
      .setPrivate(initiallyPrivate);
    dbTester.components().insertComponents(branch, module, dir, file);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PRIVATE : PUBLIC)
      .execute();

    assertThat(isPrivateInDb(project)).isEqualTo(initiallyPrivate);
    assertThat(isPrivateInDb(branch)).isEqualTo(initiallyPrivate);
    assertThat(isPrivateInDb(module)).isEqualTo(initiallyPrivate);
    assertThat(isPrivateInDb(dir)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(file)).isEqualTo(initiallyPrivate);
  }

  @Test
  public void execute_deletes_all_permissions_to_Anyone_on_specified_project_when_new_visibility_is_private() {
    ComponentDto project = dbTester.components().insertPublicProject();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(project, user, group);
  }

  @Test
  public void execute_does_not_delete_all_permissions_to_AnyOne_on_specified_project_if_already_private() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyStillHasAllPermissions(project, user, group);
  }

  @Test
  public void execute_deletes_all_permissions_USER_and_BROWSE_of_specified_project_when_new_visibility_is_public() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(project, user, group);
  }

  @Test
  public void execute_does_not_delete_permissions_USER_and_BROWSE_of_specified_project_when_new_component_is_already_public() {
    ComponentDto project = dbTester.components().insertPublicProject();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyStillHasAllPermissions(project, user, group);
  }

  @Test
  public void execute_updates_permission_of_specified_project_in_indexes_when_changing_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PUBLIC : PRIVATE)
      .execute();

    assertThat(projectIndexers.hasBeenCalled(project.uuid(), ProjectIndexer.Cause.PERMISSION_CHANGE)).isTrue();
  }

  @Test
  public void execute_does_not_update_permission_of_specified_project_in_indexes_if_already_has_specified_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PRIVATE : PUBLIC)
      .execute();

    assertThat(projectIndexers.hasBeenCalled(project.uuid())).isFalse();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_user_with_at_least_one_permission_when_making_project_private() {
    ComponentDto project = dbTester.components().insertPublicProject();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user1, "p1", project);
    dbTester.users().insertProjectPermissionOnUser(user1, "p2", project);
    dbTester.users().insertProjectPermissionOnUser(user2, "p2", project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user1.getUuid(), project.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p1", "p2");
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user2.getUuid(), project.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p2");
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user3.getUuid(), project.uuid()))
      .isEmpty();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_group_with_at_least_one_permission_when_making_project_private() {
    ComponentDto project = dbTester.components().insertPublicProject();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();
    dbTester.users().insertProjectPermissionOnGroup(group1, "p1", project);
    dbTester.users().insertProjectPermissionOnGroup(group1, "p2", project);
    dbTester.users().insertProjectPermissionOnGroup(group2, "p2", project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group1.getUuid(), project.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p1", "p2");
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group2.getUuid(), project.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p2");
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group3.getUuid(), project.uuid()))
      .isEmpty();
  }

  @Test
  public void update_a_portfolio_to_private() {
    ComponentDto portfolio = dbTester.components().insertPublicPortfolio();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN);
  }

  @Test
  public void update_a_portfolio_to_public() {
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio();
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.CODEVIEWER, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.ADMIN);
  }

  @Test
  public void update_an_application_to_private() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, application);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, application);
    userSessionRule.addProjectPermission(UserRole.ADMIN, application);

    request.setParam(PARAM_PROJECT, application.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, application.uuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), application.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), application.uuid()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN);
  }

  @Test
  public void update_an_application_to_public() {
    ComponentDto portfolio = dbTester.components().insertPrivateApplication();
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.CODEVIEWER, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), portfolio.uuid()))
      .containsOnly(UserRole.ADMIN);
  }

  private void unsafeGiveAllPermissionsToRootComponent(ComponentDto component, UserDto user, GroupDto group) {
    Arrays.stream(GlobalPermission.values())
      .forEach(globalPermission -> {
        dbTester.users().insertPermissionOnAnyone(globalPermission);
        dbTester.users().insertPermissionOnGroup(group, globalPermission);
        dbTester.users().insertPermissionOnUser(user, globalPermission);
      });
    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        unsafeInsertProjectPermissionOnAnyone(component, permission);
        unsafeInsertProjectPermissionOnGroup(component, group, permission);
        unsafeInsertProjectPermissionOnUser(component, user, permission);
      });
  }

  private void unsafeInsertProjectPermissionOnAnyone(ComponentDto component, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission)
      .setComponentUuid(component.uuid())
      .setComponentName(component.name());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto, component, null);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnGroup(ComponentDto component, GroupDto group, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(permission)
      .setComponentUuid(component.uuid())
      .setComponentName(component.name());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto, component, null);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnUser(ComponentDto component, UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission, user.getUuid(), component.uuid());
    dbTester.getDbClient().userPermissionDao().insert(dbTester.getSession(), dto, component, user, null);
    dbTester.commit();
  }

  private void verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, null, component.uuid()))
      .isEmpty();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), component.uuid()))
      .containsAll(permissionService.getAllProjectPermissions());
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), component.uuid()))
      .containsAll(permissionService.getAllProjectPermissions());
  }

  private void verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, null, component.uuid()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), component.uuid()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), component.uuid()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
  }

  private void verifyStillHasAllPermissions(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, null, component.uuid()))
      .containsAll(permissionService.getAllProjectPermissions());
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, group.getUuid(), component.uuid()))
      .containsAll(permissionService.getAllProjectPermissions());
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getUuid(), component.uuid()))
      .containsAll(permissionService.getAllProjectPermissions());
  }

  private void insertPendingTask(ComponentDto project) {
    insertCeQueueDto(project, CeQueueDto.Status.PENDING);
  }

  private void insertInProgressTask(ComponentDto project) {
    insertCeQueueDto(project, CeQueueDto.Status.IN_PROGRESS);
  }

  private int counter = 0;

  private void insertCeQueueDto(ComponentDto project, CeQueueDto.Status status) {
    dbClient.ceQueueDao().insert(dbTester.getSession(), new CeQueueDto()
      .setUuid("pending" + counter++)
      .setComponent(project)
      .setTaskType("foo")
      .setStatus(status));
    dbTester.commit();
  }

  private boolean isPrivateInDb(ComponentDto project) {
    return dbClient.componentDao().selectByUuid(dbTester.getSession(), project.uuid()).get().isPrivate();
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return random.nextBoolean() ? dbTester.components().insertPublicProject() : dbTester.components().insertPrivateProject();
  }
}
