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
package org.sonar.server.user;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newChildComponent;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class ServerUserSessionTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = db.getDbClient();

  @Test
  public void anonymous_is_not_logged_in_and_does_not_have_login() {
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.getUuid()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void shouldResetPassword_is_false_on_anonymous() {
    assertThat(newAnonymousSession().shouldResetPassword()).isFalse();
  }

  @Test
  public void shouldResetPassword_is_false_if_set_on_UserDto() {
    UserDto user = db.users().insertUser(userDto -> userDto.setResetPassword(false));
    assertThat(newUserSession(user).shouldResetPassword()).isFalse();
  }

  @Test
  public void shouldResetPassword_is_true_if_set_on_UserDto() {
    UserDto user = db.users().insertUser(userDto -> userDto.setResetPassword(true));
    assertThat(newUserSession(user).shouldResetPassword()).isTrue();
  }

  @Test
  public void getGroups_is_empty_on_anonymous() {
    assertThat(newAnonymousSession().getGroups()).isEmpty();
  }

  @Test
  public void getGroups_is_empty_if_user_is_not_member_of_any_group() {
    UserDto user = db.users().insertUser();
    assertThat(newUserSession(user).getGroups()).isEmpty();
  }

  @Test
  public void getGroups_returns_the_groups_of_logged_in_user() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    assertThat(newUserSession(user).getGroups()).extracting(GroupDto::getUuid).containsOnly(group1.getUuid(), group2.getUuid());
  }

  @Test
  public void getLastSonarlintConnectionDate() {
    UserDto user = db.users().insertUser(p -> p.setLastSonarlintConnectionDate(1000L));
    assertThat(newUserSession(user).getLastSonarlintConnectionDate()).isEqualTo(1000L);
  }

  @Test
  public void getGroups_keeps_groups_in_cache() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user);

    ServerUserSession session = newUserSession(user);
    assertThat(session.getGroups()).extracting(GroupDto::getUuid).containsOnly(group1.getUuid());

    // membership updated but not cache
    db.users().insertMember(group2, user);
    assertThat(session.getGroups()).extracting(GroupDto::getUuid).containsOnly(group1.getUuid());
  }

  @Test
  public void isActive_redirectsValueFromUserDto() {
    UserDto active = db.users().insertUser();
    active.setActive(true);
    assertThat(newUserSession(active).isActive()).isTrue();

    UserDto notActive = db.users().insertUser();
    notActive.setActive(false);
    assertThat(newUserSession(notActive).isActive()).isFalse();
  }

  @Test
  public void checkComponentUuidPermission_fails_with_FE_when_user_has_not_permission_for_specified_uuid_in_db() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, USER, project);
    UserSession session = newUserSession(user);

    assertThatForbiddenExceptionIsThrown(() -> session.checkComponentUuidPermission(USER, "another-uuid"));
  }

  @Test
  public void checkChildProjectsPermission_succeeds_if_user_has_permissions_on_all_application_child_projects() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, USER, project);
    ComponentDto application = db.components().insertPrivateApplication();
    db.components().addApplicationProject(application, project);

    UserSession underTest = newUserSession(user);

    assertThat(underTest.checkChildProjectsPermission(USER, application)).isSameAs(underTest);
  }

  @Test
  public void checkChildProjectsPermission_succeeds_if_component_is_not_an_application() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();

    UserSession underTest = newUserSession(user);

    assertThat(underTest.checkChildProjectsPermission(USER, project)).isSameAs(underTest);
  }

  @Test
  public void checkChildProjectsPermission_fails_with_FE_when_user_has_not_permission_for_specified_uuid_in_db() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto application = db.components().insertPrivateApplication();
    db.components().addApplicationProject(application, project);
    //add computed project
    db.components().insertComponent(newProjectCopy(project, application));

    UserSession underTest = newUserSession(user);

    assertThatForbiddenExceptionIsThrown(() -> underTest.checkChildProjectsPermission(USER, application));
  }

  @Test
  public void checkPermission_throws_ForbiddenException_when_user_doesnt_have_the_specified_permission() {
    UserDto user = db.users().insertUser();

    assertThatForbiddenExceptionIsThrown(() -> newUserSession(user).checkPermission(PROVISION_PROJECTS));
  }

  @Test
  public void checkPermission_succeeds_when_user_has_the_specified_permission() {
    UserDto adminUser = db.users().insertAdminByUserPermission();
    db.users().insertPermissionOnUser(adminUser, PROVISIONING);

    newUserSession(adminUser).checkPermission(PROVISION_PROJECTS);
  }

  @Test
  public void test_hasPermission_for_logged_in_user() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, PROVISION_PROJECTS);
    db.users().insertProjectPermissionOnUser(user, ADMIN, project);

    UserSession session = newUserSession(user);
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();
    assertThat(session.hasPermission(ADMINISTER)).isFalse();
  }

  @Test
  public void test_hasPermission_for_anonymous_user() {
    db.users().insertPermissionOnAnyone(PROVISION_PROJECTS);

    UserSession session = newAnonymousSession();
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();
    assertThat(session.hasPermission(ADMINISTER)).isFalse();
  }

  @Test
  public void hasPermission_keeps_cache_of_permissions_of_logged_in_user() {
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, PROVISIONING);

    UserSession session = newUserSession(user);

    // feed the cache
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();

    // change permissions without updating the cache
    db.users().deletePermissionFromUser(user, PROVISION_PROJECTS);
    db.users().insertPermissionOnUser(user, SCAN);
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();
    assertThat(session.hasPermission(ADMINISTER)).isFalse();
    assertThat(session.hasPermission(SCAN)).isFalse();
  }

  @Test
  public void hasPermission_keeps_cache_of_permissions_of_anonymous_user() {
    db.users().insertPermissionOnAnyone(PROVISION_PROJECTS);

    UserSession session = newAnonymousSession();

    // feed the cache
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();

    // change permissions without updating the cache
    db.users().insertPermissionOnAnyone(SCAN);
    assertThat(session.hasPermission(PROVISION_PROJECTS)).isTrue();
    assertThat(session.hasPermission(SCAN)).isFalse();
  }

  @Test
  public void test_hasChildProjectsPermission_for_logged_in_user() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project1);

    ComponentDto application = db.components().insertPrivateApplication();
    db.components().addApplicationProject(application, project1);
    // add computed project
    db.components().insertComponent(newProjectCopy(project1, application));

    UserSession session = newUserSession(user);
    assertThat(session.hasChildProjectsPermission(USER, application)).isTrue();

    db.components().addApplicationProject(application, project2);
    db.components().insertComponent(newProjectCopy(project2, application));

    assertThat(session.hasChildProjectsPermission(USER, application)).isFalse();
  }

  @Test
  public void test_hasChildProjectsPermission_for_anonymous_user() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnAnyone(USER);
    ComponentDto application = db.components().insertPrivateApplication();
    db.components().addApplicationProject(application, project);
    // add computed project
    db.components().insertComponent(newProjectCopy(project, application));

    UserSession session = newAnonymousSession();
    assertThat(session.hasChildProjectsPermission(USER, application)).isFalse();
  }

  @Test
  public void hasChildProjectsPermission_keeps_cache_of_permissions_of_anonymous_user() {
    db.users().insertPermissionOnAnyone(USER);

    ComponentDto project = db.components().insertPublicProject();
    ComponentDto application = db.components().insertPublicApplication();
    db.components().addApplicationProject(application, project);

    UserSession session = newAnonymousSession();

    // feed the cache
    assertThat(session.hasChildProjectsPermission(USER, application)).isTrue();

    // change privacy of the project without updating the cache
    db.getDbClient().componentDao().setPrivateForRootComponentUuidWithoutAudit(db.getSession(), project.uuid(), true);
    assertThat(session.hasChildProjectsPermission(USER, application)).isTrue();
  }

  @Test
  public void test_hasPortfolioChildProjectsPermission_for_logged_in_user() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto project4 = db.components().insertPrivateProject();
    ComponentDto project5 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();
    UserSession session = newUserSession(user);

    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto subPortfolio = db.components().insertComponent(newSubPortfolio(portfolio));

    // Add public project1 to private portfolio
    db.components().addPortfolioProject(portfolio, project1);
    db.components().insertComponent(newProjectCopy(project1, portfolio));

    assertThat(session.hasPortfolioChildProjectsPermission(USER, portfolio)).isTrue();

    // Add private project2 with USER permissions to private portfolio
    db.users().insertProjectPermissionOnUser(user, USER, project2);
    db.components().addPortfolioProject(portfolio, project2);
    db.components().insertComponent(newProjectCopy(project2, portfolio));

    assertThat(session.hasPortfolioChildProjectsPermission(USER, portfolio)).isTrue();

    // Add private project4 with USER permissions to sub-portfolio
    db.users().insertProjectPermissionOnUser(user, USER, project4);
    db.components().addPortfolioProject(subPortfolio, project4);
    db.components().insertComponent(newProjectCopy(project4, subPortfolio));
    db.components().addPortfolioReference(portfolio, subPortfolio.uuid());

    // The predicate should work both on view and subview components
    assertThat(session.hasPortfolioChildProjectsPermission(USER, portfolio)).isTrue();
    assertThat(session.hasPortfolioChildProjectsPermission(USER, subPortfolio)).isTrue();

    // Add private project3 without permissions to private portfolio
    db.components().addPortfolioProject(portfolio, project3);
    db.components().insertComponent(newProjectCopy(project3, portfolio));

    assertThat(session.hasChildProjectsPermission(USER, portfolio)).isFalse();

    // Add private project5 without permissions to sub-portfolio
    db.components().addPortfolioProject(subPortfolio, project5);
    db.components().insertComponent(newProjectCopy(project5, subPortfolio));

    assertThat(session.hasPortfolioChildProjectsPermission(USER, portfolio)).isFalse();
    assertThat(session.hasPortfolioChildProjectsPermission(USER, subPortfolio)).isFalse();
  }

  @Test
  public void test_hasPortfolioChildProjectsPermission_for_anonymous_user() {
    ComponentDto project = db.components().insertPrivateProject();

    db.users().insertPermissionOnAnyone(USER);

    ComponentDto portfolio = db.components().insertPrivatePortfolio();

    db.components().addPortfolioProject(portfolio, project);
    // add computed project
    db.components().insertComponent(newProjectCopy(project, portfolio));

    UserSession session = newAnonymousSession();
    assertThat(session.hasPortfolioChildProjectsPermission(USER, portfolio)).isFalse();
  }

  @Test
  public void hasPortfolioChildProjectsPermission_keeps_cache_of_permissions_of_anonymous_user() {
    db.users().insertPermissionOnAnyone(USER);

    ComponentDto project = db.components().insertPublicProject();
    ComponentDto portfolio = db.components().insertPublicPortfolio();
    db.components().addPortfolioProject(portfolio, project);

    UserSession session = newAnonymousSession();

    // feed the cache
    assertThat(session.hasChildProjectsPermission(USER, portfolio)).isTrue();

    // change privacy of the project without updating the cache
    db.getDbClient().componentDao().setPrivateForRootComponentUuidWithoutAudit(db.getSession(), project.uuid(), true);
    assertThat(session.hasChildProjectsPermission(USER, portfolio)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_without_permissions() {
    ComponentDto publicProject = db.components().insertPublicProject();

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_global_permissions() {
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_group_permissions() {
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(), "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_user_permissions() {
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_without_permissions() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_group_permissions() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(), "p1", privateProject);

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_user_permissions() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", privateProject);

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_inserted_permissions_on_group_AnyOne_on_public_projects() {
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_public_projects() {
    ComponentDto publicProject = db.components().insertPublicProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_private_projects() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, "p1", privateProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_public_projects() {
    UserDto user = db.users().insertUser();
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnUser(user, "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_private_projects() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, "p1", project);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", project)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_keeps_cache_of_permissions_of_logged_in_user() {
    UserDto user = db.users().insertUser();
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnUser(user, ADMIN, publicProject);

    UserSession underTest = newUserSession(user);

    // feed the cache
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ADMIN, publicProject)).isTrue();

    // change permissions without updating the cache
    db.users().deletePermissionFromUser(publicProject, user, ADMIN);
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, publicProject);
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ADMIN, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ISSUE_ADMIN, publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_keeps_cache_of_permissions_of_anonymous_user() {
    ComponentDto publicProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone(ADMIN, publicProject);

    UserSession underTest = newAnonymousSession();

    // feed the cache
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ADMIN, publicProject)).isTrue();

    // change permissions without updating the cache
    db.users().deleteProjectPermissionFromAnyone(publicProject, ADMIN);
    db.users().insertProjectPermissionOnAnyone(ISSUE_ADMIN, publicProject);
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ADMIN, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, ISSUE_ADMIN, publicProject)).isFalse();
  }

  private boolean hasComponentPermissionByDtoOrUuid(UserSession underTest, String permission, ComponentDto component) {
    boolean b1 = underTest.hasComponentPermission(permission, component);
    boolean b2 = underTest.hasComponentUuidPermission(permission, component.uuid());
    checkState(b1 == b2, "Different behaviors");
    return b1;
  }

  @Test
  public void keepAuthorizedComponents_returns_empty_list_if_no_permissions_are_granted() {
    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();

    UserSession underTest = newAnonymousSession();

    assertThat(underTest.keepAuthorizedComponents(ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_anonymous() {
    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnAnyone(ISSUE_ADMIN, publicProject);

    UserSession underTest = newAnonymousSession();

    assertThat(underTest.keepAuthorizedComponents(ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(ISSUE_ADMIN, Arrays.asList(privateProject, publicProject))).containsExactly(publicProject);
  }

  @Test
  public void keepAuthorizedComponents_on_branches() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ADMIN, privateProject);
    ComponentDto privateBranchProject = db.components().insertProjectBranch(privateProject);

    UserSession underTest = newUserSession(user);

    assertThat(underTest.keepAuthorizedComponents(ADMIN, asList(privateProject, privateBranchProject)))
      .containsExactlyInAnyOrder(privateProject, privateBranchProject);
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_logged_in_user() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto project4 = db.components().insertPrivateProject();
    ComponentDto project5 = db.components().insertPrivateProject();
    ComponentDto project6 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();
    UserSession underTest = newUserSession(user);

    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    db.users().insertProjectPermissionOnUser(user, USER, portfolio);

    ComponentDto subPortfolio = db.components().insertComponent(newSubPortfolio(portfolio));
    db.users().insertProjectPermissionOnUser(user, USER, subPortfolio);

    ComponentDto app = db.components().insertPrivateApplication();
    db.users().insertProjectPermissionOnUser(user, USER, app);

    ComponentDto app2 = db.components().insertPrivateApplication();

    // Add public project1 to private portfolio
    db.components().addPortfolioProject(portfolio, project1);
    var copyProject1 = db.components().insertComponent(newProjectCopy(project1, portfolio));

    // Add private project2 with USER permissions to private portfolio
    db.users().insertProjectPermissionOnUser(user, USER, project2);
    db.components().addPortfolioProject(portfolio, project2);
    var copyProject2 = db.components().insertComponent(newProjectCopy(project2, portfolio));

    // Add private project4 with USER permissions to sub-portfolio
    db.users().insertProjectPermissionOnUser(user, USER, project4);
    db.components().addPortfolioProject(subPortfolio, project4);
    var copyProject4 = db.components().insertComponent(newProjectCopy(project4, subPortfolio));
    db.components().addPortfolioReference(portfolio, subPortfolio.uuid());

    // Add private project3 without permissions to private portfolio
    db.components().addPortfolioProject(portfolio, project3);
    var copyProject3 = db.components().insertComponent(newProjectCopy(project3, portfolio));

    // Add private project5 with USER permissions to app
    db.users().insertProjectPermissionOnUser(user, USER, project5);
    db.components().addApplicationProject(app, project5);
    var copyProject5 = db.components().insertComponent(newProjectCopy(project5, app));
    db.components().addPortfolioReference(portfolio, app.uuid());

    // Add private project6 to private app2
    db.components().addApplicationProject(app2, project6);
    var copyProject6 = db.components().insertComponent(newProjectCopy(project6, app2));
    db.components().addPortfolioReference(portfolio, app2.uuid());

    assertThat(underTest.keepAuthorizedComponents(ADMIN, List.of(portfolio))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(USER, List.of(portfolio))).containsExactly(portfolio);

    assertThat(underTest.keepAuthorizedComponents(ADMIN, Arrays.asList(app, subPortfolio, app2))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(USER, Arrays.asList(app, subPortfolio, app2))).containsExactly(app, subPortfolio);

    assertThat(underTest.keepAuthorizedComponents(ADMIN, Arrays.asList(project1, project2, project3, project4, project5, project6))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(USER, Arrays.asList(project1, project2, project3, project4, project5, project6))).containsExactly(project1, project2, project4, project5);

    assertThat(underTest.keepAuthorizedComponents(USER, Arrays.asList(copyProject1, copyProject2, copyProject3, copyProject4, copyProject5, copyProject6)))
        .containsExactly(copyProject1, copyProject2, copyProject4, copyProject5);
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_not_root() {
    UserDto user = db.users().insertUser();

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_true_if_user_is_administrator() {
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, SYSTEM_ADMIN);

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_user_is_not_administrator() {
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, PROVISIONING);

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void keep_isSystemAdministrator_flag_in_cache() {
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, SYSTEM_ADMIN);

    UserSession session = newUserSession(user);

    session.checkIsSystemAdministrator();

    db.getDbClient().userDao().deactivateUser(db.getSession(), user);
    db.commit();

    // should fail but succeeds because flag is kept in cache
    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_throws_ForbiddenException_if_not_system_administrator() {
    UserDto user = db.users().insertUser();

    UserSession session = newUserSession(user);

    assertThatThrownBy(session::checkIsSystemAdministrator)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void hasComponentPermission_on_branch_checks_permissions_of_its_project() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(privateProject, b -> b.setKey("feature/foo"));
    ComponentDto fileInBranch = db.components().insertComponent(newChildComponent("fileUuid", branch, branch));

    // permissions are defined on the project, not on the branch
    db.users().insertProjectPermissionOnUser(user, "p1", privateProject);

    UserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", branch)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", fileInBranch)).isTrue();
  }

  @Test
  public void keepAuthorizedProjects_shouldAcceptsPublicProjects_whenCalledWithPublicPermissionAndNoUser(){
    ComponentDto publicProject  = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();

    Set<ProjectDto> projectDto = Set.of(getProjectDto(publicProject), getProjectDto(privateProject));
    List<ProjectDto> projectDtos = newUserSession(null).keepAuthorizedProjects(USER, projectDto);

    assertThat(projectDtos).containsExactly(db.components().getProjectDto(publicProject));
  }

  @Test
  public void keepAuthorizedProjects_shouldAcceptsPublicProjects_whenCalledWithPublicPermissionAndAnUser(){
    UserDto userDto = db.users().insertUser();
    ComponentDto publicProject  = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();

    Set<ProjectDto> projectDto = Set.of(getProjectDto(publicProject), getProjectDto(privateProject));
    List<ProjectDto> projectDtos = newUserSession(userDto).keepAuthorizedProjects(USER, projectDto);

    assertThat(projectDtos).containsExactly(db.components().getProjectDto(publicProject));
  }

  @Test
  public void keepAuthorizedProjects_shouldAcceptsOnlyPrivateProject_whenCalledWithGoodPermissionAndAnUser(){
    String permission = "aNewPermission";
    UserDto userDto = db.users().insertUser();
    ComponentDto publicProject  = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(userDto, permission,privateProject);
    ComponentDto privateProjectWithoutPermission = db.components().insertPrivateProject();

    Set<ProjectDto> projectDto = Set.of(getProjectDto(publicProject), getProjectDto(privateProject), getProjectDto(privateProjectWithoutPermission));
    List<ProjectDto> projectDtos = newUserSession(userDto).keepAuthorizedProjects(permission, projectDto);

    assertThat(projectDtos).containsExactly(db.components().getProjectDto(privateProject));
  }

  @Test
  public void keepAuthorizedProjects_shouldRejectPrivateAndPublicProject_whenCalledWithWrongPermissionAndNoUser(){
    String permission = "aNewPermission";
    UserDto userDto = db.users().insertUser();
    ComponentDto publicProject  = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(userDto, permission,privateProject);
    ComponentDto privateProjectWithoutPermission = db.components().insertPrivateProject();

    Set<ProjectDto> projectDto = Set.of(getProjectDto(publicProject), getProjectDto(privateProject), getProjectDto(privateProjectWithoutPermission));
    List<ProjectDto> projectDtos = newUserSession(null).keepAuthorizedProjects(permission, projectDto);

    assertThat(projectDtos).isEmpty();
  }

  private ProjectDto getProjectDto(ComponentDto publicProject) {
    return db.components().getProjectDto(publicProject);
  }


  private ServerUserSession newUserSession(@Nullable UserDto userDto) {
    return new ServerUserSession(dbClient, userDto);
  }

  private ServerUserSession newAnonymousSession() {
    return newUserSession(null);
  }

  private void assertThatForbiddenExceptionIsThrown(ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

}
