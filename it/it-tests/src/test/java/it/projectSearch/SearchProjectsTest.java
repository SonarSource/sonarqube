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
import it.Category4Suite;
import java.io.IOException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Common.FacetValue;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static com.sonar.orchestrator.build.SonarScanner.create;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

/**
 * Tests WS api/components/search_projects
 */
public class SearchProjectsTest {

  private static final String PROJECT_KEY = "sample";
  private static final String PROJECT_NAME = "Sample";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void setUp() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void filter_projects_by_measure_values() throws Exception {
    orchestrator.executeBuild(create(projectDir("shared/xoo-sample")));

    verifyFilterMatches("ncloc > 1");
    verifyFilterMatches("ncloc > 1 and comment_lines < 10000");
    verifyFilterDoesNotMatch("ncloc <= 1");
  }

  @Test
  public void provisioned_projects_should_be_included_to_results() throws Exception {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);

    SearchProjectsWsResponse response = searchProjects(SearchProjectsRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY);
  }

  @Test
  public void filter_by_text_query() throws IOException {
    orchestrator.executeBuild(create(projectDir("shared/xoo-sample"), "sonar.projectKey", "project1", "sonar.projectName", "apachee"));
    orchestrator.executeBuild(create(projectDir("shared/xoo-sample"), "sonar.projectKey", "project2", "sonar.projectName", "Apache"));
    orchestrator.executeBuild(create(projectDir("shared/xoo-multi-modules-sample"), "sonar.projectKey", "project3", "sonar.projectName", "Apache Foundation"));
    orchestrator.executeBuild(create(projectDir("shared/xoo-multi-modules-sample"), "sonar.projectKey", "project4", "sonar.projectName", "Windows"));

    // Search only by text query
    assertThat(searchProjects("query = \"apache\"").getComponentsList()).extracting(Component::getKey).containsExactly("project2", "project3", "project1");
    assertThat(searchProjects("query = \"pAch\"").getComponentsList()).extracting(Component::getKey).containsExactly("project2", "project3", "project1");
    assertThat(searchProjects("query = \"hee\"").getComponentsList()).extracting(Component::getKey).containsExactly("project1");
    assertThat(searchProjects("query = \"project1\"").getComponentsList()).extracting(Component::getKey).containsExactly("project1");
    assertThat(searchProjects("query = \"unknown\"").getComponentsList()).isEmpty();

    // Search by metric criteria and text query
    assertThat(searchProjects(SearchProjectsRequest.builder().setFilter("query = \"pAch\" AND ncloc > 50").build()).getComponentsList())
      .extracting(Component::getKey).containsExactly("project3");
    assertThat(searchProjects(SearchProjectsRequest.builder().setFilter("query = \"nd\" AND ncloc > 50").build()).getComponentsList())
      .extracting(Component::getKey).containsExactly("project3", "project4");
    assertThat(searchProjects(SearchProjectsRequest.builder().setFilter("query = \"unknown\" AND ncloc > 50").build()).getComponentsList()).isEmpty();;

    // Check facets
    assertThat(searchProjects(SearchProjectsRequest.builder().setFilter("query = \"apache\"").setFacets(singletonList("ncloc")).build()).getFacets().getFacets(0).getValuesList())
      .extracting(FacetValue::getVal, FacetValue::getCount)
      .containsOnly(tuple("*-1000.0", 3L), tuple("1000.0-10000.0", 0L), tuple("10000.0-100000.0", 0L), tuple("100000.0-500000.0", 0L), tuple("500000.0-*", 0L));
    assertThat(searchProjects(SearchProjectsRequest.builder().setFilter("query = \"unknown\"").setFacets(singletonList("ncloc")).build()).getFacets().getFacets(0)
      .getValuesList()).extracting(FacetValue::getVal, FacetValue::getCount)
      .containsOnly(tuple("*-1000.0", 0L), tuple("1000.0-10000.0", 0L), tuple("10000.0-100000.0", 0L), tuple("100000.0-500000.0", 0L), tuple("500000.0-*", 0L));
  }

  private SearchProjectsWsResponse searchProjects(String filter) throws IOException {
    return searchProjects(SearchProjectsRequest.builder().setFilter(filter).build());
  }

  private SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) throws IOException {
    return newAdminWsClient(orchestrator).components().searchProjects(request);
  }

  private void verifyFilterMatches(String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY);
  }

  private void verifyFilterDoesNotMatch(String filter) throws IOException {
    assertThat(searchProjects(filter).getComponentsCount()).isZero();
  }
}
