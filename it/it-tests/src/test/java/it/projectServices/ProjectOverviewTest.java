/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.projectServices;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class ProjectOverviewTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Test
  public void test_project_overview_after_first_analysis() throws Exception {
    executeBuild("shared/xoo-sample", "project-for-overview", "Project For Overview");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_project_overview_after_first_analysis",
      "/projectServices/ProjectOverviewTest/test_project_overview_after_first_analysis.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void test_ut_coverage_on_project_overview() throws Exception {
    executeBuild("testing/xoo-sample-ut-coverage", "project-for-overview-ut-coverage", "Project For Overview UT");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_ut_coverage_on_project_overview",
      "/projectServices/ProjectOverviewTest/test_ut_coverage_on_project_overview.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void test_it_coverage_on_project_overview() throws Exception {
    executeBuild("testing/xoo-sample-it-coverage", "project-for-overview-it-coverage", "Project For Overview IT");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_it_coverage_onfi_project_overview",
      "/projectServices/ProjectOverviewTest/test_it_coverage_on_project_overview.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void test_overall_coverage_on_project_overview() throws Exception {
    executeBuild("testing/xoo-sample-overall-coverage", "project-for-overview-overall-coverage", "Project For Overview Overall");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_overall_coverage_on_project_overview",
      "/projectServices/ProjectOverviewTest/test_overall_coverage_on_project_overview.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private void executeBuild(String projectLocation, String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir(projectLocation))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );
  }

}
