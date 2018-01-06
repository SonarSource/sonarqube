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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.junit.After;
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
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.projects.UpdateKeyRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectKeyUpdateTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static final Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));

  @Rule
  public Tester tester = new Tester(orchestrator).setElasticsearchHttpPort(ProjectSuite.SEARCH_HTTP_PORT);

  @After
  public void tearDown() throws Exception {
    unlockWritesOnProjectIndices();
  }

  @Test
  public void update_key() {
    analyzeXooSample();
    String newProjectKey = "another_project_key";
    Components.Component project = tester.wsClient().components().show(new ShowRequest().setComponent(PROJECT_KEY)).getComponent();
    assertThat(project.getKey()).isEqualTo(PROJECT_KEY);

    tester.wsClient().projects().updateKey(new UpdateKeyRequest()
      .setFrom(PROJECT_KEY)
      .setTo(newProjectKey));

    assertThat(tester.wsClient().components().show(new ShowRequest().setComponentId(project.getId())).getComponent().getKey()).isEqualTo(newProjectKey);
  }

  @Test
  public void update_key_of_provisioned_project() {
    Organizations.Organization organization = tester.organizations().generate();
    Projects.CreateWsResponse.Project project = createProject(organization, "one", "Foo");

    updateKey(project, "two");

    assertThat(isProjectInDatabase("one")).isFalse();
    assertThat(isProjectInDatabase("two")).isTrue();
    assertThat(isComponentInDatabase("one")).isFalse();
    assertThat(isComponentInDatabase("two")).isTrue();
    assertThat(keyInComponentSearchProjects("Foo")).isEqualTo("two");
    assertThat(keysInComponentSuggestions("Foo")).containsExactly("two");
  }

  @Test
  public void recover_indexing_errors_when_updating_key_of_provisioned_project() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    Projects.CreateWsResponse.Project project = createProject(organization, "one", "Foo");

    lockWritesOnProjectIndices();

    updateKey(project, "two");

    assertThat(isProjectInDatabase("one")).isFalse();

    // WS gets the list of projects from ES then reloads projects from db.
    // That's why keys in WS responses are correct.
    assertThat(isProjectInDatabase("one")).isFalse();
    assertThat(isProjectInDatabase("two")).isTrue();
    assertThat(keyInComponentSearchProjects("Foo")).isEqualTo("two");
    assertThat(keysInComponentSuggestions("Foo")).containsExactly("two");

    // however searching by key is inconsistent
    assertThat(keyInComponentSearchProjects("one")).isEqualTo("two");
    assertThat(keysInComponentSuggestions("one")).containsExactly("two");
    assertThat(keyInComponentSearchProjects("two")).isNull();
    assertThat(keysInComponentSuggestions("two")).isEmpty();

    unlockWritesOnProjectIndices();

    boolean recovered = false;
    while (!recovered) {
      // recovery daemon runs every second, see Category6Suite
      Thread.sleep(1_000L);
      recovered = keyInComponentSearchProjects("one") == null &&
        keysInComponentSuggestions("one").isEmpty() &&
        "two".equals(keyInComponentSearchProjects("two")) &&
        keysInComponentSuggestions("two").contains("two");
    }
  }

  @Test
  public void update_key_of_module() {
    Organizations.Organization organization = tester.organizations().generate();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin", "sonar.password", "admin"));

    String initialKey = "com.sonarsource.it.samples:multi-modules-sample:module_a";
    String newKey = "com.sonarsource.it.samples:multi-modules-sample:module_c";

    updateKey(initialKey, newKey);

    assertThat(isComponentInDatabase(initialKey)).isFalse();
    assertThat(isComponentInDatabase(newKey)).isTrue();
    // suggestions engine ignores one-character words, so we can't search for "Module A"
    assertThat(keysInComponentSuggestions("Module"))
      .contains(newKey)
      .doesNotContain(initialKey);
    assertThat(keysInComponentSuggestions(newKey))
      .contains(newKey)
      .doesNotContain(initialKey);
    assertThat(keysInComponentSuggestions(initialKey)).isEmpty();

  }

  @Test
  public void recover_indexing_errors_when_updating_key_of_module() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin", "sonar.password", "admin"));

    String initialKey = "com.sonarsource.it.samples:multi-modules-sample:module_a";
    String newKey = "com.sonarsource.it.samples:multi-modules-sample:module_c";

    lockWritesOnProjectIndices();
    updateKey(initialKey, newKey);

    // api/components/search loads keys from db, so results are consistent
    assertThat(isComponentInDatabase(initialKey)).isFalse();
    assertThat(isComponentInDatabase(newKey)).isTrue();

    // key in result of suggestion engine is loaded from db, so results are ok when searching for unchanged name
    assertThat(keysInComponentSuggestions("Module"))
      .contains(newKey)
      .doesNotContain(initialKey);

    // but searching for new key does not work
    assertThat(keysInComponentSuggestions(newKey)).isEmpty();
    assertThat(keysInComponentSuggestions(initialKey))
      .isNotEmpty()
      .contains(newKey /* the returned key is loaded from db, so it's correct */);

    unlockWritesOnProjectIndices();

    boolean recovered = false;
    while (!recovered) {
      // recovery daemon runs every second, see Category6Suite
      Thread.sleep(1_000L);
      recovered = keysInComponentSuggestions(newKey).contains(newKey) && keysInComponentSuggestions(initialKey).isEmpty();
    }

  }

  private void lockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().lockWrites("components");
    tester.elasticsearch().lockWrites("projectmeasures");
  }

  private void unlockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().unlockWrites("components");
    tester.elasticsearch().unlockWrites("projectmeasures");
  }

  private void updateKey(Projects.CreateWsResponse.Project project, String newKey) {
    tester.wsClient().projects().updateKey(new UpdateKeyRequest().setFrom(project.getKey()).setTo(newKey));
  }

  private void updateKey(String initialKey, String newKey) {
    tester.wsClient().projects().updateKey(new UpdateKeyRequest().setFrom(initialKey).setTo(newKey));
  }

  private Projects.CreateWsResponse.Project createProject(Organizations.Organization organization, String key, String name) {
    CreateRequest createRequest = new CreateRequest().setProject(key).setName(name).setOrganization(organization.getKey());
    return tester.wsClient().projects().create(createRequest).getProject();
  }

  private boolean isProjectInDatabase(String projectKey) {
    return orchestrator.getDatabase().countSql(String.format("select count(id) from projects where qualifier='TRK' and kee='%s'", projectKey)) == 1L;
  }

  private boolean isComponentInDatabase(String componentKey) {
    return orchestrator.getDatabase().countSql(String.format("select count(id) from projects where kee='%s'", componentKey)) == 1L;
  }

  /**
   * Projects page - api/components/search_projects - uses ES + DB
   */
  @CheckForNull
  private String keyInComponentSearchProjects(String name) {
    Components.SearchProjectsWsResponse response = tester.wsClient().components().searchProjects(
      new SearchProjectsRequest().setFilter("query=\"" + name + "\""));
    if (response.getComponentsCount() > 0) {
      return response.getComponents(0).getKey();
    }
    return null;
  }

  /**
   * Top-right search engine - api/components/suggestions - uses ES + DB
   */
  private List<String> keysInComponentSuggestions(String name) {
    GetRequest request = new GetRequest("api/components/suggestions").setParam("s", name);
    WsResponse response = tester.wsClient().wsConnector().call(request);
    Map<String, Object> json = ItUtils.jsonToMap(response.content());
    Collection<Map<String, Object>> results = (Collection<Map<String, Object>>) json.get("results");
    return results.stream()
      .filter(map -> "TRK".equals(map.get("q")) || "BRC".equals(map.get("q")))
      .flatMap(map -> ((Collection<Map<String, Object>>) map.get("items")).stream())
      .map(map -> (String) map.get("key"))
      .collect(Collectors.toList());
  }

  private void analyzeXooSample() {
    SonarScanner build = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
  }
}
