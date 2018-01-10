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
package org.sonarqube.tests.measure;

import com.codeborne.selenide.Selenide;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectDashboardPage;
import org.sonarqube.ws.client.PostRequest;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class ProjectDashboardTest {

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private String adminUser;

  @Before
  public void setUp() {
    adminUser = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
  }

  @Test
  public void after_first_analysis() {
    executeBuild("shared/xoo-sample", "project-for-overview", "Project For Overview");

    runSelenese(orchestrator, "/measure/ProjectDashboardTest/test_project_overview_after_first_analysis.html");
  }

  @Test
  public void display_size() {
    executeBuild("shared/xoo-sample", "sample", "Sample");

    ProjectDashboardPage page = tester.openBrowser().openProjectDashboard("sample");

    page.getLinesOfCode().should(text("13"));
    page.getLanguageDistribution().should(text("Xoo"), text("13"));
  }

  @Test
  public void display_tags_without_edit() {
    executeBuild("shared/xoo-sample", "sample", "Sample");

    // Add some tags to the project
    tester.wsClient().wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", "sample")
        .setParam("tags", "foo,bar,baz"));

    ProjectDashboardPage page = tester.openBrowser().openProjectDashboard("sample");
    page
      .shouldHaveTags("foo", "bar", "baz")
      .shouldNotBeEditable();
  }

  @Test
  public void display_tags_with_edit() {
    executeBuild("shared/xoo-sample", "sample-with-tags", "Sample with tags");
    // Add some tags to another project to have them in the list
    tester.wsClient().wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", "sample-with-tags")
        .setParam("tags", "foo,bar,baz"));

    executeBuild("shared/xoo-sample", "sample", "Sample");
    ProjectDashboardPage page = tester.openBrowser().logIn().submitCredentials(adminUser).openProjectDashboard("sample");
    page
      .shouldHaveTags("No tags")
      .shouldBeEditable()
      .openTagEditor()
      .getTagAtIdx(2).click();
    page
      .shouldHaveTags("foo")
      .sendKeysToTagsInput("test")
      .getTagAtIdx(0).should(text("+ test")).click();
    page
      .shouldHaveTags("foo", "test")
      .getTagAtIdx(1).should(text("test"));
    page
      .sendKeysToTagsInput(Keys.ENTER)
      .shouldHaveTags("test");
  }

  @Test
  public void display_project_activity_shortcut() {
    executeBuild("shared/xoo-sample", "sample-with-tags", "Sample with tags");
    // Add some tags to another project to have them in the list
    tester.wsClient().wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", "sample-with-tags")
        .setParam("tags", "foo,bar,baz"));

    executeBuild("shared/xoo-sample", "sample", "Sample");
    ProjectDashboardPage page = tester.openBrowser()
      .logIn()
      .submitCredentials(adminUser)
      .openProjectDashboard("sample");
    page.getOverviewMeasure("Debt").$(".overview-domain-measure-history-link").should(exist);
  }

  @Test
  public void display_a_nice_error_when_requesting_unknown_project() {
    tester.openBrowser().open("/dashboard/index?id=unknown");
    Selenide.$("#nonav").should(text("The requested project does not exist. Either it has never been analyzed successfully or it has been deleted."));
    // TODO verify that on global homepage
  }

  private void executeBuild(String projectLocation, String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir(projectLocation))
        .setProjectKey(projectKey)
        .setProjectName(projectName));
  }

}
