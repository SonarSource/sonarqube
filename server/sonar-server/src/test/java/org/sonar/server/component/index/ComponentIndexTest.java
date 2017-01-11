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
package org.sonar.server.component.index;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexTest {

  private static final String BLA = "bla";
  private static final String UUID_DOC = "UUID-DOC-";
  private static final String UUID_DOC_1 = UUID_DOC + "1";
  private static final String KEY = "KEY-";
  private static final String KEY_1 = KEY + "1";

  private static final String PREFIX = "Son";
  private static final String MIDDLE = "arQ";
  private static final String SUFFIX = "ube";
  private static final String PREFIX_MIDDLE_SUFFIX = PREFIX + MIDDLE + SUFFIX;

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentIndex index;
  private ComponentIndexer indexer;
  private OrganizationDto organization;

  @Before
  public void setUp() {
    index = new ComponentIndex(es.client());
    indexer = new ComponentIndexer(db.getDbClient(), es.client());
    organization = OrganizationTesting.newOrganizationDto();
  }

  @Test
  public void empty_search() {
    assertSearch(emptyList(), BLA, emptyList());
  }

  @Test
  public void exact_match_search() {
    assertMatch(BLA, BLA);
  }

  @Test
  public void ignore_case() {
    assertMatch("bLa", "BlA");
  }

  @Test
  public void search_for_different_qualifier() {

    // create a component of type project
    ComponentDto project = ComponentTesting
      .newProjectDto(organization, UUID_DOC_1)
      .setName(BLA)
      .setKey(BLA);

    // search for components of type file
    ComponentIndexQuery fileQuery = new ComponentIndexQuery(BLA);
    fileQuery.addQualifier(Qualifiers.FILE);

    // should not have any results
    assertThat(search(asList(project), fileQuery)).isEmpty();
  }

  @Test
  public void prefix_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, PREFIX);
  }

  @Test
  public void middle_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, MIDDLE);
  }

  @Test
  public void suffix_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, SUFFIX);
  }

  @Test
  public void exact_match_should_be_shown_first() {
    ComponentDto good = newDoc(UUID_DOC + 1, "Current SonarQube Plattform");
    ComponentDto better = newDoc(UUID_DOC + 2, "SonarQube Plattform");

    assertThat(search(asList(good, better), "SonarQube"))
      .containsExactly(better.uuid(), good.uuid());
  }

  @Test
  public void do_not_interpret_input() {
    assertNotMatch(BLA, "*");
  }

  @Test
  public void key_match_search() {
    assertSearch(
      asList(newDoc(UUID_DOC_1, "name is not a match", "matchingKey")),
      "matchingKey",
      asList(UUID_DOC_1));
  }

  @Test
  public void unmatching_search() {
    assertNotMatch(BLA, "blubb");
  }

  @Test
  public void limit_number_of_documents() {
    Collection<ComponentDto> docs = IntStream
      .rangeClosed(1, 42)
      .mapToObj(i -> newDoc(UUID_DOC + i, BLA, KEY + i))
      .collect(Collectors.toList());

    int pageSize = 41;
    ComponentIndexQuery componentIndexQuery = new ComponentIndexQuery(BLA)
      .addQualifier(Qualifiers.PROJECT)
      .setLimit(pageSize);
    assertThat(search(docs, componentIndexQuery)).hasSize(pageSize);
  }

  private void assertMatch(String name, String query) {
    assertSearch(
      asList(newDoc(name)),
      query,
      asList(UUID_DOC_1));
  }

  private void assertNotMatch(String name, String query) {
    assertSearch(
      asList(newDoc(name)),
      query,
      emptyList());
  }

  private ComponentDto newDoc(String name) {
    return newDoc(UUID_DOC_1, name);
  }

  private ComponentDto newDoc(String uuid, String name) {
    return newDoc(uuid, name, KEY_1);
  }

  private ComponentDto newDoc(String uuid, String name, String key) {
    return ComponentTesting
      .newProjectDto(organization, uuid)
      .setName(name)
      .setKey(key);
  }

  private void assertSearch(Collection<ComponentDto> input, String queryText, Collection<String> expectedOutput) {
    assertThat(search(input, queryText))
      .hasSameElementsAs(expectedOutput);
  }

  private List<String> search(Collection<ComponentDto> input, String query) {
    return search(input, new ComponentIndexQuery(query).addQualifier(Qualifiers.PROJECT));
  }

  private List<String> search(Collection<ComponentDto> input, ComponentIndexQuery query) {
    input.stream().forEach(indexer::index);
    return index.search(query);
  }
}
