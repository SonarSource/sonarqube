/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRule;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexSearchTest {
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @Rule
  public ComponentTextSearchFeatureRule features = new ComponentTextSearchFeatureRule();

  private final EntityDefinitionIndexer indexer = new EntityDefinitionIndexer(db.getDbClient(), es.client());
  private final PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);
  private final ComponentIndex underTest = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);

  @Test
  public void filter_by_name() {
    ProjectData ignoredProject = db.components().insertPrivateProject(p -> p.setName("ignored project"));
    ProjectData project = db.components().insertPrivateProject(p -> p.setName("Project Shiny name"));
    index(ignoredProject.getProjectDto(), project.getProjectDto());

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQuery("shiny").build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactlyInAnyOrder(project.projectUuid());
  }

  @Test
  public void filter_by_key_with_exact_match() {
    ProjectData ignoredProject = db.components().insertPrivateProject(p -> p.setKey("ignored-project"));
    ProjectData project = db.components().insertPrivateProject(p -> p.setKey("shiny-project"));
    db.components().insertPrivateProject(p -> p.setKey("another-shiny-project")).getMainBranchComponent();
    index(ignoredProject.getProjectDto(), project.getProjectDto());

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQuery("shiny-project").build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactlyInAnyOrder(project.projectUuid());
  }

  @Test
  public void filter_by_qualifier() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    index(project.getProjectDto());
    index(db.components().getPortfolioDto(portfolio));

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQualifiers(singleton(ComponentQualifiers.PROJECT)).build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactlyInAnyOrder(project.projectUuid());
  }

  @Test
  public void order_by_name_case_insensitive() {
    ProjectData project2 = db.components().insertPrivateProject(p -> p.setName("PROJECT 2"));
    ProjectData project3 = db.components().insertPrivateProject(p -> p.setName("project 3"));
    ProjectData project1 = db.components().insertPrivateProject(p -> p.setName("Project 1"));
    index(project1.getProjectDto(), project2.getProjectDto(), project3.getProjectDto());

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactly(project1.projectUuid(),
      project2.projectUuid(),
      project3.projectUuid());
  }

  @Test
  public void paginate_results() {
    List<ProjectData> projects = IntStream.range(0, 9)
      .mapToObj(i -> db.components().insertPrivateProject(p -> p.setName("project " + i)))
      .toList();
    ProjectDto[] projectDtos = projects.stream().map(ProjectData::getProjectDto).toArray(ProjectDto[]::new);
    index(projectDtos);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions().setPage(2, 3));

    assertThat(result.getUuids()).containsExactlyInAnyOrder(projects.get(3).projectUuid(),
      projects.get(4).projectUuid(),
      projects.get(5).projectUuid());
  }

  @Test
  public void filter_unauthorized_components() {
    ProjectDto unauthorizedProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    indexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project1);
    authorizationIndexerTester.allowOnlyAnyone(project2);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions());

    assertThat(result.getUuids()).containsExactlyInAnyOrder(project1.getUuid(), project2.getUuid())
      .doesNotContain(unauthorizedProject.getUuid());
  }

  private void index(EntityDto... components) {
    indexer.indexAll();
    Arrays.stream(components).forEach(authorizationIndexerTester::allowOnlyAnyone);
  }
}
