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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.projects.ProjectsPage;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.favorites.AddRequest;
import org.sonarqube.ws.client.favorites.RemoveRequest;

import static com.codeborne.selenide.Selenide.clearBrowserLocalStorage;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectsPageTest {

  private static final String PROJECT_KEY = "key-foo";

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void setUp() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProjectKey(PROJECT_KEY));
    orchestrator.executeBuild(SonarScanner.create(projectDir("duplications/file-duplications")).setProjectKey("key-bar"));
  }

  @Before
  public void before() {
    clearBrowserLocalStorage();
  }

  @Test
  public void should_display_projects() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.shouldHaveTotal(2);
    page.getProjectByKey(PROJECT_KEY)
      .shouldHaveMeasure("reliability_rating", "A")
      .shouldHaveMeasure("security_rating", "A")
      .shouldHaveMeasure("sqale_rating", "A")
      .shouldHaveMeasure("duplicated_lines_density", "0.0%")
      .shouldHaveMeasure("ncloc", "13")
      .shouldHaveMeasure("ncloc", "Xoo");
  }

  @Test
  public void should_display_facets() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.getFacetByProperty("duplications")
      .shouldHaveValue("1", "1")
      .shouldHaveValue("2", "1")
      .shouldHaveValue("3", "1")
      .shouldHaveValue("4", "1")
      .shouldHaveValue("5", "1")
      .shouldHaveValue("6", "0");
  }

  @Test
  public void should_filter_using_facet() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.shouldHaveTotal(2);
    page.getFacetByProperty("duplications").selectValue("3");
    page.shouldHaveTotal(1);
  }

  @Test
  public void should_open_default_page() {
    // default page can be "All Projects" or "Favorite Projects" depending on your last choice
    Navigation nav = tester.openBrowser();
    ProjectsPage page = nav.openProjects();

    // all projects for anonymous user with default sorting to analysis date
    page.shouldHaveTotal(2).shouldDisplayAllProjectsWidthSort("-analysis_date");

    // all projects by default for logged in user
    Users.CreateWsResponse.User administrator = tester.users().generateAdministratorOnDefaultOrganization();
    page = nav.logIn().submitCredentials(administrator.getLogin()).openProjects();
    page.shouldHaveTotal(2).shouldDisplayAllProjects();

    // favorite one project
    WsClient administratorWsClient = tester.as(administrator.getLogin()).wsClient();
    administratorWsClient.favorites().add(new AddRequest().setComponent(PROJECT_KEY));
    page = nav.openProjects();
    page.shouldHaveTotal(1).shouldDisplayFavoriteProjects();

    // un-favorite this project
    administratorWsClient.favorites().remove(new RemoveRequest().setComponent(PROJECT_KEY));
    page = nav.openProjects();
    page.shouldHaveTotal(2).shouldDisplayAllProjects();

    // select favorite
    page.selectFavoriteProjects();
    page = nav.openProjects();
    page.shouldHaveTotal(0).shouldDisplayFavoriteProjects();

    // select all
    page.selectAllProjects();
    page = nav.openProjects();
    page.shouldHaveTotal(2).shouldDisplayAllProjects();
  }

  @Test
  public void should_add_language_to_facet() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.getFacetByProperty("languages")
      .selectOptionItem("xoo2")
      .shouldHaveValue("xoo2", "0");
  }

  @Test
  public void should_add_tag_to_facet() {
    // Add some tags to this project
    tester.wsClient().wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", PROJECT_KEY)
        .setParam("tags", "aa,bb,cc,dd,ee,ff,gg,hh,ii,jj,zz"));

    ProjectsPage page = tester.openBrowser().openProjects();
    page.getFacetByProperty("tags")
      .shouldHaveValue("aa", "1")
      .shouldHaveValue("ii", "1")
      .selectOptionItem("zz")
      .shouldHaveValue("zz", "1");
  }

  @Test
  public void should_switch_between_perspectives() {
    Users.CreateWsResponse.User administrator = tester.users().generateAdministratorOnDefaultOrganization();
    ProjectsPage page = tester.openBrowser()
      .logIn().submitCredentials(administrator.getLogin())
      .openProjects();
    page.changePerspective("Risk");
    assertThat(url()).endsWith("/projects?view=visualizations&visualization=risk");
    page.changePerspective("Leak");
    assertThat(url()).endsWith("/projects?view=leak");
  }

  @Test
  public void should_sort_by_facet() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.sortProjects("Duplications");
    page.getProjectByIdx(0).shouldHaveMeasure("duplicated_lines_density", "63.7%");
    page.invertSorting();
    page.getProjectByIdx(0).shouldHaveMeasure("duplicated_lines_density", "0.0%");
  }

  @Test
  public void should_search_for_project() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.searchProject("s").shouldHaveTotal(2);
    page.searchProject("sam").shouldHaveTotal(1);
  }

  @Test
  public void should_search_for_project_and_keep_other_filters() {
    ProjectsPage page = tester.openBrowser().openProjects();
    page.shouldHaveTotal(2);
    page.getFacetByProperty("duplications").selectValue("3");
    page.shouldHaveTotal(1);
    page.searchProject("sample").shouldHaveTotal(0);
  }

  @Test
  public void should_open_permalink() {
    String user = tester.users().generate().getLogin();
    Navigation nav = tester.openBrowser().logIn().submitCredentials(user);

    // make a search, so its parameters saved to local storage
    nav.openProjects().changePerspective("Leak");

    // change a page
    nav.openHome();

    // open a permalink to a particular visualization, it must be kept
    nav.openProjectsWithQuery("view=visualizations&visualization=coverage");
    assertThat(url()).contains("view=visualizations&visualization=coverage");
  }
}
