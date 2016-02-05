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
import org.sonar.wsclient.services.ResourceQuery;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class ProjectWidgetsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-sample"))
        .setProjectKey("project-widgets-test-project")
        .setProjectName("ProjectWidgetsTest Project")
    );
  }

  @Test
  public void hotspots() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("hotspots",
      "/projectServices/ProjectWidgetsTest/hotspots/hotspot-metric-widget.html",
      "/projectServices/ProjectWidgetsTest/hotspots/hide-if-no-measures.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void complexity() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("complexity",
      "/projectServices/ProjectWidgetsTest/complexity/complexity-widget.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void description() {
    long projectId = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.create("project-widgets-test-project")).getId();
    long qualityGateId = orchestrator.getServer().adminWsClient().qualityGateClient().show("SonarQube way").id();
    orchestrator.getServer().adminWsClient().qualityGateClient().selectProject(qualityGateId, projectId);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("description",
      "/projectServices/ProjectWidgetsTest/description/description-widget.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  @Ignore
  public void custom_measures() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("custom_measures",
      "/projectServices/ProjectWidgetsTest/custom_measures/should-exclude-new-metrics.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
