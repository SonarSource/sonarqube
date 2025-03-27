/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.Indexers.EntityEvent;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.es.Indexers.EntityEvent.PERMISSION_CHANGE;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

public class PermissionIndexerTest {

  private static final IndexMainType INDEX_TYPE_FOO_AUTH = IndexType.main(FooIndexDefinition.DESCRIPTOR, TYPE_AUTHORIZATION);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.createCustom(new FooIndexDefinition());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private FooIndex fooIndex = new FooIndex(es.client(), new WebAuthorizationTypeSupport(userSession));
  private FooIndexer fooIndexer = new FooIndexer(es.client(), db.getDbClient());
  private PermissionIndexer underTest = new PermissionIndexer(db.getDbClient(), es.client(), fooIndexer);

  @Test
  public void indexOnStartup_grants_access_to_any_user_and_to_group_Anyone_on_public_projects() {
    ProjectDto project = createAndIndexPublicProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    indexOnStartup();

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user1);
    verifyAuthorized(project, user2);
  }

  @Test
  public void indexAll_grants_access_to_any_user_and_to_group_Anyone_on_public_projects() {
    ProjectDto project = createAndIndexPublicProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    underTest.indexAll(underTest.getIndexTypes());

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user1);
    verifyAuthorized(project, user2);
  }

  @Test
  public void deletion_resilience_will_deindex_projects() {
    ProjectDto project1 = createUnindexedPublicProject();
    ProjectDto project2 = createUnindexedPublicProject();
    // UserDto user1 = db.users().insertUser();
    indexOnStartup();
    assertThat(es.countDocuments(INDEX_TYPE_FOO_AUTH)).isEqualTo(2);

    // Simulate an indexing issue
    db.getDbClient().purgeDao().deleteProject(db.getSession(), project1.getUuid(), PROJECT, project1.getName(), project1.getKey());
    underTest.prepareForRecoveryOnEntityEvent(db.getSession(), asList(project1.getUuid()), EntityEvent.DELETION);
    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isOne();
    Collection<EsQueueDto> esQueueDtos = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), Long.MAX_VALUE, 2);

    underTest.index(db.getSession(), esQueueDtos);

    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isZero();
    assertThat(es.countDocuments(INDEX_TYPE_FOO_AUTH)).isOne();
  }

  @Test
  public void indexOnStartup_grants_access_to_user() {
    ProjectDto project = createAndIndexPrivateProject();
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
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, USER, project);
    db.users().insertEntityPermissionOnGroup(group2, ADMIN, project);

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
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user2);
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    db.users().insertEntityPermissionOnGroup(group, USER, project);

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
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();

    indexOnStartup();

    verifyAnyoneNotAuthorized(project);
    verifyNotAuthorized(project, user);
    verifyNotAuthorized(project, user, group);
  }

  @Test
  public void indexOnStartup_grants_access_to_anybody_on_public_project() {
    ProjectDto project = createAndIndexPublicProject();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();

    indexOnStartup();

    verifyAnyoneAuthorized(project);
    verifyAuthorized(project, user);
    verifyAuthorized(project, user, group);
  }

  @Test
  public void indexOnStartup_grants_access_to_anybody_on_view() {
    PortfolioDto view = createAndIndexPortfolio();
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
    ProjectDto project = null;
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
  public void public_projects_are_visible_to_anybody() {
    ProjectDto projectOnOrg1 = createAndIndexPublicProject();
    UserDto user = db.users().insertUser();

    indexOnStartup();

    verifyAnyoneAuthorized(projectOnOrg1);
    verifyAuthorized(projectOnOrg1, user);
  }

  @Test
  public void permissions_are_not_updated_on_project_tags_update() {
    ProjectDto project = createAndIndexPublicProject();

    indexPermissions(project, EntityEvent.PROJECT_TAGS_UPDATE);

    assertThatAuthIndexHasSize(0);
    verifyAnyoneNotAuthorized(project);
  }

  @Test
  public void permissions_are_not_updated_on_project_key_update() {
    ProjectDto project = createAndIndexPublicProject();

    indexPermissions(project, EntityEvent.PROJECT_TAGS_UPDATE);

    assertThatAuthIndexHasSize(0);
    verifyAnyoneNotAuthorized(project);
  }

  @Test
  public void index_permissions_on_project_creation() {
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project);

    indexPermissions(project, EntityEvent.CREATION);

    assertThatAuthIndexHasSize(1);
    verifyAuthorized(project, user);
  }

  @Test
  public void index_permissions_on_permission_change() {
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    indexPermissions(project, EntityEvent.CREATION);
    verifyAuthorized(project, user1);
    verifyNotAuthorized(project, user2);

    db.users().insertProjectPermissionOnUser(user2, USER, project);
    indexPermissions(project, PERMISSION_CHANGE);

    verifyAuthorized(project, user1);
    verifyAuthorized(project, user1);
  }

  @Test
  public void delete_permissions_on_project_deletion() {
    ProjectDto project = createAndIndexPrivateProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project);
    indexPermissions(project, EntityEvent.CREATION);
    verifyAuthorized(project, user);

    db.getDbClient().purgeDao().deleteProject(db.getSession(), project.getUuid(), PROJECT, project.getUuid(), project.getKey());
    indexPermissions(project, EntityEvent.DELETION);

    verifyNotAuthorized(project, user);
    assertThatAuthIndexHasSize(0);
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ProjectDto project = createAndIndexPublicProject();
    es.lockWrites(INDEX_TYPE_FOO_AUTH);

    IndexingResult result = indexPermissions(project, PERMISSION_CHANGE);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();
    assertThatAuthIndexHasSize(0);
    assertThatEsQueueTableHasSize(1);

    es.unlockWrites(INDEX_TYPE_FOO_AUTH);

    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isZero();
    verifyAnyoneAuthorized(project);
    assertThatEsQueueTableHasSize(0);
  }

  private void assertThatAuthIndexHasSize(int expectedSize) {
    assertThat(es.countDocuments(FooIndexDefinition.TYPE_AUTHORIZATION)).isEqualTo(expectedSize);
  }

  private void indexOnStartup() {
    underTest.indexOnStartup(underTest.getIndexTypes());
  }

  private void verifyAuthorized(EntityDto entity, UserDto user) {
    logIn(user);
    verifyAuthorized(entity, true);
  }

  private void verifyAuthorized(EntityDto entity, UserDto user, GroupDto group) {
    logIn(user).setGroups(group);
    verifyAuthorized(entity, true);
  }

  private void verifyNotAuthorized(EntityDto entity, UserDto user) {
    logIn(user);
    verifyAuthorized(entity, false);
  }

  private void verifyNotAuthorized(EntityDto entity, UserDto user, GroupDto group) {
    logIn(user).setGroups(group);
    verifyAuthorized(entity, false);
  }

  private void verifyAnyoneAuthorized(EntityDto entity) {
    userSession.anonymous();
    verifyAuthorized(entity, true);
  }

  private void verifyAnyoneNotAuthorized(EntityDto entity) {
    userSession.anonymous();
    verifyAuthorized(entity, false);
  }

  private void verifyAuthorized(EntityDto entity, boolean expectedAccess) {
    assertThat(fooIndex.hasAccessToProject(entity.getUuid())).isEqualTo(expectedAccess);
  }

  private UserSessionRule logIn(UserDto u) {
    userSession.logIn(u);
    return userSession;
  }

  private IndexingResult indexPermissions(EntityDto entity, EntityEvent cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnEntityEvent(dbSession, singletonList(entity.getUuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private ProjectDto createUnindexedPublicProject() {
    return db.components().insertPublicProject().getProjectDto();
  }

  private ProjectDto createAndIndexPrivateProject() {
    ProjectData project = db.components().insertPrivateProject();
    fooIndexer.indexOnAnalysis(project.getMainBranchDto().getUuid());
    return project.getProjectDto();
  }

  private ProjectDto createAndIndexPublicProject() {
    ProjectData project = db.components().insertPublicProject();
    fooIndexer.indexOnAnalysis(project.getMainBranchDto().getUuid());
    return project.getProjectDto();
  }

  private PortfolioDto createAndIndexPortfolio() {
    PortfolioDto view = db.components().insertPublicPortfolioDto();
    fooIndexer.indexOnAnalysis(view.getUuid());
    return view;
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

}
