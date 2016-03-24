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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class ProjectMeasuresPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(
      SonarScanner
        .create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", "project-measures-page-test-project")
        .setProperty("sonar.projectName", "ProjectMeasuresPageTest Project")
    );

    // one more time
    orchestrator.executeBuild(
      SonarScanner
        .create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", "project-measures-page-test-project")
        .setProperty("sonar.projectName", "ProjectMeasuresPageTest Project")
    );
  }

  @Test
  public void should_display_measures_page() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_measures_page",
      "/measure/ProjectMeasuresPageTest/should_display_measures_page.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_drilldown_on_list_view() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_drilldown_on_list_view",
      "/measure/ProjectMeasuresPageTest/should_drilldown_on_list_view.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void should_drilldown_on_tree_view() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_drilldown_on_tree_view",
      "/measure/ProjectMeasuresPageTest/should_drilldown_on_tree_view.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
