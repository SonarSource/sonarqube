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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.lucene.search.join.ScoreMode;
import org.assertj.core.groups.Tuple;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.IndexPermissions;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.es.EsClient.prepareSearch;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.Indexers.EntityEvent.CREATION;
import static org.sonar.server.es.Indexers.EntityEvent.DELETION;
import static org.sonar.server.es.Indexers.EntityEvent.PROJECT_KEY_UPDATE;
import static org.sonar.server.es.Indexers.EntityEvent.PROJECT_TAGS_UPDATE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

public class ProjectMeasuresIndexerIT {

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);

  private final ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(db.getDbClient(), es.client());

  @Test
  public void getAuthorizationScope_shouldReturnTrueForProjectAndApp() {
    AuthorizationScope scope = underTest.getAuthorizationScope();
    assertThat(scope.getIndexType().getIndex()).isEqualTo(ProjectMeasuresIndexDefinition.DESCRIPTOR);
    assertThat(scope.getIndexType().getType()).isEqualTo(TYPE_AUTHORIZATION);

    Predicate<IndexPermissions> projectPredicate = scope.getEntityPredicate();
    IndexPermissions project = new IndexPermissions("P1", ComponentQualifiers.PROJECT);
    IndexPermissions app = new IndexPermissions("P1", ComponentQualifiers.APP);
    IndexPermissions file = new IndexPermissions("F1", ComponentQualifiers.FILE);
    assertThat(projectPredicate.test(project)).isTrue();
    assertThat(projectPredicate.test(app)).isTrue();
    assertThat(projectPredicate.test(file)).isFalse();
  }

  @Test
  public void indexOnStartup_whenNoEntities_shouldNotIndexAnything() {
    underTest.indexOnStartup(emptySet());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void indexOnStartup_shouldIndexAllProjects() {
    SnapshotDto project1 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project2 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());
    SnapshotDto project3 = db.components().insertProjectAndSnapshot(newPrivateProjectDto());

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2, project3);
    assertThatQualifierIs("TRK", project1, project2, project3);
  }

  @Test
  public void indexAll_indexes_all_projects() {

    ProjectData project1 = db.components().insertPrivateProject();
    SnapshotDto snapshot1 = db.components().insertSnapshot(project1.getMainBranchComponent());

    ProjectData project2 = db.components().insertPrivateProject();
    SnapshotDto snapshot2 = db.components().insertSnapshot(project2.getMainBranchComponent());

    ProjectData project3 = db.components().insertPrivateProject();
    SnapshotDto snapshot3 = db.components().insertSnapshot(project3.getMainBranchComponent());

    underTest.indexAll();

    assertThatIndexContainsOnly(snapshot1, snapshot2, snapshot3);
    assertThatQualifierIs("TRK", snapshot1, snapshot2, snapshot3);
    assertThatIndexContainsCreationDate(project1, project2, project3);
  }

  /**
   * Provisioned projects don't have analysis yet
   */
  @Test
  public void indexOnStartup_indexes_provisioned_projects() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnStartup_ignores_non_main_branches() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnStartup_indexes_all_applications() {
    ProjectDto application1 = db.components().insertPrivateApplication().getProjectDto();
    ProjectDto application2 = db.components().insertPrivateApplication().getProjectDto();
    ProjectDto application3 = db.components().insertPrivateApplication().getProjectDto();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(application1, application2, application3);
    assertThatQualifierIs("APP", application1, application2, application3);
  }

  @Test
  public void indexOnStartup_indexes_projects_and_applications() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();

    ProjectDto application1 = db.components().insertPrivateApplication().getProjectDto();
    ProjectDto application2 = db.components().insertPrivateApplication().getProjectDto();
    ProjectDto application3 = db.components().insertPrivateApplication().getProjectDto();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2, project3, application1, application2, application3);
    assertThatQualifierIs("TRK", project1, project2, project3);
    assertThatQualifierIs("APP", application1, application2, application3);
  }

  @Test
  public void indexOnAnalysis_indexes_provisioned_project() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();

    underTest.indexOnAnalysis(project1.getMainBranchComponent().uuid());

    assertThatIndexContainsOnly(project1.getProjectDto());
  }

  @Test
  public void indexOnAnalysis_whenPassingANonMainBranch_ShouldNotIndexProject() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    BranchDto branchDto = db.components().insertProjectBranch(project1.getProjectDto());
    underTest.indexOnAnalysis(branchDto.getUuid());

    assertThatIndexContainsOnly(new ProjectDto[]{});
  }

  @Test
  public void indexOnAnalysis_indexes_provisioned_application() {
    ProjectData app1 = db.components().insertPrivateApplication();
    ProjectData app2 = db.components().insertPrivateApplication();

    underTest.indexOnAnalysis(app1.getMainBranchComponent().uuid());

    assertThatIndexContainsOnly(app1.getProjectDto());
  }

  @Test
  public void update_index_when_project_key_is_updated() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    IndexingResult result = indexProject(project, PROJECT_KEY_UPDATE);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void update_index_when_project_is_created() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    IndexingResult result = indexProject(project, CREATION);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void update_index_when_project_tags_are_updated() {
    ProjectDto project = db.components().insertPrivateProject(defaults(), p -> p.setTagsString("foo")).getProjectDto();
    indexProject(project, CREATION);
    assertThatProjectHasTag(project, "foo");

    project.setTagsString("bar");
    db.getDbClient().projectDao().updateTags(db.getSession(), project);
    IndexingResult result = indexProject(project, PROJECT_TAGS_UPDATE);

    assertThatProjectHasTag(project, "bar");
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void prepareForRecoveryOnEntityEvent_shouldReindexProject_whenSwitchMainBranch() {
    ProjectData projectData = db.components().insertPrivateProject(defaults(), p -> p.setTagsString("foo"));
    ProjectDto project = projectData.getProjectDto();
    BranchDto oldMainBranchDto = projectData.getMainBranchDto();
    BranchDto newMainBranchDto = db.components().insertProjectBranch(project);
    MetricDto nloc = db.measures().insertMetric(m -> m.setKey(CoreMetrics.NCLOC_KEY));
    db.measures().insertMeasure(oldMainBranchDto, e -> e.addValue(nloc.getKey(), 1d));
    db.measures().insertMeasure(newMainBranchDto, e -> e.addValue(nloc.getKey(), 2d));
    indexProject(project, CREATION);
    assertThatProjectHasMeasure(project, CoreMetrics.NCLOC_KEY, 1d);

    db.getDbClient().branchDao().updateIsMain(db.getSession(), oldMainBranchDto.getUuid(), false);
    db.getDbClient().branchDao().updateIsMain(db.getSession(), newMainBranchDto.getUuid(), true);
    IndexingResult result = indexBranches(List.of(oldMainBranchDto, newMainBranchDto), Indexers.BranchEvent.SWITCH_OF_MAIN_BRANCH);

    assertThatProjectHasMeasure(project, CoreMetrics.NCLOC_KEY, 2d);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void delete_doc_from_index_when_project_is_deleted() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    indexProject(project, CREATION);
    assertThatIndexContainsOnly(project);

    db.getDbClient().purgeDao().deleteProject(db.getSession(), project.getUuid(), ComponentQualifiers.PROJECT, project.getName(), project.getKey());
    IndexingResult result = indexProject(project, DELETION);

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void do_nothing_if_no_projects_and_apps_to_index() {
    // this project should not be indexed
    db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertPrivateApplication().getMainBranchComponent();

    underTest.index(db.getSession(), emptyList());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    es.lockWrites(TYPE_PROJECT_MEASURES);

    IndexingResult result = indexProject(project, CREATION);
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
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnAnalysis(branch.uuid());

    assertThat(es.countDocuments(TYPE_PROJECT_MEASURES)).isZero();
  }

  private IndexingResult indexProject(ProjectDto project, Indexers.EntityEvent cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnEntityEvent(dbSession, singletonList(project.getUuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private IndexingResult indexBranches(List<BranchDto> branches, Indexers.BranchEvent cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnBranchEvent(dbSession, branches.stream().map(BranchDto::getUuid).collect(Collectors.toSet()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private void assertThatProjectHasTag(ProjectDto project, String expectedTag) {
    SearchRequest request = prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(new SearchSourceBuilder()
        .query(boolQuery()
          .filter(termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()))
          .filter(termQuery(FIELD_TAGS, expectedTag))));

    assertThat(es.client().search(request).getHits().getHits())
      .extracting(SearchHit::getId)
      .contains(project.getUuid());
  }

  private void assertThatProjectHasMeasure(ProjectDto project, String metric, Double value) {
    SearchRequest request = prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(new SearchSourceBuilder()
        .query(nestedQuery(
          FIELD_MEASURES,
          boolQuery()
            .filter(termQuery(FIELD_MEASURES_MEASURE_KEY, metric))
            .filter(termQuery(FIELD_MEASURES_MEASURE_VALUE, value)),
          ScoreMode.Avg)));

    assertThat(es.client().search(request).getHits().getHits())
      .extracting(SearchHit::getId)
      .contains(project.getUuid());
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

  private void assertThatIndexContainsOnly(SnapshotDto... expectedSnapshots) {
    assertThat(es.getIds(TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrder(
      Arrays.stream(expectedSnapshots).map(this::getProjectUuidFromSnapshot).toArray(String[]::new));
  }

  private void assertThatIndexContainsCreationDate(ProjectData... projectDatas) {
    List<Map<String, Object>> documents = es.getDocuments(TYPE_PROJECT_MEASURES).stream().map(SearchHit::getSourceAsMap).toList();

    List<Tuple> expected = Arrays.stream(projectDatas).map(
        projectData -> tuple(
          projectData.getProjectDto().getKey(),
          projectData.getProjectDto().getCreatedAt()))
      .toList();
    assertThat(documents)
      .extracting(hit -> hit.get("key"), hit -> stringDateToMilliseconds((String) hit.get("createdAt")))
      .containsExactlyInAnyOrderElementsOf(expected);

  }

  private static long stringDateToMilliseconds(String date) {
    return DateTime.parse(date).getMillis();
  }

  private String getProjectUuidFromSnapshot(SnapshotDto s) {
    ProjectDto projectDto = db.getDbClient().projectDao().selectByBranchUuid(db.getSession(), s.getRootComponentUuid()).orElseThrow();
    return projectDto.getUuid();
  }

  private void assertThatIndexContainsOnly(ProjectDto... expectedProjects) {
    assertThat(es.getIds(TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrderElementsOf(
      Arrays.stream(expectedProjects).map(ProjectDto::getUuid).toList());
  }

  private void assertThatQualifierIs(String qualifier, ProjectDto... expectedProjects) {
    String[] expectedComponentUuids = Arrays.stream(expectedProjects).map(ProjectDto::getUuid).toArray(String[]::new);
    assertThatQualifierIs(qualifier, expectedComponentUuids);
  }

  private void assertThatQualifierIs(String qualifier, SnapshotDto... expectedSnapshots) {
    String[] expectedComponentUuids = Arrays.stream(expectedSnapshots)
      .map(this::getProjectUuidFromSnapshot).toArray(String[]::new);
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
