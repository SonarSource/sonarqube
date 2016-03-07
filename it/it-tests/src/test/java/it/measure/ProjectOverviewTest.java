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
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class ProjectOverviewTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void test_project_overview_after_first_analysis() throws Exception {
    executeBuild("shared/xoo-sample", "project-for-overview", "Project For Overview");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_project_overview_after_first_analysis",
      "/measure/ProjectOverviewTest/test_project_overview_after_first_analysis.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_display_a_nice_error_when_requesting_unknown_project() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_a_nice_error_when_requesting_unknown_project",
      "/measure/ProjectOverviewTest/should-display-nice-error-on-unknown-project.html").build();
    orchestrator.executeSelenese(selenese);
  }

  private void executeBuild(String projectLocation, String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir(projectLocation))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );
  }

}
