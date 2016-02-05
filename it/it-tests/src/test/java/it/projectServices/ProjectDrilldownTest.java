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
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.selenium.SeleneseTest;

import java.util.List;

import static util.ItUtils.projectDir;

public class ProjectDrilldownTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-sample"))
        .setProjectKey("project-drilldown-test-project")
        .setProjectName("ProjectDrilldownTest Project")
    );
  }

  @Test
  public void should_display_measure_drilldown() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_display_measure_drilldown",
      "/projectServices/ProjectDrilldownTest/should_display_measure_drilldown.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
