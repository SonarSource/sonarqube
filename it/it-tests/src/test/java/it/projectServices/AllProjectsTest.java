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
import org.junit.Test;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class AllProjectsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-sample"))
        .setProjectKey("all-project-test-project")
        .setProjectName("AllProjectsTest Project")
    );
  }

  @Test
  public void test_all_projects_page() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_all_projects_page",
      "/projectServices/AllProjectsTest/test_all_projects_page.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
