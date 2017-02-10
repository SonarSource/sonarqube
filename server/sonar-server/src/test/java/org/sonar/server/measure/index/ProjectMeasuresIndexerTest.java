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
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;

public class ProjectMeasuresIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  private ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(system2, dbTester.getDbClient(), esTester.client());

  @Test
  public void index_nothing() {
    underTest.index();

    assertThat(esTester.countDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).isZero();
  }

  @Test
  public void index_all_project() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    componentDbTester.insertProjectAndSnapshot(newProjectDto(organizationDto));
    componentDbTester.insertProjectAndSnapshot(newProjectDto(organizationDto));
    componentDbTester.insertProjectAndSnapshot(newProjectDto(organizationDto));

    underTest.index();

    assertThat(esTester.countDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).isEqualTo(3);
  }

  /**
   * Provisioned projects don't have analysis yet
   */
  @Test
  public void index_provisioned_projects() {
    ComponentDto project = componentDbTester.insertProject();

    underTest.index();

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void indexProject_indexes_provisioned_project() {
    ComponentDto project = componentDbTester.insertProject();

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_CREATION);

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void indexProject_indexes_project_when_its_key_is_updated() {
    ComponentDto project = componentDbTester.insertProject();

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void index_one_project() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = newProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project);
    componentDbTester.insertProjectAndSnapshot(newProjectDto(organizationDto));

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.NEW_ANALYSIS);

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void update_existing_document_when_indexing_one_project() throws Exception {
    String uuid = "PROJECT-UUID";
    esTester.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, new ProjectMeasuresDoc()
      .setId(uuid)
      .setKey("Old Key")
      .setName("Old Name")
      .setAnalysedAt(new Date(1_000_000L)));
    ComponentDto project = newProjectDto(dbTester.getDefaultOrganization(), uuid).setKey("New key").setName("New name");
    SnapshotDto analysis = componentDbTester.insertProjectAndSnapshot(project);

    underTest.indexProject(project.uuid(), ProjectIndexer.Cause.NEW_ANALYSIS);

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(uuid);
    SearchRequestBuilder request = esTester.client()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURE)
      .setQuery(boolQuery().must(matchAllQuery()).filter(
        boolQuery()
          .must(termQuery("_id", uuid))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_KEY, "New key"))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_NAME, "New name"))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT, new Date(analysis.getCreatedAt())))));
    assertThat(request.get().getHits()).hasSize(1);
  }

  @Test
  public void delete_project() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project1 = newProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project1);
    ComponentDto project2 = newProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project2);
    ComponentDto project3 = newProjectDto(organizationDto);
    componentDbTester.insertProjectAndSnapshot(project3);
    underTest.index();

    underTest.deleteProject(project1.uuid());

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project2.uuid(), project3.uuid());
  }

  @Test
  public void does_nothing_when_deleting_unknown_project() throws Exception {
    ComponentDto project = newProjectDto(dbTester.organizations().insert());
    componentDbTester.insertProjectAndSnapshot(project);
    underTest.index();

    underTest.deleteProject("UNKNOWN");

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }
}
