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
import java.util.Collection;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectProvisioningTest {

  @ClassRule
  public static final Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));

  @Rule
  public Tester tester = new Tester(orchestrator).setElasticsearchHttpPort(ProjectSuite.SEARCH_HTTP_PORT);

  @Test
  public void provisioned_project_is_available_in_search_engines() {
    Organizations.Organization organization = tester.organizations().generate();

    createProject(organization, "one", "Foo");

    assertThat(isInProjectsSearch(organization, "Foo")).isTrue();
    assertThat(isInComponentSearchProjects("Foo")).isTrue();
    assertThat(isInComponentSuggestions("Foo")).isTrue();
  }

  @Test
  public void indexing_errors_are_recovered_asynchronously_when_provisioning_project() throws Exception {
    tester.elasticsearch().lockWrites("components");
    tester.elasticsearch().lockWrites("projectmeasures");

    Organizations.Organization organization = tester.organizations().generate();
    Project project = createProject(organization, "one", "Foo");

    // no ES requests but only DB
    assertThat(isInProjectsSearch(organization, project.getName())).isTrue();

    // these WS use ES so they are temporarily inconsistent
    assertThat(isInComponentSearchProjects(project.getName())).isFalse();
    assertThat(isInComponentSuggestions(project.getName())).isFalse();

    tester.elasticsearch().unlockWrites("components");
    tester.elasticsearch().unlockWrites("projectmeasures");

    boolean found = false;
    while (!found) {
      // recovery daemon runs every second (see Category6Suite)
      Thread.sleep(1_000L);
      found = isInComponentSearchProjects(project.getName()) && isInComponentSuggestions(project.getName());
    }
  }

  private Project createProject(Organizations.Organization organization, String key, String name) {
    CreateRequest createRequest = new CreateRequest().setProject(key).setName(name).setOrganization(organization.getKey());
    return tester.wsClient().projects().create(createRequest).getProject();
  }

  /**
   * Projects administration page - uses database
   */
  private boolean isInProjectsSearch(Organizations.Organization organization, String name) {
    Projects.SearchWsResponse response = tester.wsClient().projects().search(
      new SearchRequest().setOrganization(organization.getKey()).setQ(name).setQualifiers(singletonList("TRK")));
    return response.getComponentsCount() > 0;
  }

  /**
   * Projects page - api/components/search_projects - uses ES + DB
   */
  private boolean isInComponentSearchProjects(String name) {
    Components.SearchProjectsWsResponse response = tester.wsClient().components().searchProjects(
      new SearchProjectsRequest().setFilter("query=\"" + name + "\""));
    return response.getComponentsCount() > 0;
  }

  /**
   * Top-right search engine - api/components/suggestions - uses ES + DB
   */
  private boolean isInComponentSuggestions(String name) {
    GetRequest request = new GetRequest("api/components/suggestions").setParam("s", name);
    WsResponse response = tester.wsClient().wsConnector().call(request);
    Map<String, Object> json = ItUtils.jsonToMap(response.content());
    Collection<Map<String, Object>> results = (Collection<Map<String, Object>>) json.get("results");
    Collection items = results.stream()
      .filter(map -> "TRK".equals(map.get("q")))
      .map(map -> (Collection) map.get("items"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("missing field results/[q=TRK]"));
    return !items.isEmpty();
  }
}
