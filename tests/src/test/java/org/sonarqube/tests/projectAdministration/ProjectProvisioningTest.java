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
package org.sonarqube.tests.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import java.util.Collection;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsProjects;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.project.SearchWsRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectProvisioningTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));
  @Rule
  public Tester tester = new Tester(orchestrator)
    .setElasticsearchHttpPort(Category6Suite.SEARCH_HTTP_PORT);

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
    CreateRequest createRequest = CreateRequest.builder().setKey(key).setName(name).setOrganization(organization.getKey()).build();
    return tester.wsClient().projects().create(createRequest).getProject();
  }

  /**
   * Projects administration page - uses database
   */
  private boolean isInProjectsSearch(Organizations.Organization organization, String name) {
    WsProjects.SearchWsResponse response = tester.wsClient().projects().search(
      SearchWsRequest.builder().setOrganization(organization.getKey()).setQuery(name).setQualifiers(singletonList("TRK")).build());
    return response.getComponentsCount() > 0;
  }

  /**
   * Projects page - api/components/search_projects - uses ES + DB
   */
  private boolean isInComponentSearchProjects(String name) {
    WsComponents.SearchProjectsWsResponse response = tester.wsClient().components().searchProjects(
      SearchProjectsRequest.builder().setFilter("query=\"" + name + "\"").build());
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
