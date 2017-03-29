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
package it.projectSearch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import pageobjects.Navigation;
import pageobjects.projects.ProjectsPage;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class ProjectsPageTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  private static WsClient wsClient;
  private static final String PROJECT_KEY = "key-foo";

  @BeforeClass
  public static void setUp() {
    wsClient = newAdminWsClient(ORCHESTRATOR);
    ORCHESTRATOR.resetData();
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProjectKey(PROJECT_KEY));
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir("duplications/file-duplications")).setProjectKey("key-bar"));
  }

  @Test
  public void should_display_projects() {
    ProjectsPage page = nav.openProjects();
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
    ProjectsPage page = nav.openProjects();
    page.getFacetByProperty("duplications")
      .shouldHaveValue("1", "1")
      .shouldHaveValue("2", "1")
      .shouldHaveValue("3", "1")
      .shouldHaveValue("4", "1")
      .shouldHaveValue("5", "1");
  }

  @Test
  public void should_filter_using_facet() {
    ProjectsPage page = nav.openProjects();
    page.shouldHaveTotal(2);
    page.getFacetByProperty("duplications").selectValue("3");
    page.shouldHaveTotal(1);
  }

  @Test
  public void should_open_default_page() {
    // default page can be "All Projects" or "Favorite Projects" depending on your last choice
    ProjectsPage page = nav.openProjects();

    // all projects for anonymous user
    page.shouldHaveTotal(2).shouldDisplayAllProjects();

    // all projects by default for logged in user
    page = nav.logIn().asAdmin().openProjects();
    page.shouldHaveTotal(2).shouldDisplayAllProjects();

    // favorite one project
    wsClient.favorites().add(PROJECT_KEY);
    page = nav.openProjects();
    page.shouldHaveTotal(1).shouldDisplayFavoriteProjects();

    // un-favorite this project
    wsClient.favorites().remove(PROJECT_KEY);
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
    ProjectsPage page = nav.openProjects();
    page.getFacetByProperty("languages")
      .selectOptionItem("xoo2")
      .shouldHaveValue("xoo2", "0");
  }

  @Test
  public void should_add_tag_to_facet() {
    // Add some tags to this project
    wsClient.wsConnector().call(
      new PostRequest("api/project_tags/set")
        .setParam("project", PROJECT_KEY)
        .setParam("tags", "aa,bb,cc,dd,ee,ff,gg,hh,ii,jj,zz")
    );

    ProjectsPage page = nav.openProjects();
    page.getFacetByProperty("tags")
      .shouldHaveValue("aa", "1")
      .shouldHaveValue("ii", "1")
      .selectOptionItem("zz")
      .shouldHaveValue("zz", "1");
  }

  @Test
  public void should_sort_by_facet() {
    ProjectsPage page = nav.openProjects();
    page.getFacetByProperty("duplications")
      .sortListDesc();
    page.getProjectByIdx(0).shouldHaveMeasure("duplicated_lines_density", "63.7%");
    page.getFacetByProperty("duplications")
      .sortListAsc();
    page.getProjectByIdx(0).shouldHaveMeasure("duplicated_lines_density", "0.0%");
  }

  @Test
  public void should_search_for_project() {
    ProjectsPage page = nav.openProjects();
    page.searchProject("s").shouldHaveTotal(2);
    page.searchProject("sam").shouldHaveTotal(1);
  }

  @Test
  public void should_search_for_project_and_keep_other_filters() {
    ProjectsPage page = nav.openProjects();
    page.shouldHaveTotal(2);
    page.getFacetByProperty("duplications").selectValue("3");
    page.shouldHaveTotal(1);
    page.searchProject("sample").shouldHaveTotal(0);
  }
}
