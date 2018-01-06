/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.OrganizationPermission;
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
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class UpdateVisibilityActionTest {
  private static final String PARAM_VISIBILITY = "visibility";
  private static final String PARAM_PROJECT = "project";
  private static final String PUBLIC = "public";
  private static final String PRIVATE = "private";
  private static final Set<String> ORGANIZATION_PERMISSIONS_NAME_SET = stream(OrganizationPermission.values()).map(OrganizationPermission::getKey)
    .collect(MoreCollectors.toSet(OrganizationPermission.values().length));
  private static final Set<String> PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER = ProjectPermissions.ALL.stream()
    .filter(perm -> !perm.equals(UserRole.USER) && !perm.equals(UserRole.CODEVIEWER)).collect(MoreCollectors.toSet(ProjectPermissions.ALL.size() - 2));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester esTester = new EsTester(new FooIndexDefinition());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone()
    .logIn();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private BillingValidationsProxy billingValidations = mock(BillingValidationsProxy.class);

  private ProjectsWsSupport wsSupport = new ProjectsWsSupport(dbClient, TestDefaultOrganizationProvider.from(dbTester), billingValidations);
  private UpdateVisibilityAction underTest = new UpdateVisibilityAction(dbClient, TestComponentFinder.from(dbTester), userSessionRule, projectIndexers, wsSupport);
  private WsActionTester ws = new WsActionTester(underTest);

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

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void execute_fails_with_IAE_when_project_parameter_is_not_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    request.execute();
  }

  @Test
  public void execute_fails_with_IAE_when_project_parameter_is_not_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    request.execute();
  }

  @Test
  public void execute_fails_with_IAE_when_parameter_visibility_is_not_provided() {
    request.setParam(PARAM_PROJECT, "foo");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'visibility' parameter is missing");

    request.execute();
  }

  @Test
  public void execute_fails_with_IAE_when_parameter_visibility_is_empty() {
    request.setParam(PARAM_PROJECT, "foo")
      .setParam(PARAM_VISIBILITY, "");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter '" + PARAM_VISIBILITY + "' () must be one of: [private, public]");

    request.execute();
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

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'foo' not found");

    request.execute();
  }

  @Test
  public void execute_fails_with_BadRequestException_if_specified_component_is_neither_a_project_a_portfolio_nor_an_application() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = randomPublicOrPrivateProject();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto dir = ComponentTesting.newDirectory(project, "path");
    ComponentDto file = ComponentTesting.newFileDto(project);
    dbTester.components().insertComponents(module, dir, file);
    ComponentDto application = dbTester.components().insertApplication(organization);
    ComponentDto portfolio = dbTester.components().insertView(organization);
    ComponentDto subView = ComponentTesting.newSubView(portfolio);
    ComponentDto projectCopy = newProjectCopy("foo", project, subView);
    dbTester.components().insertComponents(subView, projectCopy);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project, portfolio, application);

    Stream.of(project, portfolio, application).forEach(c -> request
      .setParam(PARAM_PROJECT, c.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility)
      .execute());

    Stream.of(module, dir, file, subView, projectCopy)
      .forEach(nonRootComponent -> {
        request.setParam(PARAM_PROJECT, nonRootComponent.getDbKey())
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
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);

    expectInsufficientPrivilegeException();

    request.execute();
  }

  @Test
  public void execute_throws_ForbiddenException_if_user_has_all_permissions_but_ADMIN_on_specified_component() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ISSUE_ADMIN, project);
    Arrays.stream(OrganizationPermission.values())
      .forEach(perm -> userSessionRule.addPermission(perm, organization));
    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);

    expectInsufficientPrivilegeException();

    request.execute();
  }

  @Test
  public void execute_throws_BadRequestException_if_specified_component_has_pending_tasks() {
    ComponentDto project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertPendingTask(project));
    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");

    request.execute();
  }

  @Test
  public void execute_throws_BadRequestException_if_specified_component_has_in_progress_tasks() {
    ComponentDto project = randomPublicOrPrivateProject();
    IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .forEach(i -> insertInProgressTask(project));
    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, randomVisibility);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");

    request.execute();
  }

  @Test
  public void execute_throws_ISE_when_project_organization_uuid_does_not_match_existing_organization() {
    // Organization is not persisted
    OrganizationDto organization = newOrganizationDto();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Could not find organization with uuid '%s' of project '%s'", organization.getUuid(), project.getDbKey()));

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();
  }

  @Test
  public void execute_changes_private_flag_of_specified_project_and_all_children_to_specified_new_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();

    BranchDto branchDto = ComponentTesting.newBranchDto(project);
    dbClient.branchDao().insert(dbSession, branchDto);

    ComponentDto branch = ComponentTesting.newProjectBranch(project, branchDto);
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto dir = ComponentTesting.newDirectory(project, "path");
    ComponentDto file = ComponentTesting.newFileDto(project);

    dbTester.components().insertComponents(branch, module, dir, file);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
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

    ComponentDto branch = ComponentTesting.newProjectBranch(project, branchDto)
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

    request.setParam(PARAM_PROJECT, project.getDbKey())
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
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup(organization);
    unsafeGiveAllPermissionsToRootComponent(project, user, group, organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(project, user, group);
  }

  @Test
  public void execute_does_not_delete_all_permissions_to_AnyOne_on_specified_project_if_already_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup(organization);
    unsafeGiveAllPermissionsToRootComponent(project, user, group, organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    verifyStillHasAllPermissions(project, user, group);
  }

  @Test
  public void execute_deletes_all_permissions_USER_and_BROWSE_of_specified_project_when_new_visibility_is_public() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup(organization);
    unsafeGiveAllPermissionsToRootComponent(project, user, group, organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(project, user, group);
  }

  @Test
  public void execute_does_not_delete_permissions_USER_and_BROWSE_of_specified_project_when_new_component_is_already_public() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    UserDto user = dbTester.users().insertUser();
    GroupDto group = dbTester.users().insertGroup(organization);
    unsafeGiveAllPermissionsToRootComponent(project, user, group, organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    verifyStillHasAllPermissions(project, user, group);
  }

  @Test
  public void execute_updates_permission_of_specified_project_in_indexes_when_changing_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PUBLIC : PRIVATE)
      .execute();

    assertThat(projectIndexers.hasBeenCalled(project.uuid(), ProjectIndexer.Cause.PERMISSION_CHANGE)).isTrue();
  }

  @Test
  public void execute_does_not_update_permission_of_specified_project_in_indexes_if_already_has_specified_visibility() {
    ComponentDto project = randomPublicOrPrivateProject();
    boolean initiallyPrivate = project.isPrivate();
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, initiallyPrivate ? PRIVATE : PUBLIC)
      .execute();

    assertThat(projectIndexers.hasBeenCalled(project.uuid())).isFalse();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_user_with_at_least_one_permission_when_making_project_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user1, "p1", project);
    dbTester.users().insertProjectPermissionOnUser(user1, "p2", project);
    dbTester.users().insertProjectPermissionOnUser(user2, "p2", project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user1.getId(), project.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p1", "p2");
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user2.getId(), project.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p2");
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user3.getId(), project.getId()))
      .isEmpty();
  }

  @Test
  public void execute_grants_USER_and_CODEVIEWER_permissions_to_any_group_with_at_least_one_permission_when_making_project_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    GroupDto group1 = dbTester.users().insertGroup(organization);
    GroupDto group2 = dbTester.users().insertGroup(organization);
    GroupDto group3 = dbTester.users().insertGroup(organization);
    dbTester.users().insertProjectPermissionOnGroup(group1, "p1", project);
    dbTester.users().insertProjectPermissionOnGroup(group1, "p2", project);
    dbTester.users().insertProjectPermissionOnGroup(group2, "p2", project);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group1.getId(), project.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p1", "p2");
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group2.getId(), project.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, "p2");
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group3.getId(), project.getId()))
      .isEmpty();
  }

  @Test
  public void update_a_portfolio_to_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto portfolio = dbTester.components().insertPublicPortfolio(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), portfolio.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), portfolio.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN);
  }

  @Test
  public void update_a_portfolio_to_public() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio(organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.CODEVIEWER, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), portfolio.getId()))
      .containsOnly(UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), portfolio.getId()))
      .containsOnly(UserRole.ADMIN);
  }

  @Test
  public void update_an_application_to_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto application = dbTester.components().insertPublicApplication(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, application);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, application);
    userSessionRule.addProjectPermission(UserRole.ADMIN, application);

    request.setParam(PARAM_PROJECT, application.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, application.uuid()).get().isPrivate()).isTrue();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), application.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), application.getId()))
      .containsOnly(UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN);
  }

  @Test
  public void update_an_application_to_public() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto portfolio = dbTester.components().insertPrivateApplication(organization);
    userSessionRule.addProjectPermission(UserRole.ADMIN, portfolio);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnGroup(group, UserRole.CODEVIEWER, portfolio);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.USER, portfolio);
    dbTester.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, portfolio);

    request.setParam(PARAM_PROJECT, portfolio.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();

    assertThat(dbClient.componentDao().selectByUuid(dbSession, portfolio.uuid()).get().isPrivate()).isFalse();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, organization.getUuid(), group.getId(), portfolio.getId()))
      .containsOnly(UserRole.ISSUE_ADMIN);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), portfolio.getId()))
      .containsOnly(UserRole.ADMIN);
  }


  @Test
  public void fail_to_update_visibility_to_private_when_organization_is_not_allowed_to_use_private_projects() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    dbTester.organizations().setNewProjectPrivate(organization, true);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);
    doThrow(new BillingValidations.BillingValidationsException("This organization cannot use project private")).when(billingValidations)
      .checkCanUpdateProjectVisibility(any(BillingValidations.Organization.class), eq(true));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("This organization cannot use project private");

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PRIVATE)
      .execute();
  }

  @Test
  public void does_not_fail_to_update_visibility_to_public_when_organization_is_not_allowed_to_use_private_projects() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    dbTester.organizations().setNewProjectPrivate(organization, true);
    userSessionRule.addProjectPermission(UserRole.ADMIN, project);
    doThrow(new BillingValidations.BillingValidationsException("This organization cannot use project private")).when(billingValidations)
      .checkCanUpdateProjectVisibility(any(BillingValidations.Organization.class), eq(true));

    request.setParam(PARAM_PROJECT, project.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = dbTester.components().insertMainBranch();
    userSessionRule.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = dbTester.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component key '%s' not found", branch.getDbKey()));

    request.setParam(PARAM_PROJECT, branch.getDbKey())
      .setParam(PARAM_VISIBILITY, PUBLIC)
      .execute();
  }

  private void unsafeGiveAllPermissionsToRootComponent(ComponentDto component, UserDto user, GroupDto group, OrganizationDto organization) {
    Arrays.stream(OrganizationPermission.values())
      .forEach(organizationPermission -> {
        dbTester.users().insertPermissionOnAnyone(organization, organizationPermission);
        dbTester.users().insertPermissionOnGroup(group, organizationPermission);
        dbTester.users().insertPermissionOnUser(organization, user, organizationPermission);
      });
    ProjectPermissions.ALL
      .forEach(permission -> {
        unsafeInsertProjectPermissionOnAnyone(component, permission);
        unsafeInsertProjectPermissionOnGroup(component, group, permission);
        unsafeInsertProjectPermissionOnUser(component, user, permission);
      });
  }

  private void unsafeInsertProjectPermissionOnAnyone(ComponentDto component, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(component.getOrganizationUuid())
      .setGroupId(null)
      .setRole(permission)
      .setResourceId(component.getId());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnGroup(ComponentDto component, GroupDto group, String permission) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(group.getOrganizationUuid())
      .setGroupId(group.getId())
      .setRole(permission)
      .setResourceId(component.getId());
    dbTester.getDbClient().groupPermissionDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  private void unsafeInsertProjectPermissionOnUser(ComponentDto component, UserDto user, String permission) {
    UserPermissionDto dto = new UserPermissionDto(component.getOrganizationUuid(), permission, user.getId(), component.getId());
    dbTester.getDbClient().userPermissionDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  private void verifyHasAllPermissionsButProjectPermissionsToGroupAnyOne(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), component.getOrganizationUuid()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null, component.getId()))
      .isEmpty();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId(), component.getId()))
      .containsAll(ProjectPermissions.ALL);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), component.getId()))
      .containsAll(ProjectPermissions.ALL);
  }

  private void verifyHasAllPermissionsButProjectPermissionsUserAndBrowse(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), component.getOrganizationUuid()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null, component.getId()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId(), component.getId()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), component.getId()))
      .doesNotContain(UserRole.USER)
      .doesNotContain(UserRole.CODEVIEWER)
      .containsAll(PROJECT_PERMISSIONS_BUT_USER_AND_CODEVIEWER);
  }

  private void verifyStillHasAllPermissions(ComponentDto component, UserDto user, GroupDto group) {
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectGlobalPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), component.getOrganizationUuid()))
      .containsAll(ORGANIZATION_PERMISSIONS_NAME_SET);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), null, component.getId()))
      .containsAll(ProjectPermissions.ALL);
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession, component.getOrganizationUuid(), group.getId(), component.getId()))
      .containsAll(ProjectPermissions.ALL);
    assertThat(dbClient.userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), component.getId()))
      .containsAll(ProjectPermissions.ALL);
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
      .setComponentUuid(project.uuid())
      .setTaskType("foo")
      .setStatus(status));
    dbTester.commit();
  }

  private void expectInsufficientPrivilegeException() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
  }

  private boolean isPrivateInDb(ComponentDto project) {
    return dbClient.componentDao().selectByUuid(dbTester.getSession(), project.uuid()).get().isPrivate();
  }

  private ComponentDto randomPublicOrPrivateProject() {
    OrganizationDto organization = dbTester.organizations().insert();
    return random.nextBoolean() ? dbTester.components().insertPublicProject(organization) : dbTester.components().insertPrivateProject(organization);
  }
}
