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
package org.sonar.server.measure.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.IndexPermissions;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.es.EsClient.prepareSearch;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_CREATION;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_DELETION;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_KEY_UPDATE;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_TAGS_UPDATE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

public class ProjectMeasuresIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);

  private ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(db.getDbClient(), es.client());

  @Test
  public void test_getAuthorizationScope() {
    AuthorizationScope scope = underTest.getAuthorizationScope();
    assertThat(scope.getIndexType().getIndex()).isEqualTo(ProjectMeasuresIndexDefinition.DESCRIPTOR);
    assertThat(scope.getIndexType().getType()).isEqualTo(TYPE_AUTHORIZATION);

    Predicate<IndexPermissions> projectPredicate = scope.getProjectPredicate();
    IndexPermissions project = new IndexPermissions("P1", Qualifiers.PROJECT);
    IndexPermissions app = new IndexPermissions("P1", Qualifiers.APP);
    IndexPermissions file = new IndexPermissions("F1", Qualifiers.FILE);
    assertThat(projectPredicate.test(project)).isTrue();
    assertThat(projectPredicate.test(app)).isTrue();
    assertThat(projectPredicate.test(file)).isFalse();
  }

  @Test
  public void index_nothing() {
    underTest.indexOnStartup(emptySet());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void indexOnStartup_indexes_all_projects() {
    SnapshotDto project1 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project2 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project3 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2, project3);
    assertThatQualifierIs("TRK", project1, project2, project3);
  }

  @Test
  public void indexAll_indexes_all_projects() {
    SnapshotDto project1 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project2 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project3 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());

    underTest.indexAll();

    assertThatIndexContainsOnly(project1, project2, project3);
    assertThatQualifierIs("TRK", project1, project2, project3);
  }

  /**
   * Provisioned projects don't have analysis yet
   */
  @Test
  public void indexOnStartup_indexes_provisioned_projects() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnStartup_ignores_non_main_branches() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnStartup_indexes_all_applications() {
    ComponentDto application1 = db.components().insertPrivateApplication();
    ComponentDto application2 = db.components().insertPrivateApplication();
    ComponentDto application3 = db.components().insertPrivateApplication();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(application1, application2, application3);
    assertThatQualifierIs("APP", application1, application2, application3);
  }

  @Test
  public void indexOnStartup_indexes_projects_and_applications() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();

    ComponentDto application1 = db.components().insertPrivateApplication();
    ComponentDto application2 = db.components().insertPrivateApplication();
    ComponentDto application3 = db.components().insertPrivateApplication();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2, project3, application1, application2, application3);
    assertThatQualifierIs("TRK", project1, project2, project3);
    assertThatQualifierIs("APP", application1, application2, application3);
  }

  @Test
  public void indexOnAnalysis_indexes_provisioned_project() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    underTest.indexOnAnalysis(project1.uuid());

    assertThatIndexContainsOnly(project1);
  }

  @Test
  public void indexOnAnalysis_indexes_provisioned_application() {
    ComponentDto app1 = db.components().insertPrivateApplication();
    ComponentDto app2 = db.components().insertPrivateApplication();

    underTest.indexOnAnalysis(app1.uuid());

    assertThatIndexContainsOnly(app1);
  }

  @Test
  public void update_index_when_project_key_is_updated() {
    ComponentDto project = db.components().insertPrivateProject();

    IndexingResult result = indexProject(project, PROJECT_KEY_UPDATE);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void update_index_when_project_is_created() {
    ComponentDto project = db.components().insertPrivateProject();

    IndexingResult result = indexProject(project, PROJECT_CREATION);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void update_index_when_project_tags_are_updated() {
    ComponentDto project = db.components().insertPrivateProject(defaults(), p -> p.setTagsString("foo"));
    indexProject(project, PROJECT_CREATION);
    assertThatProjectHasTag(project, "foo");

    ProjectDto projectDto = db.components().getProjectDto(project);
    projectDto.setTagsString("bar");
    db.getDbClient().projectDao().updateTags(db.getSession(), projectDto);
    // TODO change indexing?
    IndexingResult result = indexProject(project, PROJECT_TAGS_UPDATE);

    assertThatProjectHasTag(project, "bar");
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void delete_doc_from_index_when_project_is_deleted() {
    ComponentDto project = db.components().insertPrivateProject();
    indexProject(project, PROJECT_CREATION);
    assertThatIndexContainsOnly(project);

    db.getDbClient().purgeDao().deleteProject(db.getSession(), project.uuid(), Qualifiers.PROJECT, project.name(), project.getKey());
    IndexingResult result = indexProject(project, PROJECT_DELETION);

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void do_nothing_if_no_projects_and_apps_to_index() {
    // this project should not be indexed
    db.components().insertPrivateProject();
    db.components().insertPrivateApplication();

    underTest.index(db.getSession(), emptyList());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ComponentDto project = db.components().insertPrivateProject();
    es.lockWrites(TYPE_PROJECT_MEASURES);

    IndexingResult result = indexProject(project, PROJECT_CREATION);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();
    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
    assertThatEsQueueTableHasSize(1);

    es.unlockWrites(TYPE_PROJECT_MEASURES);

    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isZero();
    assertThatEsQueueTableHasSize(0);
    assertThatIndexContainsOnly(project);
  }

  @Test
  public void non_main_branches_are_not_indexed_during_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnAnalysis(branch.uuid());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  private IndexingResult indexProject(ComponentDto project, ProjectIndexer.Cause cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecovery(dbSession, singletonList(project.uuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private void assertThatProjectHasTag(ComponentDto project, String expectedTag) {
    SearchRequest request = prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(new SearchSourceBuilder()
        .query(boolQuery()
          .filter(termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()))
          .filter(termQuery(FIELD_TAGS, expectedTag))));

    assertThat(es.client().search(request).getHits().getHits())
      .extracting(SearchHit::getId)
      .contains(project.uuid());
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

  private void assertThatIndexContainsOnly(SnapshotDto... expectedProjects) {
    assertThat(es.getIds(TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrder(
      Arrays.stream(expectedProjects).map(SnapshotDto::getComponentUuid).toArray(String[]::new));
  }

  private void assertThatIndexContainsOnly(ComponentDto... expectedProjects) {
    assertThat(es.getIds(TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrder(
      Arrays.stream(expectedProjects).map(ComponentDto::uuid).toArray(String[]::new));
  }

  private void assertThatQualifierIs(String qualifier, ComponentDto... expectedComponents) {
    String[] expectedComponentUuids = Arrays.stream(expectedComponents).map(ComponentDto::uuid).toArray(String[]::new);
    assertThatQualifierIs(qualifier, expectedComponentUuids);
  }

  private void assertThatQualifierIs(String qualifier, SnapshotDto... expectedComponents) {
    String[] expectedComponentUuids = Arrays.stream(expectedComponents).map(SnapshotDto::getComponentUuid).toArray(String[]::new);
    assertThatQualifierIs(qualifier, expectedComponentUuids);
  }

  private void assertThatQualifierIs(String qualifier, String... componentsUuid) {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(boolQuery()
        .filter(termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()))
        .filter(termQuery(FIELD_QUALIFIER, qualifier))
        .filter(termsQuery(FIELD_UUID, componentsUuid)));

    SearchRequest request = prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(searchSourceBuilder);
    assertThat(es.client().search(request).getHits().getHits())
      .extracting(SearchHit::getId)
      .containsExactlyInAnyOrder(componentsUuid);
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }

  private static <T> Consumer<T> defaults() {
    return t -> {
      // do nothing
    };
  }

}
