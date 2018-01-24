/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.SearchProjectsWsResponse;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.projects.CreateRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.concat;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.sanitizeTimezones;

/**
 * Tests WS api/components/search_projects
 */
public class ProjectFilterTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private Organization organization;

  @Before
  public void setUp() {
    organization = tester.organizations().generate();
    restoreProfile(orchestrator, ProjectFilterTest.class.getResource("/projectSearch/SearchProjectsTest/with-many-rules.xml"), organization.getKey());
  }

  @Test
  public void filter_projects_by_measure_values() throws Exception {
    String projectKey = newProjectKey();
    analyzeProject(projectKey, "shared/xoo-sample");

    verifyFilterMatches(projectKey, "ncloc > 1");
    verifyFilterMatches(projectKey, "ncloc > 1 and duplicated_lines_density <= 100");
    verifyFilterDoesNotMatch("ncloc <= 1");
  }

  @Test
  public void find_projects_with_no_data() throws Exception {
    String projectKey = newProjectKey();
    analyzeProject(projectKey, "shared/xoo-sample");

    verifyFilterMatches(projectKey, "coverage = NO_DATA");
    verifyFilterDoesNotMatch("ncloc = NO_DATA");
  }

  @Test
  public void provisioned_projects_should_be_included_to_results() throws Exception {
    String projectKey = newProjectKey();
    tester.wsClient().projects().create(new CreateRequest().setProject(projectKey).setName(projectKey).setOrganization(organization.getKey()));

    SearchProjectsWsResponse response = searchProjects(new SearchProjectsRequest().setOrganization(organization.getKey()));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(projectKey);
  }

  @Test
  public void return_leak_period_date() throws Exception {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");
    // This project has a leak period
    String projectKey1 = newProjectKey();
    analyzeProject(projectKey1, "shared/xoo-sample", "sonar.projectDate", "2016-12-31");
    analyzeProject(projectKey1, "shared/xoo-sample");
    // This project has only one analysis, so no leak period
    String projectKey2 = newProjectKey();
    analyzeProject(projectKey2, "shared/xoo-sample");
    // This project is provisioned, so has no leak period
    String projectKey3 = newProjectKey();
    tester.wsClient().projects().create(new CreateRequest().setProject(projectKey3).setName(projectKey3).setOrganization(organization.getKey()));

    SearchProjectsWsResponse response = searchProjects(
      new SearchProjectsRequest().setF(singletonList("leakPeriodDate")).setOrganization(organization.getKey()));

    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::hasLeakPeriodDate)
      .containsOnly(
        tuple(projectKey1, true),
        tuple(projectKey2, false),
        tuple(projectKey3, false));
    Component project1 = response.getComponentsList().stream().filter(component -> component.getKey().equals(projectKey1)).findFirst()
      .orElseThrow(() -> new IllegalStateException("Project1 is not found"));
    assertThat(sanitizeTimezones(project1.getLeakPeriodDate())).isEqualTo("2016-12-31T00:00:00+0000");
  }

  @Test
  public void filter_by_text_query() throws IOException {
    analyzeProject("project1", "shared/xoo-sample", "sonar.projectName", "apachee");
    analyzeProject("project2", "shared/xoo-sample", "sonar.projectName", "Apache");
    analyzeProject("project3", "shared/xoo-multi-modules-sample", "sonar.projectName", "Apache Foundation");
    analyzeProject("project4", "shared/xoo-multi-modules-sample", "sonar.projectName", "Windows");

    // Search only by text query
    assertThat(searchProjects("query = \"apache\"").getComponentsList()).extracting(Component::getKey).containsExactly("project2", "project3", "project1");
    assertThat(searchProjects("query = \"pAch\"").getComponentsList()).extracting(Component::getKey).containsExactly("project2", "project3", "project1");
    assertThat(searchProjects("query = \"hee\"").getComponentsList()).extracting(Component::getKey).containsExactly("project1");
    assertThat(searchProjects("query = \"project1\"").getComponentsList()).extracting(Component::getKey).containsExactly("project1");
    assertThat(searchProjects("query = \"unknown\"").getComponentsList()).isEmpty();

    // Search by metric criteria and text query
    assertThat(searchProjects(new SearchProjectsRequest().setFilter("query = \"pAch\" AND ncloc > 50")).getComponentsList())
      .extracting(Component::getKey).containsExactly("project3");
    assertThat(searchProjects(new SearchProjectsRequest().setFilter("query = \"nd\" AND ncloc > 50")).getComponentsList())
      .extracting(Component::getKey).containsExactly("project3", "project4");
    assertThat(searchProjects(new SearchProjectsRequest().setFilter("query = \"unknown\" AND ncloc > 50")).getComponentsList()).isEmpty();

    // Check facets
    assertThat(searchProjects(new SearchProjectsRequest().setFilter("query = \"apache\"").setFacets(singletonList("ncloc"))).getFacets().getFacets(0).getValuesList())
      .extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
      .containsOnly(tuple("*-1000.0", 3L), tuple("1000.0-10000.0", 0L), tuple("10000.0-100000.0", 0L), tuple("100000.0-500000.0", 0L), tuple("500000.0-*", 0L));
    assertThat(searchProjects(new SearchProjectsRequest().setFilter("query = \"unknown\"").setFacets(singletonList("ncloc"))).getFacets().getFacets(0)
      .getValuesList()).extracting(Common.FacetValue::getVal, Common.FacetValue::getCount)
        .containsOnly(tuple("*-1000.0", 0L), tuple("1000.0-10000.0", 0L), tuple("10000.0-100000.0", 0L), tuple("100000.0-500000.0", 0L), tuple("500000.0-*", 0L));
  }

  @Test
  public void should_return_facets() throws Exception {
    analyzeProject(newProjectKey(), "shared/xoo-sample");
    analyzeProject(newProjectKey(), "shared/xoo-multi-modules-sample");

    SearchProjectsWsResponse response = searchProjects(new SearchProjectsRequest().setOrganization(organization.getKey()).setFacets(asList(
      "alert_status",
      "coverage",
      "duplicated_lines_density",
      "languages",
      "ncloc",
      "reliability_rating",
      "security_rating",
      "sqale_rating",
      "tags")));

    checkFacet(response, "alert_status",
      tuple("OK", 2L),
      tuple("WARN", 0L),
      tuple("ERROR", 0L));
    checkFacet(response, "coverage",
      tuple("NO_DATA", 2L),
      tuple("*-30.0", 0L),
      tuple("30.0-50.0", 0L),
      tuple("50.0-70.0", 0L),
      tuple("70.0-80.0", 0L),
      tuple("80.0-*", 0L));
    checkFacet(response, "duplicated_lines_density",
      tuple("NO_DATA", 0L),
      tuple("*-3.0", 2L),
      tuple("3.0-5.0", 0L),
      tuple("5.0-10.0", 0L),
      tuple("10.0-20.0", 0L),
      tuple("20.0-*", 0L));
    checkFacet(response, "languages",
      tuple("xoo", 2L));
    checkFacet(response, "ncloc",
      tuple("*-1000.0", 2L),
      tuple("1000.0-10000.0", 0L),
      tuple("10000.0-100000.0", 0L),
      tuple("100000.0-500000.0", 0L),
      tuple("500000.0-*", 0L));
    checkFacet(response, "reliability_rating",
      tuple("1", 2L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "security_rating",
      tuple("1", 2L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "sqale_rating",
      tuple("1", 0L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 2L),
      tuple("5", 0L));
    checkFacet(response, "tags");
  }

  @Test
  public void should_return_facets_on_leak() throws Exception {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");
    // This project has no duplication on new code
    String projectKey1 = newProjectKey();
    analyzeProject(projectKey1, "shared/xoo-sample", "sonar.projectDate", "2016-12-31");
    analyzeProject(projectKey1, "shared/xoo-sample");
    // This project has 0% duplication on new code
    String projectKey2 = newProjectKey();
    analyzeProject(projectKey2, "projectSearch/xoo-history-v1", "sonar.projectDate", "2016-12-31");
    analyzeProject(projectKey2, "projectSearch/xoo-history-v2");

    SearchProjectsWsResponse response = searchProjects(new SearchProjectsRequest().setOrganization(organization.getKey()).setFacets(asList(
      "new_reliability_rating", "new_security_rating", "new_maintainability_rating", "new_coverage", "new_duplicated_lines_density", "new_lines")));

    checkFacet(response, "new_reliability_rating",
      tuple("1", 2L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "new_security_rating",
      tuple("1", 2L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "new_maintainability_rating",
      tuple("1", 2L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "new_coverage",
      tuple("NO_DATA", 2L),
      tuple("*-30.0", 0L),
      tuple("30.0-50.0", 0L),
      tuple("50.0-70.0", 0L),
      tuple("70.0-80.0", 0L),
      tuple("80.0-*", 0L));
    checkFacet(response, "new_duplicated_lines_density",
      tuple("NO_DATA", 1L),
      tuple("*-3.0", 1L),
      tuple("3.0-5.0", 0L),
      tuple("5.0-10.0", 0L),
      tuple("10.0-20.0", 0L),
      tuple("20.0-*", 0L));
    checkFacet(response, "new_lines",
      tuple("*-1000.0", 2L),
      tuple("1000.0-10000.0", 0L),
      tuple("10000.0-100000.0", 0L),
      tuple("100000.0-500000.0", 0L),
      tuple("500000.0-*", 0L));
  }

  private void checkFacet(SearchProjectsWsResponse response, String facetKey, Tuple... values) {
    Common.Facet facet = response.getFacets().getFacetsList().stream().filter(f -> f.getProperty().equals(facetKey)).findAny().get();
    assertThat(facet.getValuesList()).extracting(Common.FacetValue::getVal, Common.FacetValue::getCount).containsExactlyInAnyOrder(values);
  }

  private void analyzeProject(String projectKey, String relativePath, String... properties) {
    List<String> keyValueProperties = new ArrayList<>(asList(
      "sonar.projectKey", projectKey,
      "sonar.organization", organization.getKey(),
      "sonar.profile", "with-many-rules",
      "sonar.login", "admin", "sonar.password", "admin",
      "sonar.scm.disabled", "false"));
    orchestrator.executeBuild(SonarScanner.create(projectDir(relativePath), concat(keyValueProperties.toArray(new String[0]), properties)));
  }

  private SearchProjectsWsResponse searchProjects(String filter) throws IOException {
    return searchProjects(new SearchProjectsRequest().setOrganization(organization.getKey()).setFilter(filter));
  }

  private SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) {
    return tester.wsClient().components().searchProjects(request);
  }

  private void verifyFilterMatches(String projectKey, String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsList()).extracting(Component::getKey).containsOnly(projectKey);
  }

  private void verifyFilterDoesNotMatch(String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsCount()).isZero();
  }
}
