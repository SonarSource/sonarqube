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
package it.projectComparison;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

@Category(QaOnly.class)
public class ProjectComparisonTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-sample"))
        .setProjectKey("project-comparison-test-project")
        .setProjectName("ProjectComparisonTest Project")
    );
  }

  @Test
  @Ignore("need to find a way to type into invisible input fields")
  public void test_project_comparison_service() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_project_comparison_service",
      "/projectComparison/ProjectComparisonTest/should-display-basic-set-of-metrics.html",
      "/projectComparison/ProjectComparisonTest/should-add-projects.html",
      "/projectComparison/ProjectComparisonTest/should-move-and-remove-projects.html",
      "/projectComparison/ProjectComparisonTest/should-add-metrics.html",
      "/projectComparison/ProjectComparisonTest/should-not-add-differential-metrics.html",
      "/projectComparison/ProjectComparisonTest/should-move-and-remove-metrics.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
