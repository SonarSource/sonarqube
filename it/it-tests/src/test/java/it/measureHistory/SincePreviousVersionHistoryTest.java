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
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class SincePreviousVersionHistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  /**
   * SONAR-2496
   */
  @Test
  public void test_since_previous_version_period() {
    orchestrator.resetData();
    analyzeProject("0.9", "**/*2.xoo");
    analyzeProject("1.0-SNAPSHOT", null);
    analyzeProject("1.0-SNAPSHOT", null);

    Resource project = getProject("files");
    Measure measure = project.getMeasure("files");

    // There are 4 files
    assertThat(measure.getValue()).isEqualTo(4);

    // nothing changed in the previous analysis
    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");
    assertThat(measure.getVariation1()).isEqualTo(0);

    // but 2 files were added since the first analysis which was version 0.9
    assertThat(project.getPeriod3Mode()).isEqualTo("previous_version");
    assertThat(project.getPeriod3Param()).isEqualTo("0.9");
    assertThat(measure.getVariation3()).isEqualTo(2);
  }

  private static void analyzeProject(String version, String exclusions) {
    SonarRunner build = SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectVersion", version);
    if (exclusions != null) {
      build.setProperties("sonar.exclusions", exclusions);
    }
    orchestrator.executeBuild(build);
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }
}
