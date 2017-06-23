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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectDashboardPage;
import util.user.UserRule;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.text;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class ProjectDashboardTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private Navigation nav = Navigation.create(orchestrator);

  private static WsClient wsClient;
  private String adminUser;

  @Before
  public void setUp() throws Exception {
    wsClient = newAdminWsClient(orchestrator);
    orchestrator.resetData();
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void after_first_analysis() throws Exception {
    executeBuild("shared/xoo-sample", "project-for-overview", "Project For Overview");

    runSelenese(orchestrator, "/measure/ProjectDashboardTest/test_project_overview_after_first_analysis.html");
  }

  @Test
  public void display_size() {
    executeBuild("shared/xoo-sample", "sample", "Sample");

    ProjectDashboardPage page = Navigation.create(orchestrator).openProjectDashboard("sample");

    page.getLinesOfCode().should(hasText("13"));
    page.getLanguageDistribution().should(hasText("Xoo"), hasText("13"));
  }

  @Test
  public void display_tags_without_edit() {
    executeBuild("shared/xoo-sample", "sample", "Sample");

    // Add some tags to the project
    wsClient.wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", "sample")
        .setParam("tags", "foo,bar,baz"));

    ProjectDashboardPage page = Navigation.create(orchestrator).openProjectDashboard("sample");
    page
      .shouldHaveTags("foo", "bar", "baz")
      .shouldNotBeEditable();
  }

  @Test
  public void display_tags_with_edit() {
    executeBuild("shared/xoo-sample", "sample-with-tags", "Sample with tags");
    // Add some tags to another project to have them in the list
    wsClient.wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", "sample-with-tags")
        .setParam("tags", "foo,bar,baz"));

    executeBuild("shared/xoo-sample", "sample", "Sample");
    ProjectDashboardPage page = nav.logIn().submitCredentials(adminUser).openProjectDashboard("sample");
    page
      .shouldHaveTags("No tags")
      .shouldBeEditable()
      .openTagEditor()
      .getTagAtIdx(2).click();
    page
      .shouldHaveTags("foo")
      .sendKeysToTagsInput("test")
      .getTagAtIdx(0).should(hasText("+ test")).click();
    page
      .shouldHaveTags("foo", "test")
      .getTagAtIdx(1).should(hasText("test"));
    page
      .sendKeysToTagsInput(Keys.ENTER)
      .shouldHaveTags("test");
  }

  @Test
  @Ignore("there is no more place to show the error")
  public void display_a_nice_error_when_requesting_unknown_project() {
    Navigation nav = Navigation.create(orchestrator);
    nav.open("/dashboard/index?id=unknown");
    nav.getErrorMessage().should(text("The requested project does not exist. Either it has never been analyzed successfully or it has been deleted."));
    // TODO verify that on global homepage
  }

  private void executeBuild(String projectLocation, String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir(projectLocation))
        .setProjectKey(projectKey)
        .setProjectName(projectName));
  }

}
