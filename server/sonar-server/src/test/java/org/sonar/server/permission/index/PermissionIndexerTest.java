/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.permission.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.permission.index.FooIndexDefinition.FOO_INDEX;

public class PermissionIndexerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new FooIndexDefinition());

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private UserDbTester userDbTester = new UserDbTester(dbTester);
  private FooIndex fooIndex = new FooIndex(esTester.client(), new AuthorizationTypeSupport(userSession));
  private FooIndexer fooIndexer = new FooIndexer(esTester.client());
  private PermissionIndexer underTest = new PermissionIndexer(
    dbTester.getDbClient(), esTester.client(), fooIndexer);

  @Before
  public void setUp() throws Exception {
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void indexAllIfEmpty_does_nothing_if_no_data() {
    underTest.indexAllIfEmpty();

    assertThat(esTester.countDocuments(FOO_INDEX, "authorization")).isZero();
    assertThat(fooIndex.hasAccessToProject("a_project")).isFalse();
  }

  @Test
  public void indexAllIfEmpty_grants_access_to_user() {
    ComponentDto project = createAndIndexProject();
    UserDto user1 = userDbTester.insertUser();
    UserDto user2 = userDbTester.insertUser();
    userDbTester.insertProjectPermissionOnUser(user1, USER, project);
    userDbTester.insertProjectPermissionOnUser(user2, ADMIN, project);

    underTest.indexAllIfEmpty();

    // anonymous
    verifyAnyoneNotAuthorized(project);

    // user1 has access
    verifyAuthorized(project, user1);

    // user2 has not access (only USER permission is accepted)
    verifyNotAuthorized(project, user2);
  }

  @Test
  public void indexAllIfEmpty_grants_access_to_group() {
    ComponentDto project = createAndIndexProject();
    UserDto user1 = userDbTester.insertUser();
    UserDto user2 = userDbTester.insertUser();
    UserDto user3 = userDbTester.insertUser();
    GroupDto group1 = userDbTester.insertGroup();
    GroupDto group2 = userDbTester.insertGroup();
    userDbTester.insertProjectPermissionOnGroup(group1, USER, project);
    userDbTester.insertProjectPermissionOnGroup(group2, ADMIN, project);

    underTest.indexAllIfEmpty();

    // anonymous
    verifyAnyoneNotAuthorized(project);

    // group1 has access
    verifyAuthorized(project, user1, group1);

    // group2 has not access (only USER permission is accepted)
    verifyNotAuthorized(project, user2, group2);

    // user3 is not in any group
    verifyNotAuthorized(project, user3);
  }

  @Test
  public void indexAllIfEmpty_grants_access_to_user_and_group() {
    ComponentDto project = createAndIndexProject();
    UserDto user1 = userDbTester.insertUser();
    UserDto user2 = userDbTester.insertUser();
    GroupDto group = userDbTester.insertGroup();
    userDbTester.insertMember(group, user2);
    userDbTester.insertProjectPermissionOnUser(user1, USER, project);
    userDbTester.insertProjectPermissionOnGroup(group, USER, project);

    underTest.indexAllIfEmpty();

    // anonymous
    verifyAnyoneNotAuthorized(project);

    // has direct access
    verifyAuthorized(project, user1);

    // has access through group
    verifyAuthorized(project, user1, group);

    // no access
    verifyNotAuthorized(project, user2);
  }

  @Test
  public void indexAllIfEmpty_does_not_grant_access_to_anybody() {
    ComponentDto project = createAndIndexProject();
    UserDto user = userDbTester.insertUser();
    GroupDto group = userDbTester.insertGroup();

    underTest.indexAllIfEmpty();

    verifyAnyoneNotAuthorized(project);
    verifyNotAuthorized(project, user);
    verifyNotAuthorized(project, user, group);
  }

  @Test
  public void indexAllIfEmpty_grants_access_to_anyone() {
    ComponentDto project = createAndIndexProject();
    UserDto user = userDbTester.insertUser();
    GroupDto group = userDbTester.insertGroup();
    userDbTester.insertProjectPermissionOnAnyone(USER, project);

    underTest.indexAllIfEmpty();

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user);
    verifyAuthorized(project, user, group);
  }

  @Test
  public void indexAllIfEmpty_grants_access_on_many_projects() {
    UserDto user1 = userDbTester.insertUser();
    UserDto user2 = userDbTester.insertUser();
    ComponentDto project = null;
    for (int i = 0; i < PermissionIndexer.MAX_BATCH_SIZE + 10; i++) {
      project = createAndIndexProject();
      userDbTester.insertProjectPermissionOnUser(user1, USER, project);
    }

    underTest.indexAllIfEmpty();

    verifyAnyoneNotAuthorized(project);
    verifyAuthorized(project, user1);
    verifyNotAuthorized(project, user2);
  }

  @Test
  public void deleteProject_deletes_the_documents_related_to_the_project() {
    ComponentDto project1 = createAndIndexProject();
    ComponentDto project2 = createAndIndexProject();
    userDbTester.insertProjectPermissionOnAnyone(USER, project1);
    userDbTester.insertProjectPermissionOnAnyone(USER, project2);
    underTest.indexAllIfEmpty();
    assertThat(esTester.countDocuments(FooIndexDefinition.FOO_INDEX, AuthorizationTypeSupport.TYPE_AUTHORIZATION)).isEqualTo(2);

    underTest.deleteProject(project1.uuid());
    assertThat(esTester.countDocuments(FooIndexDefinition.FOO_INDEX, AuthorizationTypeSupport.TYPE_AUTHORIZATION)).isEqualTo(1);
  }

  @Test
  public void indexProject_does_nothing_because_authorizations_are_triggered_outside_standard_indexer_lifecycle() {
    ComponentDto project = createAndIndexProject();
    userDbTester.insertProjectPermissionOnAnyone(USER, project);

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.NEW_ANALYSIS);
    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);

    assertThat(esTester.countDocuments(FooIndexDefinition.FOO_INDEX, AuthorizationTypeSupport.TYPE_AUTHORIZATION)).isEqualTo(0);
  }

  @Test
  public void projects_without_any_permission_are_not_returned() {
    ComponentDto project = createAndIndexProject();
    UserDto user1 = userDbTester.insertUser();

    underTest.indexAllIfEmpty();

    verifyAnyoneNotAuthorized(project);
    verifyNotAuthorized(project, user1);
  }

  @Test
  public void permissions_on_anyone_should_not_conflict_between_organizations() {
    ComponentDto projectOnOrg1 = createAndIndexProject(dbTester.organizations().insert());
    ComponentDto projectOnOrg2 = createAndIndexProject(dbTester.organizations().insert());
    UserDto user = userDbTester.insertUser();
    userDbTester.insertProjectPermissionOnAnyone(USER, projectOnOrg1);
    userDbTester.insertProjectPermissionOnUser(user, USER, projectOnOrg2);

    underTest.indexAllIfEmpty();

    verifyAnyoneAuthorized(projectOnOrg1);
    verifyAnyoneNotAuthorized(projectOnOrg2);
    verifyAuthorized(projectOnOrg1, user);// because anyone
    verifyAuthorized(projectOnOrg2, user);
  }

  private void verifyAuthorized(ComponentDto project, UserDto user) {
    log_in(user);
    verifyAuthorized(project, true);
  }

  private void verifyAuthorized(ComponentDto project, UserDto user, GroupDto group) {
    log_in(user).setGroups(group);
    verifyAuthorized(project, true);
  }

  private void verifyNotAuthorized(ComponentDto project, UserDto user) {
    log_in(user);
    verifyAuthorized(project, false);
  }

  private void verifyNotAuthorized(ComponentDto project, UserDto user, GroupDto group) {
    log_in(user).setGroups(group);
    verifyAuthorized(project, false);
  }

  private void verifyAnyoneAuthorized(ComponentDto project) {
    userSession.anonymous();
    verifyAuthorized(project, true);
  }

  private void verifyAnyoneNotAuthorized(ComponentDto project) {
    userSession.anonymous();
    verifyAuthorized(project, false);
  }

  private void verifyAuthorized(ComponentDto project, boolean expectedAccess) {
    assertThat(fooIndex.hasAccessToProject(project.uuid())).isEqualTo(expectedAccess);
  }

  private UserSessionRule log_in(UserDto u) {
    userSession.logIn(u.getLogin()).setUserId(u.getId().intValue());
    return userSession;
  }

  private ComponentDto createAndIndexProject() {
    ComponentDto project = componentDbTester.insertProject();
    fooIndexer.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    return project;
  }

  private ComponentDto createAndIndexProject(OrganizationDto org) {
    ComponentDto project = componentDbTester.insertProject(org);
    fooIndexer.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_CREATION);
    return project;
  }
}
