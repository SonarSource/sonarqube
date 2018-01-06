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
package org.sonar.server.permission.index;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.es.ProjectIndexer.Cause.PERMISSION_CHANGE;

public class PermissionIndexerTest {

  private static final IndexType INDEX_TYPE_FOO_AUTH = AuthorizationTypeSupport.getAuthorizationIndexType(FooIndexDefinition.INDEX_TYPE_FOO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = new EsTester(new FooIndexDefinition());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private FooIndex fooIndex = new FooIndex(es.client(), new AuthorizationTypeSupport(userSession));
  private FooIndexer fooIndexer = new FooIndexer(db.getDbClient(), es.client());
  private PermissionIndexer underTest = new PermissionIndexer(db.getDbClient(), es.client(), fooIndexer);

  @Test
  public void indexOnStartup_grants_access_to_any_user_and_to_group_Anyone_on_public_projects() {
    ComponentDto project = createAndIndexPublicProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    indexOnStartup();

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user1);
    verifyAuthorized(project, user2);
  }

  @Test
  public void deletion_resilience_will_deindex_projects() {
    ComponentDto project1 = createUnindexedPublicProject();
    ComponentDto project2 = createUnindexedPublicProject();
    //UserDto user1 = db.users().insertUser();
    indexOnStartup();
    assertThat(es.countDocuments(INDEX_TYPE_FOO_AUTH)).isEqualTo(2);

    // Simulate a indexation issue
    db.getDbClient().componentDao().delete(db.getSession(), project1.getId());
    underTest.prepareForRecovery(db.getSession(), asList(project1.uuid()), ProjectIndexer.Cause.PROJECT_DELETION);
    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isEqualTo(1);
    Collection<EsQueueDto> esQueueDtos = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), Long.MAX_VALUE, 2);

    underTest.index(db.getSession(), esQueueDtos);

    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isEqualTo(0);
    assertThat(es.countDocuments(INDEX_TYPE_FOO_AUTH)).isEqualTo(1);
  }

  @Test
  public void indexOnStartup_grants_access_to_user() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    db.users().insertProjectPermissionOnUser(user2, ADMIN, project);

    indexOnStartup();

    // anonymous
    verifyAnyoneNotAuthorized(project);

    // user1 has access
    verifyAuthorized(project, user1);

    // user2 has not access (only USER permission is accepted)
    verifyNotAuthorized(project, user2);
  }

  @Test
  public void indexOnStartup_grants_access_to_group_on_private_project() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, USER, project);
    db.users().insertProjectPermissionOnGroup(group2, ADMIN, project);

    indexOnStartup();

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
  public void indexOnStartup_grants_access_to_user_and_group() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user2);
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    db.users().insertProjectPermissionOnGroup(group, USER, project);

    indexOnStartup();

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
  public void indexOnStartup_does_not_grant_access_to_anybody_on_private_project() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();

    indexOnStartup();

    verifyAnyoneNotAuthorized(project);
    verifyNotAuthorized(project, user);
    verifyNotAuthorized(project, user, group);
  }

  @Test
  public void indexOnStartup_grants_access_to_anybody_on_public_project() {
    ComponentDto project = createAndIndexPublicProject();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();

    indexOnStartup();

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user);
    verifyAuthorized(project, user, group);
  }

  @Test
  public void indexOnStartup_grants_access_to_anybody_on_view() {
    ComponentDto view = createAndIndexView();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();

    indexOnStartup();

    verifyAnyoneAuthorized(view);
    verifyAuthorized(view, user);
    verifyAuthorized(view, user, group);
  }

  @Test
  public void indexOnStartup_grants_access_on_many_projects() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ComponentDto project = null;
    for (int i = 0; i < 10; i++) {
      project = createAndIndexPrivateProject();
      db.users().insertProjectPermissionOnUser(user1, USER, project);
    }

    indexOnStartup();

    verifyAnyoneNotAuthorized(project);
    verifyAuthorized(project, user1);
    verifyNotAuthorized(project, user2);
  }

  @Test
  public void public_projects_are_visible_to_anybody_whatever_the_organization() {
    ComponentDto projectOnOrg1 = createAndIndexPublicProject(db.organizations().insert());
    ComponentDto projectOnOrg2 = createAndIndexPublicProject(db.organizations().insert());
    UserDto user = db.users().insertUser();

    indexOnStartup();

    verifyAnyoneAuthorized(projectOnOrg1);
    verifyAnyoneAuthorized(projectOnOrg2);
    verifyAuthorized(projectOnOrg1, user);
    verifyAuthorized(projectOnOrg2, user);
  }

  @Test
  public void indexOnAnalysis_does_nothing_because_CE_does_not_touch_permissions() {
    ComponentDto project = createAndIndexPublicProject();

    underTest.indexOnAnalysis(project.uuid());

    assertThatAuthIndexHasSize(0);
    verifyAnyoneNotAuthorized(project);
  }

  @Test
  public void permissions_are_not_updated_on_project_tags_update() {
    ComponentDto project = createAndIndexPublicProject();

    indexPermissions(project, ProjectIndexer.Cause.PROJECT_TAGS_UPDATE);

    assertThatAuthIndexHasSize(0);
    verifyAnyoneNotAuthorized(project);
  }

  @Test
  public void permissions_are_not_updated_on_project_key_update() {
    ComponentDto project = createAndIndexPublicProject();

    indexPermissions(project, ProjectIndexer.Cause.PROJECT_TAGS_UPDATE);

    assertThatAuthIndexHasSize(0);
    verifyAnyoneNotAuthorized(project);
  }

  @Test
  public void index_permissions_on_project_creation() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project);

    indexPermissions(project, ProjectIndexer.Cause.PROJECT_CREATION);

    assertThatAuthIndexHasSize(1);
    verifyAuthorized(project, user);
  }

  @Test
  public void index_permissions_on_permission_change() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    indexPermissions(project, ProjectIndexer.Cause.PROJECT_CREATION);
    verifyAuthorized(project, user1);
    verifyNotAuthorized(project, user2);

    db.users().insertProjectPermissionOnUser(user2, USER, project);
    indexPermissions(project, PERMISSION_CHANGE);

    verifyAuthorized(project, user1);
    verifyAuthorized(project, user1);
  }

  @Test
  public void delete_permissions_on_project_deletion() {
    ComponentDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project);
    indexPermissions(project, ProjectIndexer.Cause.PROJECT_CREATION);
    verifyAuthorized(project, user);

    db.getDbClient().componentDao().delete(db.getSession(), project.getId());
    indexPermissions(project, ProjectIndexer.Cause.PROJECT_DELETION);

    verifyNotAuthorized(project, user);
    assertThatAuthIndexHasSize(0);
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ComponentDto project = createAndIndexPublicProject();
    es.lockWrites(INDEX_TYPE_FOO_AUTH);

    IndexingResult result = indexPermissions(project, PERMISSION_CHANGE);
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(1L);

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(1L);
    assertThatAuthIndexHasSize(0);
    assertThatEsQueueTableHasSize(1);

    es.unlockWrites(INDEX_TYPE_FOO_AUTH);

    result = recover();
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(0L);
    verifyAnyoneAuthorized(project);
    assertThatEsQueueTableHasSize(0);
  }

  private void assertThatAuthIndexHasSize(int expectedSize) {
    IndexType authIndexType = underTest.getIndexTypes().iterator().next();
    assertThat(es.countDocuments(authIndexType)).isEqualTo(expectedSize);
  }

  private void indexOnStartup() {
    underTest.indexOnStartup(underTest.getIndexTypes());
  }

  private void verifyAuthorized(ComponentDto project, UserDto user) {
    logIn(user);
    verifyAuthorized(project, true);
  }

  private void verifyAuthorized(ComponentDto project, UserDto user, GroupDto group) {
    logIn(user).setGroups(group);
    verifyAuthorized(project, true);
  }

  private void verifyNotAuthorized(ComponentDto project, UserDto user) {
    logIn(user);
    verifyAuthorized(project, false);
  }

  private void verifyNotAuthorized(ComponentDto project, UserDto user, GroupDto group) {
    logIn(user).setGroups(group);
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

  private UserSessionRule logIn(UserDto u) {
    userSession.logIn(u.getLogin()).setUserId(u.getId());
    return userSession;
  }

  private IndexingResult indexPermissions(ComponentDto project, ProjectIndexer.Cause cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecovery(dbSession, singletonList(project.uuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private ComponentDto createUnindexedPublicProject() {
    ComponentDto project = db.components().insertPublicProject();
    return project;
  }

  private ComponentDto createAndIndexPrivateProject() {
    ComponentDto project = db.components().insertPrivateProject();
    fooIndexer.indexOnAnalysis(project.uuid());
    return project;
  }

  private ComponentDto createAndIndexPublicProject() {
    ComponentDto project = db.components().insertPublicProject();
    fooIndexer.indexOnAnalysis(project.uuid());
    return project;
  }

  private ComponentDto createAndIndexView() {
    ComponentDto view = db.components().insertView();
    fooIndexer.indexOnAnalysis(view.uuid());
    return view;
  }

  private ComponentDto createAndIndexPublicProject(OrganizationDto org) {
    ComponentDto project = db.components().insertPublicProject(org);
    fooIndexer.indexOnAnalysis(project.uuid());
    return project;
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

}
