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
import java.io.File;
import java.io.IOException;
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
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Projects.BulkUpdateKeyWsResponse.Key;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.components.TreeRequest;
import org.sonarqube.ws.client.projects.BulkUpdateKeyRequest;
import org.sonarqube.ws.client.projects.UpdateKeyRequest;
import util.ItUtils;
import util.XooProjectBuilder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.projectDir;

public class ProjectKeyUpdateTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static final Orchestrator orchestrator = OrganizationProjectSuite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public Tester tester = new Tester(orchestrator).setElasticsearchHttpPort(OrganizationProjectSuite.SEARCH_HTTP_PORT);

  @After
  public void tearDown() throws Exception {
    unlockWritesOnProjectIndices();
  }

  @Test
  public void update_key() throws IOException {
    Organizations.Organization organization = tester.organizations().generate();
    File projectDir = new XooProjectBuilder(PROJECT_KEY)
      .build(temp.newFolder());
    analyze(organization, projectDir);
    assertThat(tester.projects().exists(PROJECT_KEY)).isTrue();

    String newProjectKey = "renamed";
    updateProjectKey(PROJECT_KEY, newProjectKey, false);

    assertThat(tester.projects().exists(PROJECT_KEY)).isFalse();
    assertThat(tester.projects().exists(newProjectKey)).isTrue();
  }

  @Test
  public void update_key_of_provisioned_project() {
    Organizations.Organization organization = tester.organizations().generate();
    Projects.CreateWsResponse.Project project = createProject(organization, "one", "Foo");

    updateProjectKey(project.getKey(), "two", false);

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

    updateProjectKey(project.getKey(), "two", false);

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

    updateModuleKey(initialKey, newKey);

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
    updateModuleKey(initialKey, newKey);

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

  /**
   * SONAR-10511
   */
  @Test
  public void update_key_of_disabled_files() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();

    // first analysis
    File projectWith2Files = new XooProjectBuilder(PROJECT_KEY)
      .setFilesPerModule(2)
      .build(temp.newFolder());
    analyze(organization, projectWith2Files);
    assertThat(countFilesInProject()).isEqualTo(2);

    // second analysis emulates a deletion of file
    File projectWith1File = new XooProjectBuilder(PROJECT_KEY)
      .setFilesPerModule(1)
      .build(temp.newFolder());
    analyze(organization, projectWith1File);
    assertThat(countFilesInProject()).isEqualTo(1);

    // update the project key
    updateProjectKey(PROJECT_KEY, "renamed", false);
    ItUtils.expectNotFoundError(() -> tester.wsClient().components().show(new ShowRequest().setComponent(PROJECT_KEY)));

    // first analysis of the new project, which re-enables the deleted file
    analyze(organization, projectWith2Files);
    assertThat(countFilesInProject()).isEqualTo(2);
  }

  /**
   * SONAR-10511
   */
  @Test
  public void update_of_project_key_includes_disabled_modules() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();

    // first analysis
    File projectWithModulesAB = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_a", "module_b")
      .build(temp.newFolder());
    analyze(organization, projectWithModulesAB);
    assertThat(countFilesInProject()).isEqualTo(3);

    // second analysis emulates deletion of module_b
    File projectWithModuleA = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_a")
      .build(temp.newFolder());
    analyze(organization, projectWithModuleA);
    assertThat(countFilesInProject()).isEqualTo(2);

    // update the project key
    updateProjectKey(PROJECT_KEY, "renamed", false);
    assertThat(tester.projects().exists(PROJECT_KEY)).isFalse();

    // analysis of new project, re-enabling the deleted module
    File projectWithModulesBC = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_b", "module_c")
      .build(temp.newFolder());
    analyze(organization, projectWithModulesBC);
    assertThat(countFilesInProject()).isEqualTo(3);
  }

  @Test
  public void simulate_update_key_of_modules() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();

    File project = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_a", "module_b")
      .build(temp.newFolder());
    analyze(organization, project);
    assertThat(tester.projects().exists(PROJECT_KEY)).isTrue();

    // simulate update of project key
    Projects.BulkUpdateKeyWsResponse response = updateProjectKey(PROJECT_KEY, "renamed", true);

    assertThat(tester.projects().exists(PROJECT_KEY)).isTrue();
    assertThat(tester.projects().exists("renamed")).isFalse();
    assertThat(response.getKeysList())
      .extracting(Key::getKey, Key::getNewKey)
      .containsExactlyInAnyOrder(
        tuple(PROJECT_KEY, "renamed"),
        tuple(PROJECT_KEY + ":module_a", "renamed:module_a"),
        tuple(PROJECT_KEY + ":module_b", "renamed:module_b"));
  }

  @Test
  public void simulate_update_key_of_disabled_modules() throws Exception {
    Organizations.Organization organization = tester.organizations().generate();

    // first analysis
    File projectWithModulesAB = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_a", "module_b")
      .build(temp.newFolder());
    analyze(organization, projectWithModulesAB);
    assertThat(countFilesInProject()).isEqualTo(3);

    // second analysis emulates deletion of module_b
    File projectWithModuleA = new XooProjectBuilder(PROJECT_KEY)
      .addModules("module_a")
      .build(temp.newFolder());
    analyze(organization, projectWithModuleA);
    assertThat(countFilesInProject()).isEqualTo(2);

    // update the project key
    Projects.BulkUpdateKeyWsResponse response = updateProjectKey(PROJECT_KEY, "renamed", true);

    assertThat(tester.projects().exists(PROJECT_KEY)).isTrue();
    assertThat(tester.projects().exists("renamed")).isFalse();
    assertThat(response.getKeysList())
      .extracting(Key::getKey, Key::getNewKey)
      .containsExactlyInAnyOrder(
        tuple(PROJECT_KEY, "renamed"),
        tuple(PROJECT_KEY + ":module_a", "renamed:module_a"));
  }

  private int countFilesInProject() {
    TreeRequest request = new TreeRequest().setComponent(PROJECT_KEY).setQualifiers(asList("FIL"));
    return tester.wsClient().components().tree(request).getComponentsCount();
  }

  private void analyze(Organizations.Organization organization, File projectDir) {
    orchestrator.executeBuild(SonarScanner.create(projectDir,
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin", "sonar.password", "admin"));
  }

  private void lockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().lockWrites("components");
    tester.elasticsearch().lockWrites("projectmeasures");
  }

  private void unlockWritesOnProjectIndices() throws Exception {
    tester.elasticsearch().unlockWrites("components");
    tester.elasticsearch().unlockWrites("projectmeasures");
  }

  private void updateModuleKey(String initialKey, String newKey) {
    tester.wsClient().projects().updateKey(new UpdateKeyRequest().setFrom(initialKey).setTo(newKey));
  }

  private Projects.BulkUpdateKeyWsResponse updateProjectKey(String initialKey, String newKey, boolean dryRun) {
    return tester.wsClient().projects().bulkUpdateKey(new BulkUpdateKeyRequest()
      .setProject(initialKey)
      .setFrom(initialKey)
      .setTo(newKey)
      .setDryRun(String.valueOf(dryRun)));
  }

  private Projects.CreateWsResponse.Project createProject(Organizations.Organization organization, String key, String name) {
    return tester.projects().provision(organization, r -> r.setProject(key).setName(name));
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

}
