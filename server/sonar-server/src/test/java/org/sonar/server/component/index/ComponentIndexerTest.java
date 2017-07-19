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
package org.sonar.server.component.index;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ComponentUpdateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.ProjectIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_TYPE_COMPONENT;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

public class ComponentIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new ComponentIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private OrganizationDto organization;

  @Before
  public void setUp() {
    organization = OrganizationTesting.newOrganizationDto();
  }

  @Test
  public void index_on_startup() {
    ComponentIndexer indexer = spy(createIndexer());
    doNothing().when(indexer).index();
    indexer.indexOnStartup(null);
    verify(indexer).indexOnStartup(null);
  }

  @Test
  public void index_nothing() {
    index();
    assertThat(count()).isZero();
  }

  @Test
  public void index_everything() {
    insert(ComponentTesting.newPrivateProjectDto(organization));

    index();
    assertThat(count()).isEqualTo(1);
  }

  @Test
  public void index_unexisting_project_while_database_contains_another() {
    insert(ComponentTesting.newPrivateProjectDto(organization, "UUID-1"));

    index("UUID-2");
    assertThat(count()).isEqualTo(0);
  }

  @Test
  public void index_one_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization, "UUID-1");
    insert(project);

    index(project);
    assertThat(count()).isEqualTo(1);
  }

  @Test
  public void index_one_project_containing_a_file() {
    ComponentDto projectComponent = ComponentTesting.newPrivateProjectDto(organization, "UUID-PROJECT-1");
    insert(projectComponent);
    insert(ComponentTesting.newFileDto(projectComponent));

    index(projectComponent);
    assertThat(count()).isEqualTo(2);
  }

  @Test
  public void index_and_update_and_reindex_project() {

    // insert
    ComponentDto component = ComponentTesting.newPrivateProjectDto(organization, "UUID-1").setName("OldName");
    insert(component);

    // verify insert
    index(component);
    assertMatches("OldName", 1);

    // modify
    component.setName("NewName");
    update(component);

    // verify modification
    index(component);
    assertMatches("OldName", 0);
    assertMatches("NewName", 1);
  }

  @Test
  public void index_and_update_and_reindex_project_with_files() {

    // insert
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project).setName("OldFile"));

    // verify insert
    index(project);
    assertMatches("OldFile", 1);

    // modify
    file.setName("NewFile");
    update(file);

    // verify modification
    index(project);
    assertMatches("OldFile", 0);
    assertMatches("NewFile", 1);
  }

  @Test
  public void full_reindexing_on_empty_index() {

    // insert
    ComponentDto project = dbTester.components().insertPrivateProject();
    dbTester.components().insertComponent(ComponentTesting.newFileDto(project).setName("OldFile"));

    // verify insert
    index();
    assertMatches("OldFile", 1);
  }

  private void insert(ComponentDto component) {
    dbTester.components().insertComponent(component);
  }

  private void update(ComponentDto component) {
    ComponentUpdateDto updateComponent = ComponentUpdateDto.copyFrom(component);
    updateComponent.setBChanged(true);
    dbClient.componentDao().update(dbSession, updateComponent);
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbSession, component.getRootUuid());
    dbSession.commit();
  }

  private void index() {
    createIndexer().indexOnStartup(null);
  }

  private void index(ComponentDto component) {
    index(component.uuid());
  }

  private void index(String uuid) {
    createIndexer().indexProject(uuid, ProjectIndexer.Cause.PROJECT_CREATION);
  }

  private long count() {
    return esTester.countDocuments(INDEX_TYPE_COMPONENT);
  }

  private void assertMatches(String nameQuery, int numberOfMatches) {
    assertThat(
      esTester.client()
        .prepareSearch(INDEX_TYPE_COMPONENT)
        .setQuery(matchQuery(SORTABLE_ANALYZER.subField(FIELD_NAME), nameQuery))
        .get()
        .getHits()
        .getTotalHits()).isEqualTo(numberOfMatches);
  }

  private ComponentIndexer createIndexer() {
    return new ComponentIndexer(dbTester.getDbClient(), esTester.client());
  }

}
