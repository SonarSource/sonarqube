/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import pageobjects.Navigation;
import pageobjects.ProjectDashboardPage;
import util.selenium.SeleneseTest;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.text;
import static util.ItUtils.projectDir;

public class ProjectDashboardTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void after_first_analysis() throws Exception {
    executeBuild("shared/xoo-sample", "project-for-overview", "Project For Overview");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("after_first_analysis",
      "/measure/ProjectDashboardTest/test_project_overview_after_first_analysis.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void display_size() {
    executeBuild("shared/xoo-sample", "sample", "Sample");

    Navigation nav = Navigation.get(orchestrator);
    ProjectDashboardPage page = nav.openProjectDashboard("sample");

    page.getLinesOfCode().should(hasText("13"));
    page.getLanguageDistribution().should(hasText("Xoo"), hasText("13"));
  }

  @Test
  public void display_a_nice_error_when_requesting_unknown_project() {
    Navigation nav = Navigation.get(orchestrator);
    nav.open("/dashboard/index?id=unknown");
    nav.getErrorMessage().should(text("The requested project does not exist. Either it has never been analyzed successfully or it has been deleted."));
    // TODO verify that on global homepage
  }

  private void executeBuild(String projectLocation, String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir(projectLocation))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );
  }

}
