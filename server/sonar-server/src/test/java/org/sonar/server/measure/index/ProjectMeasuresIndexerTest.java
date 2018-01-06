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
package org.sonar.server.measure.index;

import java.util.Arrays;
import java.util.Collection;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ProjectIndexer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_CREATION;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_DELETION;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_KEY_UPDATE;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_TAGS_UPDATE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public DbTester db = DbTester.create(system2);

  private ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(db.getDbClient(), es.client());

  @Test
  public void index_nothing() {
    underTest.indexOnStartup(emptySet());

    assertThat(es.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void indexOnStartup_indexes_all_projects() {
    OrganizationDto organization = db.organizations().insert();
    SnapshotDto project1 = db.components().insertProjectAndSnapshot(newPrivateProjectDto(organization));
    SnapshotDto project2 = db.components().insertProjectAndSnapshot(newPrivateProjectDto(organization));
    SnapshotDto project3 = db.components().insertProjectAndSnapshot(newPrivateProjectDto(organization));

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2, project3);
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
  public void indexOnAnalysis_indexes_provisioned_project() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    underTest.indexOnAnalysis(project1.uuid());

    assertThatIndexContainsOnly(project1);
  }

  @Test
  public void update_index_when_project_key_is_updated() {
    ComponentDto project = db.components().insertPrivateProject();

    IndexingResult result = indexProject(project, PROJECT_KEY_UPDATE);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getSuccess()).isEqualTo(1L);
  }

  @Test
  public void update_index_when_project_is_created() {
    ComponentDto project = db.components().insertPrivateProject();

    IndexingResult result = indexProject(project, PROJECT_CREATION);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getSuccess()).isEqualTo(1L);
  }

  @Test
  public void update_index_when_project_tags_are_updated() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setTagsString("foo"));
    indexProject(project, PROJECT_CREATION);
    assertThatProjectHasTag(project, "foo");

    project.setTagsString("bar");
    db.getDbClient().componentDao().updateTags(db.getSession(), project);
    IndexingResult result = indexProject(project, PROJECT_TAGS_UPDATE);

    assertThatProjectHasTag(project, "bar");
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getSuccess()).isEqualTo(1L);
  }

  @Test
  public void delete_doc_from_index_when_project_is_deleted() {
    ComponentDto project = db.components().insertPrivateProject();
    indexProject(project, PROJECT_CREATION);
    assertThatIndexContainsOnly(project);

    db.getDbClient().componentDao().delete(db.getSession(), project.getId());
    IndexingResult result = indexProject(project, PROJECT_DELETION);

    assertThat(es.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isEqualTo(0);
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getSuccess()).isEqualTo(1L);
  }

  @Test
  public void do_nothing_if_no_projects_to_index() {
    // this project should not be indexed
    db.components().insertPrivateProject();

    underTest.index(db.getSession(), emptyList());

    assertThat(es.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isEqualTo(0);
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ComponentDto project = db.components().insertPrivateProject();
    es.lockWrites(INDEX_TYPE_PROJECT_MEASURES);

    IndexingResult result = indexProject(project, PROJECT_CREATION);
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(1L);

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(1L);
    assertThat(es.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isEqualTo(0);
    assertThatEsQueueTableHasSize(1);

    es.unlockWrites(INDEX_TYPE_PROJECT_MEASURES);

    result = recover();
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getFailures()).isEqualTo(0L);
    assertThatEsQueueTableHasSize(0);
    assertThatIndexContainsOnly(project);
  }

  @Test
  public void non_main_branches_are_not_indexed_during_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnAnalysis(branch.uuid());

    assertThat(es.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isEqualTo(0);
  }

  private IndexingResult indexProject(ComponentDto project, ProjectIndexer.Cause cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecovery(dbSession, singletonList(project.uuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private void assertThatProjectHasTag(ComponentDto project, String expectedTag) {
    SearchRequestBuilder request = es.client()
      .prepareSearch(INDEX_TYPE_PROJECT_MEASURES)
      .setQuery(boolQuery().filter(termQuery(FIELD_TAGS, expectedTag)));
    assertThat(request.get().getHits().getHits())
      .extracting(SearchHit::getId)
      .contains(project.uuid());
  }

  private void assertThatEsQueueTableHasSize(int expectedSize) {
    assertThat(db.countRowsOfTable("es_queue")).isEqualTo(expectedSize);
  }

  private void assertThatIndexContainsOnly(SnapshotDto... expectedProjects) {
    assertThat(es.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrder(
      Arrays.stream(expectedProjects).map(SnapshotDto::getComponentUuid).toArray(String[]::new));
  }

  private void assertThatIndexContainsOnly(ComponentDto... expectedProjects) {
    assertThat(es.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsExactlyInAnyOrder(
      Arrays.stream(expectedProjects).map(ComponentDto::uuid).toArray(String[]::new));
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }

}
