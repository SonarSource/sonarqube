/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRule;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class ComponentIndexSearchTest {
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ComponentTextSearchFeatureRule features = new ComponentTextSearchFeatureRule();

  private ComponentIndexer indexer = new ComponentIndexer(db.getDbClient(), es.client());
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);

  private ComponentIndex underTest = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);

  @Test
  public void filter_by_language() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto javaFile = db.components().insertComponent(newFileDto(project).setLanguage("java"));
    ComponentDto jsFile1 = db.components().insertComponent(newFileDto(project).setLanguage("js"));
    ComponentDto jsFile2 = db.components().insertComponent(newFileDto(project).setLanguage("js"));
    index(project);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setLanguage("js").build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(jsFile1.uuid(), jsFile2.uuid());
  }

  @Test
  public void filter_by_name() {
    ComponentDto ignoredProject = db.components().insertPrivateProject(p -> p.setName("ignored project"));
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("Project Shiny name"));
    index(ignoredProject, project);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQuery("shiny").build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(project.uuid());
  }

  @Test
  public void filter_by_key_with_exact_match() {
    ComponentDto ignoredProject = db.components().insertPrivateProject(p -> p.setDbKey("ignored-project"));
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("shiny-project"));
    ComponentDto anotherIgnoreProject = db.components().insertPrivateProject(p -> p.setDbKey("another-shiny-project"));
    index(ignoredProject, project);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQuery("shiny-project").build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(project.uuid());
  }

  @Test
  public void filter_by_qualifier() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    index(project);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setQualifiers(singleton(Qualifiers.FILE)).build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(file.uuid());
  }

  @Test
  public void filter_by_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    index(project, anotherProject);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().setOrganization(organization.getUuid()).build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(project.uuid());
  }

  @Test
  public void order_by_name_case_insensitive() {
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("PROJECT 2"));
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setName("project 3"));
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("Project 1"));
    index(project1, project2, project3);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions());

    assertThat(result.getIds()).containsExactly(project1.uuid(), project2.uuid(), project3.uuid());
  }

  @Test
  public void paginate_results() {
    List<ComponentDto> projects = IntStream.range(0, 9)
      .mapToObj(i -> db.components().insertPrivateProject(p -> p.setName("project " + i)))
      .collect(Collectors.toList());
    index(projects.toArray(new ComponentDto[0]));

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions().setPage(2, 3));

    assertThat(result.getIds()).containsExactlyInAnyOrder(projects.get(3).uuid(), projects.get(4).uuid(), projects.get(5).uuid());
  }

  @Test
  public void filter_unauthorized_components() {
    ComponentDto unauthorizedProject = db.components().insertPrivateProject();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    indexer.indexOnStartup(emptySet());
    authorizationIndexerTester.allowOnlyAnyone(project1);
    authorizationIndexerTester.allowOnlyAnyone(project2);

    SearchIdResult<String> result = underTest.search(ComponentQuery.builder().build(), new SearchOptions());

    assertThat(result.getIds()).containsExactlyInAnyOrder(project1.uuid(), project2.uuid())
      .doesNotContain(unauthorizedProject.uuid());
  }

  private void index(ComponentDto... components) {
    indexer.indexOnStartup(emptySet());
    Arrays.stream(components).forEach(c -> authorizationIndexerTester.allowOnlyAnyone(c));
  }
}
