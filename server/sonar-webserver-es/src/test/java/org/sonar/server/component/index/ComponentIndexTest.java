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
import java.util.Comparator;
import java.util.List;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRule;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentQualifiers.FILE;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;

public abstract class ComponentIndexTest {

  @RegisterExtension
  public EsTester es = EsTester.create();
  @RegisterExtension
  public DbTester db = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();

  @RegisterExtension
  public ComponentTextSearchFeatureRule features = new ComponentTextSearchFeatureRule();

  protected EntityDefinitionIndexer indexer = new EntityDefinitionIndexer(db.getDbClient(), es.client());
  protected ComponentIndex index = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);
  protected PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);

  protected void assertResultOrder(String query, String... resultsInOrder) {
    List<ProjectDto> projects = Arrays.stream(resultsInOrder)
      .map(r -> db.components().insertPublicProject(c -> c.setName(r)).getProjectDto())
      .toList();

    // index them, but not in the expected order
    projects.stream()
      .sorted(Comparator.comparing(ProjectDto::getUuid).reversed())
      .forEach(this::index);

    assertExactResults(query, projects.toArray(new ProjectDto[0]));
  }

  protected ListAssert<String> assertSearch(String query) {
    return assertSearch(SuggestionQuery.builder().setQuery(query).setQualifiers(asList(PROJECT, FILE)).build());
  }

  protected ListAssert<String> assertSearch(SuggestionQuery query) {
    return (ListAssert<String>) assertThat(index.searchSuggestionsV2(query, features.get()).getQualifiers())
      .flatExtracting(ComponentHitsPerQualifier::getHits)
      .extracting(ComponentHit::uuid);
  }

  protected void assertSearchResults(String query, EntityDto... expectedComponents) {
    assertSearchResults(query, List.of(PROJECT), expectedComponents);
  }

  protected void assertSearchResults(String query, List<String> queryQualifiers, EntityDto... expectedComponents) {
    assertSearchResults(SuggestionQuery.builder().setQuery(query).setQualifiers(queryQualifiers).build(), expectedComponents);
  }

  protected void assertSearchResults(SuggestionQuery query, EntityDto... expectedComponents) {
    assertSearch(query).containsOnly(uuids(expectedComponents));
  }

  protected void assertExactResults(String query, ProjectDto... expectedComponents) {
    assertSearch(query).containsExactly(uuids(expectedComponents));
  }

  protected void assertNoSearchResults(String query, String... qualifiers) {
    assertSearchResults(query, List.of(qualifiers));
  }

  protected ProjectDto indexProject(String name) {
    return indexProject(name, name);
  }

  protected ProjectDto indexProject(String key, String name) {
    return index(db.components().insertPublicProject("UUID" + key, c -> c.setKey(key).setName(name)).getProjectDto());
  }

  protected EntityDto newProject(String key, String name) {
    return db.components().insertPublicProject("UUID_" + key, c -> c.setKey(key).setName(name)).getProjectDto();
  }

  protected ProjectDto index(ProjectDto dto) {
    indexer.index(dto);
    authorizationIndexerTester.allowOnlyAnyone(dto);
    return dto;
  }

  protected static String[] uuids(EntityDto... expectedComponents) {
    return Arrays.stream(expectedComponents).map(EntityDto::getUuid).toArray(String[]::new);
  }
}
