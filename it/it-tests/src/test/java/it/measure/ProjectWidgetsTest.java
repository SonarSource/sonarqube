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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.ResourceQuery;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static util.ItUtils.projectDir;

@Category(QaOnly.class)
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
      "/measure/ProjectWidgetsTest/hotspots/hotspot-metric-widget.html",
      "/measure/ProjectWidgetsTest/hotspots/hide-if-no-measures.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void complexity() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("complexity",
      "/measure/ProjectWidgetsTest/complexity/complexity-widget.html"
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
      "/measure/ProjectWidgetsTest/description/description-widget.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  @Ignore
  public void custom_measures() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("custom_measures",
      "/measure/ProjectWidgetsTest/custom_measures/should-exclude-new-metrics.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
