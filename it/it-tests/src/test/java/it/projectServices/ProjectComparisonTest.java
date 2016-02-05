/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.projectServices;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class ProjectComparisonTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

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
      "/projectServices/ProjectComparisonTest/should-display-basic-set-of-metrics.html",
      "/projectServices/ProjectComparisonTest/should-add-projects.html",
      "/projectServices/ProjectComparisonTest/should-move-and-remove-projects.html",
      "/projectServices/ProjectComparisonTest/should-add-metrics.html",
      "/projectServices/ProjectComparisonTest/should-not-add-differential-metrics.html",
      "/projectServices/ProjectComparisonTest/should-move-and-remove-metrics.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
