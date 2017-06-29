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
package org.sonar.server.measure.index;

import java.util.Date;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(dbTester.getDbClient(), esTester.client());

  @Test
  public void index_on_startup() {
    ProjectMeasuresIndexer indexer = spy(underTest);
    doNothing().when(indexer).indexOnStartup(null);
    indexer.indexOnStartup(null);
    verify(indexer).indexOnStartup(null);
  }

  @Test
  public void index_nothing() {
    underTest.indexOnStartup(null);

    assertThat(esTester.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void index_all_project() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    componentDbTester.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));
    componentDbTester.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));
    componentDbTester.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));

    underTest.indexOnStartup(null);

    assertThat(esTester.countDocuments(INDEX_TYPE_PROJECT_MEASURES)).isEqualTo(3);
  }

  /**
   * Provisioned projects don't have analysis yet
   */
  @Test
  public void index_provisioned_projects() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    underTest.indexOnStartup(null);

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }

  @Test
  public void indexProject_indexes_provisioned_project() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_CREATION);

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }

  @Test
  public void indexProject_indexes_project_when_its_key_is_updated() {
    ComponentDto project = componentDbTester.insertPrivateProject();

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }

  @Test
  public void index_one_project() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project);
    componentDbTester.insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.NEW_ANALYSIS);

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }

  @Test
  public void update_existing_document_when_indexing_one_project() throws Exception {
    String uuid = "PROJECT-UUID";
    esTester.putDocuments(INDEX_TYPE_PROJECT_MEASURES, new ProjectMeasuresDoc()
      .setId(uuid)
      .setKey("Old Key")
      .setName("Old Name")
      .setTags(singletonList("old tag"))
      .setAnalysedAt(new Date(1_000_000L)));
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), uuid).setKey("New key").setName("New name").setTagsString("new tag");
    SnapshotDto analysis = componentDbTester.insertProjectAndSnapshot(project);

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.NEW_ANALYSIS);

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(uuid);
    SearchRequestBuilder request = esTester.client()
      .prepareSearch(INDEX_TYPE_PROJECT_MEASURES)
      .setQuery(boolQuery().must(matchAllQuery()).filter(
        boolQuery()
          .must(termQuery("_id", uuid))
          .must(termQuery(FIELD_KEY, "New key"))
          .must(termQuery(FIELD_NAME, "New name"))
          .must(termQuery(FIELD_TAGS, "new tag"))
          .must(termQuery(FIELD_ANALYSED_AT, new Date(analysis.getCreatedAt())))));
    assertThat(request.get().getHits()).hasSize(1);
  }

  @Test
  public void delete_project() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project1);
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project2);
    ComponentDto project3 = ComponentTesting.newPrivateProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project3);
    underTest.indexOnStartup(null);

    underTest.deleteProject(project1.uuid());

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project2.uuid(), project3.uuid());
  }

  @Test
  public void does_nothing_when_deleting_unknown_project() throws Exception {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert());
    componentDbTester.insertProjectAndSnapshot(project);
    underTest.indexOnStartup(null);

    underTest.deleteProject("UNKNOWN");

    assertThat(esTester.getIds(INDEX_TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }
}
