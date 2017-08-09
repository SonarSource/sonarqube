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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.sonarqube.pageobjects.measures.MeasureContent;
import org.sonarqube.pageobjects.measures.MeasuresPage;
import org.sonarqube.tests.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.tests.Tester;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class ProjectMeasuresPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private static String projectKey = "project-measures-page-test-project";

  @Before
  public void inspectProject() {
    orchestrator.executeBuild(
      SonarScanner
        .create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", projectKey)
        .setProperty("sonar.projectName", "ProjectMeasuresPageTest Project"));

    // one more time
    orchestrator.executeBuild(
      SonarScanner
        .create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", projectKey)
        .setProperty("sonar.projectName", "ProjectMeasuresPageTest Project"));
  }

  @Test
  public void should_display_measures_page() {
    MeasuresPage page = tester.openBrowser().openProjectMeasures(projectKey);
    page
      .displayBubbleChart("Risk")
      .measureHasValue("code_smells", 0)
      .measureHasLeak("new_code_smells", 0);
    page
      .openMeasureContent("code_smells")
      .shouldHaveFile("src/main/xoo/sample")
      .shouldHaveHeaderValue("0");
  }

  @Test
  public void should_drilldown_on_list_view() {
    MeasuresPage page = tester.openBrowser().openProjectMeasures(projectKey);
    MeasureContent content = page.openMeasureContent("ncloc");
    content
      .drillDown("src/main/xoo/sample/Sample.xoo")
      .shouldHaveHeaderValue("13")
      .shouldDisplayCode();
    page
      .breadcrumbsShouldHave("Sample.xoo")
      .backShortcut()
      .breadcrumbsShouldNotHave("Sample.xoo");
    content.shouldHaveFile("src/main/xoo/sample/Sample.xoo");
  }

  @Test
  public void should_drilldown_on_tree_view() {
    MeasuresPage page = tester.openBrowser().openProjectMeasures(projectKey);
    MeasureContent content = page.openMeasureContent("ncloc");
    page.switchView("tree");
    content
      .shouldHaveFile("src/main/xoo/sample");
    page.breadcrumbsShouldNotHave("src/main/xoo/sample").breadcrumbsShouldNotHave("Sample.xoo");
    content.drillDown("src/main/xoo/sample")
      .shouldHaveFile("src/main/xoo/sample/Sample.xoo");
    page.breadcrumbsShouldHave("src/main/xoo/sample").breadcrumbsShouldNotHave("Sample.xoo");
    content.drillDown("src/main/xoo/sample/Sample.xoo").shouldDisplayCode();
    page
      .breadcrumbsShouldHave("Sample.xoo")
      .backShortcut()
      .breadcrumbsShouldHave("src/main/xoo/sample")
      .breadcrumbsShouldNotHave("Sample.xoo");
  }

  @Test
  @Ignore
  public void should_redirect_history_to_project_activity() {
    Navigation nav = tester.openBrowser();
    nav.open("/component_measures/metric/reliability_rating/history?id=project-measures-page-test-project");
    assertThat(url())
      .contains("/project/activity")
      .contains("id=project-measures-page-test-project")
      .contains("graph=custom")
      .contains("custom_metrics=reliability_rating");
  }

  @Test
  public void should_show_link_to_history() {
    Navigation nav = tester.openBrowser();
    nav.open("/component_measures?id=project-measures-page-test-project&metric=reliability_rating");
    $(".js-show-history").shouldBe(visible).click();
    $("#project-activity").shouldBe(visible);
  }

}
