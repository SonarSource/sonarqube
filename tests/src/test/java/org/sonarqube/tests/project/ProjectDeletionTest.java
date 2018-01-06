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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.projects.BulkDeleteRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;
import org.sonarqube.ws.client.projects.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getComponent;
import static util.ItUtils.projectDir;

public class ProjectDeletionTest {

  @ClassRule
  public static final Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));

  @Rule
  public Tester tester = new Tester(orchestrator).setElasticsearchHttpPort(ProjectSuite.SEARCH_HTTP_PORT);

  @Test
  public void delete_project_by_web_service() {
    String projectKey = "sample";
    String fileKey = "sample:src/main/xoo/sample/Sample.xoo";

    analyzeXooSample();
    assertThat(getComponent(orchestrator, projectKey)).isNotNull();
    assertThat(getComponent(orchestrator, fileKey)).isNotNull();

    // fail to delete a file
    ItUtils.expectBadRequestError(() -> executeDeleteRequest(tester.wsClient(), fileKey));

    // fail if anonymous
    ItUtils.expectUnauthorizedError(() -> executeDeleteRequest(tester.asAnonymous().wsClient(), projectKey));

    // fail if insufficient privilege
    Users.CreateWsResponse.User user = tester.users().generate();
    ItUtils.expectForbiddenError(() -> executeDeleteRequest(tester.as(user.getLogin()).wsClient(), projectKey));

    // succeed to delete if administrator
    executeDeleteRequest(tester.wsClient(), projectKey);
    assertThat(getComponent(orchestrator, "sample")).isNull();
    assertThat(getComponent(orchestrator, "sample:src/main/xoo/sample/Sample.xoo")).isNull();
  }

  @Test
  public void deletion_removes_project_from_search_engines() {
    Organizations.Organization organization = tester.organizations().generate();
    Project project1 = createProject(organization, "one", "Foo");
    Project project2 = createProject(organization, "two", "Bar");
    assertThatProjectIsSearchable(organization, "Foo");
    assertThatProjectIsSearchable(organization, "Bar");

    deleteProject(project1);
    assertThatProjectIsNotSearchable(organization, project1.getName());
    assertThatProjectIsSearchable(organization, project2.getName());

    deleteProject(project2);
    assertThatProjectIsNotSearchable(organization, project1.getName());
    assertThatProjectIsNotSearchable(organization, project2.getName());
  }

  @Test
  public void indexing_errors_are_recovered_asynchronously_when_deleting_project() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    Project project = createProject(organization, "one", "Foo");

    tester.elasticsearch().lockWrites("components");
    tester.elasticsearch().lockWrites("projectmeasures");
    deleteProject(project);
    // WS reloads from database the results returned by Elasticsearch. That's
    // why the project does not appear in search engine.
    // However this test is still useful to verify that WS do not
    // fail during this Elasticsearch inconsistency.
    assertThatProjectIsNotSearchable(organization, project.getName());

    tester.elasticsearch().unlockWrites("components");
    tester.elasticsearch().unlockWrites("projectmeasures");
    // TODO verify that recovery daemon successfully updated indices
  }

  @Test
  public void bulk_deletion_removes_projects_from_search_engines() {
    Organizations.Organization organization = tester.organizations().generate();
    Project project1 = createProject(organization, "one", "Foo");
    Project project2 = createProject(organization, "two", "Bar");
    Project project3 = createProject(organization, "three", "Baz");

    bulkDeleteProjects(organization, project1, project3);
    assertThatProjectIsNotSearchable(organization, project1.getName());
    assertThatProjectIsSearchable(organization, project2.getName());
    assertThatProjectIsNotSearchable(organization, project3.getName());
  }

  @Test
  public void indexing_errors_are_recovered_asynchronously_when_bulk_deleting_projects() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    Project project1 = createProject(organization, "one", "Foo");
    Project project2 = createProject(organization, "two", "Bar");
    Project project3 = createProject(organization, "three", "Baz");

    tester.elasticsearch().lockWrites("components");
    tester.elasticsearch().lockWrites("projectmeasures");
    bulkDeleteProjects(organization, project1, project3);

    // WS reloads from database the results returned by Elasticsearch. That's
    // why the project does not appear in search engine.
    // However this test is still useful to verify that WS do not
    // fail during this Elasticsearch inconsistency.
    assertThatProjectIsNotSearchable(organization, project1.getName());
    assertThatProjectIsSearchable(organization, project2.getName());
    assertThatProjectIsNotSearchable(organization, project3.getName());

    tester.elasticsearch().unlockWrites("components");
    tester.elasticsearch().unlockWrites("projectmeasures");
    // TODO verify that recovery daemon successfully updated indices
  }

  private void deleteProject(Project project) {
    tester.wsClient().projects().delete(new DeleteRequest().setProject(project.getKey()));
  }

  private void bulkDeleteProjects(Organizations.Organization organization, Project... projects) {
    BulkDeleteRequest request = new BulkDeleteRequest()
      .setOrganization(organization.getKey())
      .setProjects(Arrays.stream(projects).map(Project::getKey).collect(Collectors.toList()));
    tester.wsClient().projects().bulkDelete(request);
  }

  private Project createProject(Organizations.Organization organization, String key, String name) {
    CreateRequest createRequest = new CreateRequest().setProject(key).setName(name).setOrganization(organization.getKey());
    return tester.wsClient().projects().create(createRequest).getProject();
  }

  private void assertThatProjectIsSearchable(Organizations.Organization organization, String name) {
    assertThat(isInProjectsSearch(organization, name)).isTrue();
    assertThat(isInComponentSearchProjects(name)).isTrue();
    assertThat(isInComponentSuggestions(name)).isTrue();
  }

  private void assertThatProjectIsNotSearchable(Organizations.Organization organization, String name) {
    assertThat(isInProjectsSearch(organization, name)).isFalse();
    assertThat(isInComponentSearchProjects(name)).isFalse();
    assertThat(isInComponentSuggestions(name)).isFalse();
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

  private void analyzeXooSample() {
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
  }

  private static void executeDeleteRequest(WsClient wsClient, String key) {
    wsClient.projects().delete(new DeleteRequest().setProject(key));
  }
}
