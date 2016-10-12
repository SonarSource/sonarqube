/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.project.es;

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
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.project.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexerTest {

  @Rule
  public EsTester esTester = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);

  ProjectMeasuresIndexer underTest = new ProjectMeasuresIndexer(dbTester.getDbClient(), esTester.client());

  @Test
  public void index_nothing() {
    underTest.index();

    assertThat(esTester.countDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).isZero();
  }

  @Test
  public void index_all_project() {
    componentDbTester.insertProjectAndSnapshot(newProjectDto());

    underTest.index();

    assertThat(esTester.countDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).isEqualTo(1);
  }

  @Test
  public void index_one_project() throws Exception {
    ComponentDto project = newProjectDto();
    componentDbTester.insertProjectAndSnapshot(project);
    componentDbTester.insertProjectAndSnapshot(newProjectDto());

    underTest.index(project.uuid());

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).containsOnly(project.uuid());
  }

  @Test
  public void update_existing_document_when_indexing_one_project() throws Exception {
    String uuid = "PROJECT-UUID";
    esTester.putDocuments(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, new ProjectMeasuresDoc()
      .setId(uuid)
      .setKey("Old Key")
      .setName("Old Name")
      .setAnalysedAt(new Date(1_000_000L)));
    ComponentDto project = newProjectDto(uuid).setKey("New key").setName("New name");
    SnapshotDto analysis = componentDbTester.insertProjectAndSnapshot(project);

    underTest.index(project.uuid());

    assertThat(esTester.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES)).containsOnly(uuid);
    SearchRequestBuilder request = esTester.client()
      .prepareSearch(INDEX_PROJECT_MEASURES)
      .setTypes(TYPE_PROJECT_MEASURES)
      .setQuery(boolQuery().must(matchAllQuery()).filter(
        boolQuery()
          .must(termQuery("_id", uuid))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_KEY, "New key"))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_NAME, "New name"))
          .must(termQuery(ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT, new Date(analysis.getCreatedAt())))));
    assertThat(request.get().getHits()).hasSize(1);
  }
}
