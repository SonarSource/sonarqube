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
import it.Category4Suite;
import java.io.IOException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static org.assertj.core.api.Assertions.assertThat;
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
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

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
