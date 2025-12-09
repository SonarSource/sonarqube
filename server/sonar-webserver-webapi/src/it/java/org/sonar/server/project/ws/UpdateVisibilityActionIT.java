/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.project.Visibility;
import org.sonar.server.project.VisibilityService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;

public class UpdateVisibilityActionIT {
  private static final String PARAM_VISIBILITY = "visibility";
  private static final String PARAM_PROJECT = "project";
  private static final String PUBLIC = "public";
  private static final String PRIVATE = "private";
  private static final String MANAGED_PROJECT_KEY = "managed_project_key";

  private static final Set<String> GLOBAL_PERMISSIONS_NAME_SET = stream(GlobalPermission.values()).map(GlobalPermission::getKey)
    .collect(Collectors.toSet());

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public final EsTester es = EsTester.createCustom(new FooIndexDefinition());
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final Set<String> PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER = permissionService.getAllProjectPermissions().stream()
    .filter(perm -> !perm.equals(ProjectPermission.USER) && !perm.equals(ProjectPermission.CODEVIEWER))
    .map(ProjectPermission::getKey)
    .collect(Collectors.toSet());

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final TestIndexers projectIndexers = new TestIndexers();
  private final Configuration configuration = mock(Configuration.class);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final VisibilityService visibilityService = new VisibilityService(dbClient, projectIndexers, new SequenceUuidFactory());
  private final UpdateVisibilityAction underTest = new UpdateVisibilityAction(dbClient, userSessionRule, configuration, visibilityService, managedInstanceChecker);
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
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component must be a project, a portfolio or an application");
  }

  @Test
  public void execute_fails_with_BadRequestException_if_specified_component_is_neither_a_project_a_portfolio_nor_an_application() {
    ProjectData project = randomPublicOrPrivateProject();
    ComponentDto dir = ComponentTesting.newDirectory(project.getMainBranchComponent(), "path");
    ComponentDto file = ComponentTesting.newFileDto(project.getMainBranchComponent());
    dbTester.components().insertComponents(dir, file);
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    PortfolioData portfolio = dbTester.components().insertPrivatePortfolioData();
    ComponentDto subView = ComponentTesting.newSubPortfolio(portfolio.getRootComponent());
    ComponentDto projectCopy = newProjectCopy("foo", project.getMainBranchComponent(), subView);
    dbTester.components().insertComponents(subView, projectCopy);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto(), application);
    userSessionRule.addPortfolioPermission(ProjectPermission.ADMIN, portfolio.getPortfolioDto());

    Stream.of(project.getProjectDto(), portfolio.getPortfolioDto(), application).forEach(c -> request
      .setParam(PARAM_PROJECT, c.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility)
      .execute());

    Stream.of(dir, file, subView, projectCopy)
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
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_all_permissions_but_ADMIN_on_specified_component() {
    ProjectData project = dbTester.components().insertPublicProject();
    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(ProjectPermission.ISSUE_ADMIN, project.getProjectDto());
    Arrays.stream(GlobalPermission.values())
      .forEach(userSessionRule::addPermission);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_ADMIN_permission_but_sonar_allowPermissionManagementForProjectAdmins_is_set_to_false() {
    when(configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)).thenReturn(of(false));
    ProjectData project = dbTester.components().insertPublicProject();
    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_global_ADMIN_permission_even_if_sonar_allowPermissionManagementForProjectAdmins_is_set_to_false() {
    when(configuration.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)).thenReturn(of(false));
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    userSessionRule.setSystemAdministrator().addProjectPermission(ProjectPermission.ADMIN, project);
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, "private");

    request.execute();

    assertThat(dbClient.projectDao().selectProjectByKey(dbSession, project.getKey()).get().isPrivate()).isTrue();
  }

  @Test
  public void execute_throws_IllegalStateException_if_specified_component_has_pending_tasks() {
    ProjectData project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertPendingTask(project.getMainBranchDto()));
    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Test
  public void execute_throws_IllegalStateException_if_main_component_of_specified_component_has_in_progress_tasks() {
    ProjectData project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertInProgressTask(project.getMainBranchDto()));
    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  @Test
  public void execute_throws_if_project_is_managed() {
    ProjectDto project = dbTester.components().insertPublicProject(p -> p.setKey(MANAGED_PROJECT_KEY)).getProjectDto();
    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, Visibility.PUBLIC.getLabel());
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    doThrow(new IllegalStateException("Managed project")).when(managedInstanceChecker).throwIfProjectIsManaged(any(), eq(project.getUuid()));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Managed project");
  }

  @Test
  public void execute_changes_private_flag_of_specified_project_and_all_children_to_specified_new_visibility() {
    ProjectData project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.getProjectDto().isPrivate();

    BranchDto branchDto = ComponentTesting.newBranchDto(project.projectUuid(), BranchType.BRANCH);
    dbClient.branchDao().insert(dbSession, branchDto);

    ComponentDto branch = ComponentTesting.newBranchComponent(project.getProjectDto(), branchDto);
    ComponentDto dir = ComponentTesting.newDirectory(project.getMainBranchComponent(), "path");
    ComponentDto file = ComponentTesting.newFileDto(project.getMainBranchComponent());

    dbTester.components().insertComponents(branch, dir, file);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PUBLIC : PRIVATE)
      .execute();

    assertThat(dbClient.projectDao().selectProjectByKey(dbSession, project.projectKey()).get().isPrivate()).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(branch)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(dir)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(file)).isEqualTo(!initiallyPrivate);
  }

  @Test
  public void execute_has_no_effect_if_specified_project_already_has_specified_visibility() {
    ProjectData project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.getProjectDto().isPrivate();

    BranchDto branchDto = ComponentTesting.newBranchDto(project.getMainBranchComponent());
    dbClient.branchDao().insert(dbSession, branchDto);

    ComponentDto branch = ComponentTesting.newBranchComponent(project.getProjectDto(), branchDto)
      .setPrivate(initiallyPrivate);
    ComponentDto dir = ComponentTesting.newDirectory(project.getMainBranchComponent(), "path")
      // child is inconsistent with root (should not occur) and won't be fixed
      .setPrivate(!initiallyPrivate);
    ComponentDto file = ComponentTesting.newFileDto(project.getMainBranchComponent())
      .setPrivate(initiallyPrivate);
    dbTester.components().insertComponents(branch, dir, file);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PRIVATE : PUBLIC)
      .execute();

    assertThat(isPrivateInDb(project.getMainBranchComponent())).isEqualTo(initiallyPrivate);
    assertThat(isPrivateInDb(branch)).isEqualTo(initiallyPrivate);
    assertThat(isPrivateInDb(dir)).isEqualTo(!initiallyPrivate);
    assertThat(isPrivateInDb(file)).isEqualTo(initiallyPrivate);
  }

  @Test
  public void execute_deletes_all_permissions_to_Anyone_on_specified_project_when_new_visibility_is_private() {
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(project.getUuid(), user, group);
  }

  @Test
  public void execute_does_not_delete_all_permissions_to_AnyOne_on_specified_project_if_already_private() {
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyStillHasAllPermissions(project.getUuid(), user, group);
  }

  @Test
  public void execute_deletes_all_permissions_USER_and_BROWSE_of_specified_project_when_new_visibility_is_public() {
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(project.getUuid(), user, group);
  }

  @Test
  public void execute_does_not_delete_permissions_USER_and_BROWSE_of_specified_project_when_new_component_is_already_public() {
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup();
    unsafeGiveAllPermissionsToRootComponent(project, user, group);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyStillHasAllPermissions(project.getUuid(), user, group);
  }

  @Test
  public void execute_updates_permission_of_specified_project_in_indexes_when_changing_visibility() {
    ProjectData project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.getProjectDto().isPrivate();
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PUBLIC : PRIVATE)
      .execute();

    assertThat(projectIndexers.hasBeenCalledForEntity(project.projectUuid(), Indexers.EntityEvent.PERMISSION_CHANGE)).isTrue();
  }

  @Test
  public void execute_does_not_update_permission_of_specified_project_in_indexes_if_already_has_specified_visibility() {
    ProjectData project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.getProjectDto().isPrivate();
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    request.setParam(PARAM_PROJECT, project.projectKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PRIVATE : PUBLIC)
      .execute();

    assertThat(projectIndexers.hasBeenCalledForEntity(project.projectUuid())).isFalse();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_user_with_at_least_one_permission_when_making_project_private() {
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user1, "p1", project);
    dbTester.users().insertProjectPermissionOnUser(user1, "p2", project);
    dbTester.users().insertProjectPermissionOnUser(user2, "p2", project);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user1.getUuid(), project.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), "p1", "p2");
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user2.getUuid(), project.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), "p2");
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user3.getUuid(), project.getUuid()))
      .isEmpty();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_group_with_at_least_one_permission_when_making_project_private() {
    ProjectDto project = dbTester.components().insertPublicProject().getProjectDto();
    GroupDto group1 = dbTester.users().insertGroup();
    GroupDto group2 = dbTester.users().insertGroup();
    GroupDto group3 = dbTester.users().insertGroup();
    dbTester.users().insertEntityPermissionOnGroup(group1, "p1", project);
    dbTester.users().insertEntityPermissionOnGroup(group1, "p2", project);
    dbTester.users().insertEntityPermissionOnGroup(group2, "p2", project);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group1.getUuid(), project.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), "p1", "p2");
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group2.getUuid(), project.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), "p2");
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group3.getUuid(), project.getUuid()))
      .isEmpty();
  }

  @Test
  public void update_a_portfolio_to_private() {
    PortfolioDto portfolio = dbTester.components().insertPublicPortfolioDto();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.ISSUE_ADMIN, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, portfolio);
    userSessionRule.addPortfolioPermission(ProjectPermission.ADMIN, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.portfolioDao().selectByUuid(dbSession, portfolio.getUuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), portfolio.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), ProjectPermission.ISSUE_ADMIN.getKey());
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), portfolio.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), ProjectPermission.ADMIN.getKey());
  }

  @Test
  public void update_a_portfolio_to_public() {
    PortfolioDto portfolio = dbTester.components().insertPrivatePortfolioDto();
    userSessionRule.addPortfolioPermission(ProjectPermission.ADMIN, portfolio);
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.ISSUE_ADMIN, portfolio);
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.USER, portfolio);
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.CODEVIEWER, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.CODEVIEWER, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.getUuid()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), portfolio.getUuid()))
      .containsOnly(ProjectPermission.ISSUE_ADMIN.getKey());
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), portfolio.getUuid()))
      .containsOnly(ProjectPermission.ADMIN.getKey());
  }

  @Test
  public void update_an_application_to_private() {
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.ISSUE_ADMIN, application);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, application);
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, application);

    request.setParam(PARAM_PROJECT, application.getKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.projectDao().selectByUuid(dbSession, application.getUuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), application.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), ProjectPermission.ISSUE_ADMIN.getKey());
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), application.getUuid()))
      .containsOnly(ProjectPermission.USER.getKey(), ProjectPermission.CODEVIEWER.getKey(), ProjectPermission.ADMIN.getKey());
  }

  @Test
  public void update_an_application_to_public() {
    ProjectDto application = dbTester.components().insertPrivateApplication().getProjectDto();
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, application);
    GroupDto group = dbTester.users().insertGroup();
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.ISSUE_ADMIN, application);
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.USER, application);
    dbTester.users().insertEntityPermissionOnGroup(group, ProjectPermission.CODEVIEWER, application);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, application);
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, application);
    dbTester.users().insertProjectPermissionOnUser(user, ProjectPermission.CODEVIEWER, application);

    request.setParam(PARAM_PROJECT, application.getKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.projectDao().selectApplicationByKey(dbSession, application.getKey()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), application.getUuid()))
      .containsOnly(ProjectPermission.ISSUE_ADMIN.getKey());
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), application.getUuid()))
      .containsOnly(ProjectPermission.ADMIN.getKey());
  }

  private void unsafeGiveAllPermissionsToRootComponent(ProjectDto projectDto, UserDto user, GroupDto group) {
    Arrays.stream(GlobalPermission.values())
      .forEach(globalPermission -> {
        dbTester.users().insertPermissionOnAnyone(globalPermission);
        dbTester.users().insertPermissionOnGroup(group, globalPermission);
        dbTester.users().insertGlobalPermissionOnUser(user, globalPermission);
      });
    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        unsafeInsertProjectPermissionOnAnyone(projectDto, permission);
        unsafeInsertProjectPermissionOnGroup(projectDto, group, permission);
        unsafeInsertProjectPermissionOnUser(projectDto, user, permission);
      });
  }

  private void unsafeInsertProjectPermissionOnAnyone(ProjectDto projectDto, ProjectPermission permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(permission)
      .setEntityUuid(projectDto.getUuid())
      .setEntityName(projectDto.getName());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto, projectDto, null);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnGroup(ProjectDto projectDto, GroupDto group, ProjectPermission permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(permission)
      .setEntityUuid(projectDto.getUuid())
      .setEntityName(projectDto.getName());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto, projectDto, null);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnUser(ProjectDto component, UserDto user, ProjectPermission permission) {
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), permission.getKey(), user.getUuid(), component.getUuid());
    dbTester.getDbClient().userPermissionDao().insert(dbTester.getSession(), dto, component, user, null);
    dbTester.commit();
  }

  private void verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(String projectUuid, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, null, projectUuid))
      .isEmpty();
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), projectUuid))
      .containsAll(permissionService.getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toSet()));
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), projectUuid))
      .containsAll(permissionService.getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toSet()));
  }

  private void verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(String projectUuid, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, null, projectUuid))
      .doesNotContain(ProjectPermission.USER.getKey())
      .doesNotContain(ProjectPermission.CODEVIEWER.getKey())
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), projectUuid))
      .doesNotContain(ProjectPermission.USER.getKey())
      .doesNotContain(ProjectPermission.CODEVIEWER.getKey())
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), projectUuid))
      .doesNotContain(ProjectPermission.USER.getKey())
      .doesNotContain(ProjectPermission.CODEVIEWER.getKey())
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
  }

  private void verifyStillHasAllPermissions(String projectUuid, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, null))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, group.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid()))
      .containsAll(GLOBAL_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, null, projectUuid))
      .containsAll(permissionService.getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toSet()));
    assertThat(dbClient.groupPermissionDao().selectEntityPermissionsOfGroup(dbSession, group.getUuid(), projectUuid))
      .containsAll(permissionService.getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toSet()));
    assertThat(dbClient.userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), projectUuid))
      .containsAll(permissionService.getAllProjectPermissions().stream().map(ProjectPermission::getKey).collect(Collectors.toSet()));
  }

  private void insertPendingTask(BranchDto branch) {
    insertCeQueueDto(branch, CeQueueDto.Status.PENDING);
  }

  private void insertInProgressTask(BranchDto branch) {
    insertCeQueueDto(branch, CeQueueDto.Status.IN_PROGRESS);
  }

  private int counter = 0;

  private void insertCeQueueDto(BranchDto branch, CeQueueDto.Status status) {
    dbClient.ceQueueDao().insert(dbTester.getSession(), new CeQueueDto()
      .setUuid("pending" + counter++)
      .setComponentUuid(branch.getUuid())
      .setEntityUuid(branch.getProjectUuid())
      .setTaskType("foo")
      .setStatus(status));
    dbTester.commit();
  }

  private boolean isPrivateInDb(ComponentDto component) {
    return dbClient.componentDao().selectByUuid(dbTester.getSession(), component.uuid()).get().isPrivate();
  }

  private ProjectData randomPublicOrPrivateProject() {
    return random.nextBoolean() ? dbTester.components().insertPublicProject() : dbTester.components().insertPrivateProject();
  }

}
