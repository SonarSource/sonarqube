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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class SearchProjectsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void setUp() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void search_projects_with_filter_having_one_criterion() throws Exception {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    SearchProjectsWsResponse response = searchProjects("ncloc > 1");

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("sample");
  }

  @Test
  public void return_project_even_without_analysis() throws Exception {
    orchestrator.getServer().provisionProject("sample", "sample");

    SearchProjectsWsResponse response = searchProjects(SearchProjectsRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("sample");
  }

  private SearchProjectsWsResponse searchProjects(String filter) throws IOException {
    return searchProjects(SearchProjectsRequest.builder().setFilter(filter).build());
  }

  private SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) throws IOException {
    return newAdminWsClient(orchestrator).components().searchProjects(request);
  }
}
