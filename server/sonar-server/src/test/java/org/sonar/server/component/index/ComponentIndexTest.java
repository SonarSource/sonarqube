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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.AbstractListAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexTest {

  private static final Integer TEST_USER_ID = 42;
  private static final String TEST_USER_GROUP = "TestUsers";

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es);

  private ComponentIndex index;
  private ComponentIndexer indexer;
  private OrganizationDto organization;

  @Before
  public void setUp() {
    index = new ComponentIndex(es.client(), userSession);
    indexer = new ComponentIndexer(db.getDbClient(), es.client());
    organization = OrganizationTesting.newOrganizationDto();
  }

  @Test
  public void return_empty_list_if_no_fields_match_query() {
    indexProject("struts", "Apache Struts");

    assertThat(index.search(new ComponentIndexQuery("missing"))).isEmpty();
  }

  @Test
  public void should_not_return_components_that_do_not_match_at_all() {
    indexProject("banana", "Banana Project 1");

    assertNoSearchResults("Apple");
  }

  @Test
  public void search_projects_by_exact_name() {
    ComponentDto struts = indexProject("struts", "Apache Struts");
    indexProject("sonarqube", "SonarQube");

    assertSearchResults("Apache Struts", struts);
    assertSearchResults("APACHE STRUTS", struts);
    assertSearchResults("APACHE struTS", struts);
  }

  @Test
  public void search_file_with_long_name() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/DefaultRubyComponentServiceTestManagerFactory.java", "DefaultRubyComponentServiceTestManagerFactory.java");

    assertSearchResults("DefaultRubyComponentServiceTestManagerFactory", file1);
    assertSearchResults("DefaultRubyComponentServiceTestManagerFactory.java", file1);
    assertSearchResults("RubyComponentServiceTestManager", file1);
    assertSearchResults("te", file1);
  }

  @Test
  public void should_search_by_name_with_two_characters() {
    ComponentDto project = indexProject("struts", "Apache Struts");

    assertSearchResults("st", project);
    assertSearchResults("tr", project);
  }

  @Test
  public void search_projects_by_partial_name() {
    ComponentDto struts = indexProject("struts", "Apache Struts");

    assertSearchResults("truts", struts);
    assertSearchResults("pache", struts);
    assertSearchResults("apach", struts);
    assertSearchResults("che stru", struts);
  }

  @Test
  public void search_projects_and_files_by_partial_name() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");
    indexFile(project, "src/main/java/Foo.java", "Foo.java");

    assertSearchResults("struts", project, file1);
    assertSearchResults("Struts", project, file1);
    assertSearchResults("StrutsManager", file1);
    assertSearchResults("STRUTSMAN", file1);
    assertSearchResults("utsManag", file1);
  }

  @Test
  public void should_find_file_by_file_extension() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    ComponentDto file1 = indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");
    ComponentDto file2 = indexFile(project, "src/main/java/Foo.java", "Foo.java");

    assertSearchResults(".java", file1, file2);
    assertSearchResults("manager.java", file1);

    // do not match
    assertNoSearchResults("somethingStrutsManager.java");
  }

  @Test
  public void should_search_projects_by_exact_case_insensitive_key() {
    ComponentDto project1 = indexProject("keyOne", "Project One");
    indexProject("keyTwo", "Project Two");

    assertSearchResults("keyOne", project1);
    assertSearchResults("keyone", project1);
    assertSearchResults("KEYone", project1);
  }

  @Test
  public void should_search_project_with_dot_in_key() {
    ComponentDto project = indexProject("org.sonarqube", "SonarQube");

    assertSearchResults("org.sonarqube", project);
    assertNoSearchResults("orgsonarqube");
  }

  @Test
  public void should_search_project_with_dash_in_key() {
    ComponentDto project = indexProject("org-sonarqube", "SonarQube");

    assertSearchResults("org-sonarqube", project);
    assertNoSearchResults("orgsonarqube");
  }

  @Test
  public void should_search_project_with_colon_in_key() {
    ComponentDto project = indexProject("org:sonarqube", "SonarQube");

    assertSearchResults("org:sonarqube", project);
    assertNoSearchResults("orgsonarqube");
    assertNoSearchResults("org-sonarqube");
    assertNoSearchResults("org_sonarqube");
  }

  @Test
  public void should_search_project_with_all_special_characters_in_key() {
    ComponentDto project = indexProject("org.sonarqube:sonar-sérvèr_ç", "SonarQube");

    assertSearchResults("org.sonarqube:sonar-sérvèr_ç", project);
  }

  @Test
  public void should_not_return_results_when_searching_by_partial_key() {
    indexProject("theKey", "some name");

    assertNoSearchResults("theke");
    assertNoSearchResults("hekey");
  }

  @Test
  public void filter_results_by_qualifier() {
    ComponentDto project = indexProject("struts", "Apache Struts");
    indexFile(project, "src/main/java/StrutsManager.java", "StrutsManager.java");

    assertSearchResults(new ComponentIndexQuery("struts").setQualifier(Qualifiers.PROJECT), project);
  }

  @Test
  public void should_prefer_key_matching_over_name_matching() {
    ComponentDto project1 = indexProject("quality", "SonarQube");
    ComponentDto project2 = indexProject("sonarqube", "Quality Product");

    assertExactResults("sonarqube", project2, project1);
  }

  @Test
  public void should_limit_the_number_of_results() {
    IntStream.rangeClosed(0, 10).forEach(i -> indexProject("sonarqube" + i, "SonarQube" + i));

    assertSearch(new ComponentIndexQuery("sonarqube").setLimit(5)).hasSize(5);
  }

  @Test
  public void should_not_support_wildcards() {
    indexProject("theKey", "the name");

    assertNoSearchResults("*t*");
    assertNoSearchResults("th?Key");
  }

  @Test
  public void should_find_item_despite_missing_character() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("SonrQube", project);
  }

  @Test
  public void should_find_item_despite_missing_character_and_lowercase() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("sonrqube", project);
  }

  @Test
  public void should_find_item_despite_two_missing_characters_and_lowercase() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("sonqube", project);
  }

  @Test
  public void should_search_for_word_and_suffix() {
    assertFileMatches("plugin java", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_word_and_suffix_in_any_order() {
    assertFileMatches("java plugin", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_two_words() {
    assertFileMatches("abstract factory", "AbstractPluginFactory.java");
  }

  @Test
  public void should_search_for_two_words_in_any_order() {
    assertFileMatches("factory abstract", "AbstractPluginFactory.java");
  }

  @Test
  public void should_find_item_with_at_least_one_matching_word() {
    assertFileMatches("abstract object", "AbstractPluginFactory.java");
  }

  @Test
  public void should_require_at_least_one_matching_word() {
    assertNoFileMatches("monitor object", "AbstractPluginFactory.java");
  }

  @Test
  public void NPE_should_find_NullPointerException() {
    assertFileMatches("NPE", "NullPointerException.java");
  }

  @Test
  public void npe_should_not_find_NullPointerException() {
    assertNoFileMatches("npe", "NullPointerException.java");
  }

  @Test
  public void NuPE_should_find_NullPointerException() {
    assertFileMatches("NuPE", "NullPointerException.java");
  }

  @Test
  public void NPoE_should_find_NullPointerException() {
    assertFileMatches("NPoE", "NullPointerException.java");
  }

  @Test
  public void NPEx_should_find_NullPointerException() {
    assertFileMatches("NPEx", "NullPointerException.java");
  }

  @Test
  public void PE_should_prefer_PointerException_to_NullPointException() {
    ComponentDto file1 = indexFile("NullPointerException.java");
    ComponentDto file2 = indexFile("PointerException.java");

    assertSearch("PE").containsExactly(uuids(file2, file1));
  }

  @Test
  public void should_respect_order_of_camel_case_words() {
    assertNoFileMatches("NuExcPo", "NullPointerException.java");
  }

  @Test
  public void should_respect_confidentiallity() {
    indexer.index(newProject("sonarqube", "Quality Product"));

    // do not give any permissions to that project

    assertNoSearchResults("sonarqube");
    assertNoSearchResults("Quality Product");
  }

  @Test
  public void should_find_project_for_which_the_user_has_direct_permission() {
    login();

    ComponentDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    // give the user explicit access
    authorizationIndexerTester.indexProjectPermission(project.uuid(),
      emptyList(),
      Collections.singletonList((long) TEST_USER_ID));

    assertSearchResults("sonarqube", project);
  }

  @Test
  public void should_find_project_for_which_the_user_has_indirect_permission_through_group() {
    login();

    ComponentDto project = newProject("sonarqube", "Quality Product");
    indexer.index(project);

    // give the user implicit access (though group)
    authorizationIndexerTester.indexProjectPermission(project.uuid(),
      Collections.singletonList(TEST_USER_GROUP),
      emptyList());

    assertSearchResults("sonarqube", project);
  }

  private void login() {
    userSession.login("john").setUserId(TEST_USER_ID).setUserGroups(TEST_USER_GROUP);
  }

  private void assertFileMatches(String query, String... fileNames) {
    ComponentDto[] files = Arrays.stream(fileNames)
      .map(this::indexFile)
      .toArray(ComponentDto[]::new);
    assertSearch(query).containsExactlyInAnyOrder(uuids(files));
  }

  private void assertNoFileMatches(String query, String... fileNames) {
    Arrays.stream(fileNames)
      .forEach(this::indexFile);
    assertSearch(query).isEmpty();
  }

  private AbstractListAssert<?, ? extends List<? extends String>, String> assertSearch(String query) {
    return assertSearch(new ComponentIndexQuery(query));
  }

  private AbstractListAssert<?, ? extends List<? extends String>, String> assertSearch(ComponentIndexQuery query) {
    return assertThat(index.search(query));
  }

  private void assertSearchResults(String query, ComponentDto... expectedComponents) {
    assertSearchResults(new ComponentIndexQuery(query), expectedComponents);
  }

  private void assertSearchResults(ComponentIndexQuery query, ComponentDto... expectedComponents) {
    assertSearch(query).containsOnly(uuids(expectedComponents));
  }

  private void assertExactResults(String query, ComponentDto... expectedComponents) {
    assertSearch(query).containsExactly(uuids(expectedComponents));
  }

  private void assertNoSearchResults(String query) {
    assertSearchResults(query);
  }

  private ComponentDto indexProject(String key, String name) {
    return index(
      ComponentTesting.newProjectDto(organization, "UUID_" + key)
        .setKey(key)
        .setName(name));
  }

  private ComponentDto newProject(String key, String name) {
    return ComponentTesting.newProjectDto(organization, "UUID_" + key)
      .setKey(key)
      .setName(name);
  }

  private ComponentDto indexFile(String fileName) {
    ComponentDto project = indexProject("key-1", "SonarQube");
    return indexFile(project, "src/main/java/" + fileName, fileName);
  }

  private ComponentDto indexFile(ComponentDto project, String fileKey, String fileName) {
    return index(
      ComponentTesting.newFileDto(project)
        .setKey(fileKey)
        .setName(fileName));
  }

  private ComponentDto index(ComponentDto dto) {
    indexer.index(dto);
    authorizationIndexerTester.indexProjectPermission(dto.uuid(),
      Collections.singletonList(DefaultGroups.ANYONE),
      emptyList());
    return dto;
  }

  private static String[] uuids(ComponentDto... expectedComponents) {
    return Arrays.stream(expectedComponents).map(ComponentDto::uuid).toArray(String[]::new);
  }
}
