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

import com.codeborne.selenide.Selenide;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectKeyPage;
import org.sonarqube.ws.client.PostRequest;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectKeyUpdatePageTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    tester.wsClient().users().skipOnboardingTutorial();
  }

  @Test
  public void change_key_when_no_modules() {
    createProject("sample");

    ProjectKeyPage page = openPage("sample");
    page.assertSimpleUpdate().trySimpleUpdate("another");

    Selenide.Wait().until(driver -> driver.getCurrentUrl().endsWith("/project/key?id=another"));
  }

  @Test
  public void fail_to_change_key_when_no_modules() {
    createProject("sample");
    createProject("another");

    ProjectKeyPage page = openPage("sample");
    page.assertSimpleUpdate().trySimpleUpdate("another");

    $(".process-spinner").shouldBe(visible);
    Selenide.Wait().until(driver -> driver.getCurrentUrl().endsWith("/project/key?id=sample"));
  }

  @Test
  public void change_key_of_multi_modules_project() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");

    ProjectKeyPage page = openPage("sample");
    page.openFineGrainedUpdate().tryFineGrainedUpdate("sample", "another");

    Selenide.Wait().until(driver -> driver.getCurrentUrl().endsWith("/project/key?id=another"));
  }

  @Test
  public void fail_to_change_key_of_multi_modules_project() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");
    createProject("another");

    ProjectKeyPage page = openPage("sample");
    page.openFineGrainedUpdate().tryFineGrainedUpdate("sample", "another");

    $(".process-spinner").shouldBe(visible);
    assertThat(url()).endsWith("/project/key?id=sample");
  }

  @Test
  public void change_key_of_module_of_multi_modules_project() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");

    ProjectKeyPage page = openPage("sample");
    page.openFineGrainedUpdate().tryFineGrainedUpdate("sample:module_a:module_a1", "another");

    $("#update-key-confirmation-form").shouldNotBe(visible);

    tester.openBrowser().openProjectKey("another");
    assertThat(url()).endsWith("/project/key?id=another");
  }

  @Test
  public void fail_to_change_key_of_module_of_multi_modules_project() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");
    createProject("another");

    ProjectKeyPage page = openPage("sample");
    page.openFineGrainedUpdate().tryFineGrainedUpdate("sample:module_a:module_a1", "another");

    $(".process-spinner").shouldBe(visible);
  }

  @Test
  public void bulk_change() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");

    ProjectKeyPage page = openPage("sample");
    page.assertBulkChange().simulateBulkChange("sample", "another");

    $("#bulk-update-results").shouldBe(visible);
    page.assertBulkChangeSimulationResult("sample", "another")
      .assertBulkChangeSimulationResult("sample:module_a:module_a1", "another:module_a:module_a1");

    page
      .confirmBulkUpdate()
      .assertSuccessfulBulkUpdate();
  }

  @Test
  public void fail_to_bulk_change_because_no_changed_key() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");

    ProjectKeyPage page = openPage("sample");
    page.assertBulkChange().simulateBulkChange("random", "another");

    $("#bulk-update-nothing").shouldBe(visible);
    $("#bulk-update-results").shouldNotBe(visible);
  }

  @Test
  public void fail_to_bulk_change_because_of_duplications() {
    analyzeProject("shared/xoo-multi-modules-sample", "sample");

    ProjectKeyPage page = openPage("sample");
    page.assertBulkChange().simulateBulkChange("module_a1", "module_a2");

    $("#bulk-update-duplicate").shouldBe(visible);
    $("#bulk-update-results").shouldBe(visible);

    page.assertBulkChangeSimulationResult("sample:module_a:module_a1", "sample:module_a:module_a2")
      .assertDuplicated("sample:module_a:module_a1");
  }

  private ProjectKeyPage openPage(String projectKey) {
    return tester.openBrowser()
      .logIn()
      .submitCredentials("admin", "admin")
      .openProjectKey(projectKey);
  }

  private void createProject(String projectKey) {
    tester.wsClient().wsConnector().call(new PostRequest("api/projects/create")
      .setParam("key", projectKey)
      .setParam("name", projectKey));
  }

  private void analyzeProject(String path, String projectKey) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path))
      .setProjectKey(projectKey));
  }
}
