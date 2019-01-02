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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.ListAssert;
import org.junit.Before;
import org.junit.Rule;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRule;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.MODULE;
import static org.sonar.api.resources.Qualifiers.PROJECT;

public abstract class ComponentIndexTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ComponentTextSearchFeatureRule features = new ComponentTextSearchFeatureRule();

  protected ComponentIndexer indexer = new ComponentIndexer(db.getDbClient(), es.client());
  protected ComponentIndex index = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);
  protected PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);
  private OrganizationDto organization;

  @Before
  public void setUp() {
    organization = OrganizationTesting.newOrganizationDto();
  }

  protected void assertFileMatches(String query, String... fileNames) {
    ComponentDto[] files = Arrays.stream(fileNames)
      .map(this::indexFile)
      .toArray(ComponentDto[]::new);
    assertSearch(query).containsExactlyInAnyOrder(uuids(files));
  }

  protected void assertNoFileMatches(String query, String... fileNames) {
    Arrays.stream(fileNames)
      .forEach(this::indexFile);
    assertSearch(query).isEmpty();
  }

  protected void assertResultOrder(String query, String... resultsInOrder) {
    ComponentDto project = indexProject("key-1", "Quality Product");
    List<ComponentDto> files = Arrays.stream(resultsInOrder)
      .map(r -> ComponentTesting.newFileDto(project).setName(r))
      .peek(f -> f.setUuid(f.uuid() + "_" + f.name().replaceAll("[^a-zA-Z0-9]", "")))
      .collect(Collectors.toList());

    // index them, but not in the expected order
    files.stream()
      .sorted(Comparator.comparing(ComponentDto::uuid).reversed())
      .forEach(this::index);

    assertExactResults(query, files.toArray(new ComponentDto[0]));
  }

  protected ListAssert<String> assertSearch(String query) {
    return assertSearch(SuggestionQuery.builder().setQuery(query).setQualifiers(asList(PROJECT, MODULE, FILE)).build());
  }

  protected ListAssert<String> assertSearch(SuggestionQuery query) {
    return (ListAssert<String>)assertThat(index.searchSuggestions(query, features.get()).getQualifiers())
      .flatExtracting(ComponentHitsPerQualifier::getHits)
      .extracting(ComponentHit::getUuid);
  }

  protected void assertSearchResults(String query, ComponentDto... expectedComponents) {
    assertSearchResults(SuggestionQuery.builder().setQuery(query).setQualifiers(asList(PROJECT, MODULE, FILE)).build(), expectedComponents);
  }

  protected void assertSearchResults(SuggestionQuery query, ComponentDto... expectedComponents) {
    assertSearch(query).containsOnly(uuids(expectedComponents));
  }

  protected void assertExactResults(String query, ComponentDto... expectedComponents) {
    assertSearch(query).containsExactly(uuids(expectedComponents));
  }

  protected void assertNoSearchResults(String query) {
    assertSearchResults(query);
  }

  protected ComponentDto indexProject(String key, String name) {
    return index(
      ComponentTesting.newPrivateProjectDto(organization, "UUID_" + key)
        .setDbKey(key)
        .setName(name));
  }

  protected ComponentDto newProject(String key, String name) {
    return ComponentTesting.newPrivateProjectDto(organization, "UUID_" + key)
      .setDbKey(key)
      .setName(name);
  }

  protected ComponentDto indexFile(String fileName) {
    ComponentDto project = indexProject("key-1", "SonarQube");
    return indexFile(project, "src/main/java/" + fileName, fileName);
  }

  protected ComponentDto indexFile(ComponentDto project, String fileKey, String fileName) {
    return index(
      ComponentTesting.newFileDto(project)
        .setDbKey(fileKey)
        .setName(fileName));
  }

  protected ComponentDto index(ComponentDto dto) {
    indexer.index(dto);
    authorizationIndexerTester.allowOnlyAnyone(dto);
    return dto;
  }

  protected static String[] uuids(ComponentDto... expectedComponents) {
    return Arrays.stream(expectedComponents).map(ComponentDto::uuid).toArray(String[]::new);
  }
}
