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
package org.sonar.server.component.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingResult;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;
import static org.sonar.server.es.Indexers.EntityEvent.CREATION;
import static org.sonar.server.es.Indexers.EntityEvent.DELETION;
import static org.sonar.server.es.Indexers.EntityEvent.PERMISSION_CHANGE;
import static org.sonar.server.es.Indexers.EntityEvent.PROJECT_TAGS_UPDATE;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public class EntityDefinitionIndexerIT {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private EntityDefinitionIndexer underTest;

  @Before
  public void setup() {
    underTest = new EntityDefinitionIndexer(db.getDbClient(), es.client());
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void test_getIndexTypes() {
    assertThat(underTest.getIndexTypes()).containsExactly(TYPE_COMPONENT);
  }

  @Test
  public void indexOnStartup_does_nothing_if_no_projects() {
    underTest.indexOnStartup(emptySet());

    assertThatIndexHasSize(0);
  }

  @Test
  public void indexOnStartup_indexes_all_components() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project1, project2);
  }

  @Test
  public void indexOAll_indexes_all_components() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();

    underTest.indexAll();

    assertThatIndexContainsOnly(project1, project2);
  }

  @Test
  public void map_fields() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
    ComponentDoc doc = es.getDocuments(TYPE_COMPONENT, ComponentDoc.class).get(0);
    assertThat(doc.getId()).isEqualTo(project.getUuid());
    assertThat(doc.getKey()).isEqualTo(project.getKey());
    assertThat(doc.getName()).isEqualTo(project.getName());
  }

  @Test
  public void indexOnStartup_does_not_index_non_main_branches() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnStartup(emptySet());

    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnStartup_fixes_corrupted_portfolios_if_possible_and_then_indexes_them() throws Exception {
    underTest = new EntityDefinitionIndexer(db.getDbClient(), es.client());
    String uuid = "portfolioUuid1";
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    PortfolioDto corruptedPortfolio = new PortfolioDto()
      .setKey("portfolio1")
      .setName("My Portfolio")
      .setSelectionMode(PortfolioDto.SelectionMode.NONE)
      .setUuid(uuid)
      .setRootUuid(uuid);
    db.getDbClient().portfolioDao().insert(dbSession, corruptedPortfolio, false);

    // corrupt the portfolio in a fixable way (root portfolio with self-referential parent_uuid)
    dbSession.getSqlSession().getConnection().prepareStatement(format("UPDATE portfolios SET parent_uuid = '%s' where uuid = '%s'", uuid, uuid))
      .execute();
    dbSession.commit();
    Optional<EntityDto> entity = dbClient.entityDao().selectByUuid(dbSession, uuid);

    assertThat(entity).isPresent();
    assertThat(entity.get().getAuthUuid()).isNull();

    underTest.indexOnStartup(emptySet());

    assertThat(logTester.logs()).contains("Fixing corrupted portfolio tree for root portfolio " + corruptedPortfolio.getUuid());
    assertThatIndexContainsOnly(project, corruptedPortfolio);
  }

  @Test
  public void indexOnStartup_logs_warning_about_corrupted_portfolios_that_cannot_be_fixed_automatically() throws Exception {
    underTest = new EntityDefinitionIndexer(db.getDbClient(), es.client());
    String uuid = "portfolioUuid1";
    PortfolioDto corruptedPortfolio = new PortfolioDto()
      .setKey("portfolio1")
      .setName("My Portfolio")
      .setSelectionMode(PortfolioDto.SelectionMode.NONE)
      .setUuid(uuid)
      .setRootUuid(uuid);
    db.getDbClient().portfolioDao().insert(dbSession, corruptedPortfolio, false);

    // corrupt the portfolio in an un-fixable way (non-existent parent)
    dbSession.getSqlSession().getConnection().prepareStatement(format("UPDATE portfolios SET parent_uuid = 'junk_uuid' where uuid = '%s'", uuid))
      .execute();
    dbSession.commit();
    Optional<EntityDto> entity = dbClient.entityDao().selectByUuid(dbSession, uuid);

    assertThat(entity).isPresent();
    assertThat(entity.get().getAuthUuid()).isNull();

    assertThatException()
      .isThrownBy(() -> underTest.indexOnStartup(emptySet()));

    assertThat(logTester.logs()).contains("Detected portfolio tree corruption for portfolio " + corruptedPortfolio.getUuid());

  }

  @Test
  public void indexOnAnalysis_indexes_project() {
    ProjectData project = db.components().insertPrivateProject();

    underTest.indexOnAnalysis(project.getMainBranchComponent().uuid());

    assertThatIndexContainsOnly(project.getProjectDto());
  }

  @Test
  public void indexOnAnalysis_indexes_new_components() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    underTest.indexOnAnalysis(projectData.getMainBranchComponent().uuid());
    assertThatIndexContainsOnly(project);

    underTest.indexOnAnalysis(projectData.getMainBranchComponent().uuid());
    assertThatIndexContainsOnly(project);
  }

  @Test
  public void indexOnAnalysis_does_not_index_non_main_branches() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));

    underTest.indexOnAnalysis(branch.getUuid());

    assertThatIndexHasSize(0);
  }

  @Test
  public void do_not_update_index_on_project_tag_update() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    indexProject(project, PROJECT_TAGS_UPDATE);

    assertThatIndexHasSize(0);
  }

  @Test
  public void do_not_update_index_on_permission_change() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    indexProject(project, PERMISSION_CHANGE);

    assertThatIndexHasSize(0);
  }

  @Test
  public void update_index_on_project_creation() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    IndexingResult result = indexProject(project, CREATION);

    assertThatIndexContainsOnly(project);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getSuccess()).isOne();
  }

  @Test
  public void delete_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    indexProject(project, CREATION);
    assertThatIndexHasSize(1);

    db.getDbClient().purgeDao().deleteProject(db.getSession(), project.getUuid(), PROJECT, project.getName(), project.getKey());
    indexProject(project, DELETION);

    assertThatIndexHasSize(0);
  }

  @Test
  public void indexOnAnalysis_updates_index_on_changes() {
    ProjectData project = db.components().insertPrivateProject();
    ProjectDto projectDto = project.getProjectDto();

    underTest.indexOnAnalysis(project.getMainBranchDto().getUuid());
    assertThatEntityHasName(projectDto.getUuid(), projectDto.getName());

    // modify
    projectDto.setName("NewName");

    db.getDbClient().projectDao().update(dbSession, projectDto);
    db.commit();

    // verify that index is updated
    underTest.indexOnAnalysis(project.getMainBranchDto().getUuid());

    assertThatIndexContainsOnly(projectDto.getUuid());
    assertThatEntityHasName(projectDto.getUuid(), "NewName");
  }

  @Test
  public void errors_during_indexing_are_recovered() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    es.lockWrites(TYPE_COMPONENT);

    IndexingResult result = indexProject(project1, CREATION);
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();

    // index is still read-only, fail to recover
    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isOne();
    assertThat(es.countDocuments(TYPE_COMPONENT)).isZero();

    es.unlockWrites(TYPE_COMPONENT);

    result = recover();
    assertThat(result.getTotal()).isOne();
    assertThat(result.getFailures()).isZero();
    assertThatIndexContainsOnly(project1);
  }

  private IndexingResult indexProject(ProjectDto project, Indexers.EntityEvent cause) {
    DbSession dbSession = db.getSession();
    Collection<EsQueueDto> items = underTest.prepareForRecoveryOnEntityEvent(dbSession, singletonList(project.getUuid()), cause);
    dbSession.commit();
    return underTest.index(dbSession, items);
  }

  private IndexingResult recover() {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), System.currentTimeMillis() + 1_000L, 10);
    return underTest.index(db.getSession(), items);
  }

  private void assertThatIndexHasSize(int expectedSize) {
    assertThat(es.countDocuments(TYPE_COMPONENT)).isEqualTo(expectedSize);
  }

  private void assertThatIndexContainsOnly(EntityDto... expectedEntities) {
    assertThat(es.getIds(TYPE_COMPONENT)).containsExactlyInAnyOrder(
      Arrays.stream(expectedEntities).map(EntityDto::getUuid).toArray(String[]::new));
  }

  private void assertThatIndexContainsOnly(String uuid) {
    assertThat(es.getIds(TYPE_COMPONENT)).containsExactlyInAnyOrder(uuid);
  }

  private void assertThatEntityHasName(String uuid, String expectedName) {
    SearchHit[] hits = es.client()
      .search(EsClient.prepareSearch(TYPE_COMPONENT.getMainType())
        .source(new SearchSourceBuilder()
          .query(matchQuery(SORTABLE_ANALYZER.subField(FIELD_NAME), expectedName))))
      .getHits()
      .getHits();
    assertThat(hits)
      .extracting(SearchHit::getId)
      .contains(uuid);
  }
}
