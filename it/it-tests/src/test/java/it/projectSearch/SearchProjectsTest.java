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
package it.projectSearch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import java.io.IOException;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import util.ItUtils;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.setServerProperty;

/**
 * Tests WS api/components/search_projects
 */
public class SearchProjectsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  private String organizationKey;

  @BeforeClass
  public static void beforeClass() throws Exception {
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    organizationKey = ItUtils.newOrganizationKey();
    newAdminWsClient(orchestrator).organizations().create(CreateWsRequest.builder()
      .setKey(organizationKey)
      .setName(organizationKey)
      .build());
    restoreProfile(orchestrator, SearchProjectsTest.class.getResource("/projectSearch/SearchProjectsTest/with-many-rules.xml"), organizationKey);
  }

  @Test
  public void should_return_facets() throws Exception {
    analyzeProject(newProjectKey(), "shared/xoo-sample");
    analyzeProject(newProjectKey(), "shared/xoo-multi-modules-sample");

    SearchProjectsWsResponse response = searchProjects(SearchProjectsRequest.builder().setOrganization(organizationKey).setFacets(asList(
      "alert_status",
      "coverage",
      "duplicated_lines_density",
      "languages",
      "ncloc",
      "reliability_rating",
      "security_rating",
      "sqale_rating",
      "tags")).build());

    checkFacet(response, "alert_status",
      tuple("OK", 2L),
      tuple("WARN", 0L),
      tuple("ERROR", 0L));
    checkFacet(response, "coverage",
      tuple("*-30.0", 0L),
      tuple("30.0-50.0", 0L),
      tuple("50.0-70.0", 0L),
      tuple("70.0-80.0", 0L),
      tuple("80.0-*", 0L));
    checkFacet(response, "duplicated_lines_density",
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
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
    String projectKey = newProjectKey();
    analyzeProject(projectKey, "shared/xoo-history-v1");
    analyzeProject(projectKey, "shared/xoo-history-v2");

    SearchProjectsWsResponse response = searchProjects(SearchProjectsRequest.builder().setOrganization(organizationKey).setFacets(asList(
      "new_reliability_rating", "new_security_rating")).build());

    checkFacet(response, "new_reliability_rating",
      tuple("1", 1L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
    checkFacet(response, "new_security_rating",
      tuple("1", 1L),
      tuple("2", 0L),
      tuple("3", 0L),
      tuple("4", 0L),
      tuple("5", 0L));
  }

  private void checkFacet(SearchProjectsWsResponse response, String facetKey, Tuple... values) {
    Common.Facet facet = response.getFacets().getFacetsList().stream().filter(f -> f.getProperty().equals(facetKey)).findAny().get();
    assertThat(facet.getValuesList()).extracting(Common.FacetValue::getVal, Common.FacetValue::getCount).containsExactly(values);
  }

  @Test
  public void filter_projects_by_measure_values() throws Exception {
    String projectKey = newProjectKey();
    analyzeProject(projectKey,"shared/xoo-sample");

    verifyFilterMatches(projectKey, "ncloc > 1");
    verifyFilterMatches(projectKey, "ncloc > 1 and comment_lines < 10000");
    verifyFilterDoesNotMatch("ncloc <= 1");
  }

  @Test
  public void provisioned_projects_should_be_included_to_results() throws Exception {
    String projectKey = newProjectKey();
    newAdminWsClient(orchestrator).projects().create(CreateRequest.builder().setKey(projectKey).setName(projectKey).setOrganization(organizationKey).build());

    SearchProjectsWsResponse response = searchProjects(SearchProjectsRequest.builder().setOrganization(organizationKey).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).contains(projectKey);
  }

  private void analyzeProject(String projectKey, String relativePath) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(relativePath),
      "sonar.projectKey", projectKey,
      "sonar.organization", organizationKey,
      "sonar.profile", "with-many-rules",
      "sonar.login", "admin", "sonar.password", "admin"));
  }

  private SearchProjectsWsResponse searchProjects(String filter) throws IOException {
    return searchProjects(SearchProjectsRequest.builder().setOrganization(organizationKey).setFilter(filter).build());
  }

  private SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) throws IOException {
    return newAdminWsClient(orchestrator).components().searchProjects(request);
  }

  private void verifyFilterMatches(String projectKey, String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsList()).extracting(Component::getKey).containsOnly(projectKey);
  }

  private void verifyFilterDoesNotMatch(String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsCount()).isZero();
  }
}
