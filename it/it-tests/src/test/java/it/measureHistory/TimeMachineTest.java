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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class TimeMachineTest {

  private static final String PROJECT = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initialize() {
    orchestrator.resetData();
    initPeriods();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    analyzeProject("measure/xoo-history-v1", "2014-10-19");
    analyzeProject("measure/xoo-history-v2", "2014-11-13");
  }

  public static void initPeriods() {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  private static BuildResult analyzeProject(String path, String date) {
    return orchestrator.executeBuild(SonarRunner.create(projectDir(path), "sonar.projectDate", date));
  }

  @Test
  public void projectIsAnalyzed() {
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT)).getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT)).getDate().getMonth()).isEqualTo(10); // November
  }

  @Test
  public void testHistoryOfIssues() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT, "blocker_violations", "critical_violations", "major_violations",
      "minor_violations", "info_violations");
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(query);
    assertThat(timemachine.getCells().length).isEqualTo(2);

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth()).isEqualTo(9);
    assertThat(cell1.getValues()).isEqualTo(new Object[] {0L, 0L, 0L, 26L, 0L});

    assertThat(cell2.getDate().getMonth()).isEqualTo(10);
    assertThat(cell2.getValues()).isEqualTo(new Object[] {0L, 0L, 0L, 43L, 0L});
  }

  @Test
  public void testHistoryOfMeasures() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT, "lines", "ncloc");
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(query);
    assertThat(timemachine.getCells().length).isEqualTo(2);

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth()).isEqualTo(9);
    assertThat(cell1.getValues()).isEqualTo(new Object[] {26L, 24L});

    assertThat(cell2.getDate().getMonth()).isEqualTo(10);
    assertThat(cell2.getValues()).isEqualTo(new Object[] {43L, 40L});
  }

  @Test
  public void unknownMetrics() {
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "notfound"));
    assertThat(timemachine.getCells().length).isEqualTo(0);

    timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "lines", "notfound"));
    assertThat(timemachine.getCells().length).isEqualTo(2);
    for (TimeMachineCell cell : timemachine.getCells()) {
      assertThat(cell.getValues().length).isEqualTo(1);
      assertThat(cell.getValues()[0]).isInstanceOf(Long.class);
    }

    timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT));
    assertThat(timemachine.getCells().length).isEqualTo(0);
  }

  @Test
  public void noDataForInterval() {
    Date now = new Date();
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "lines").setFrom(now).setTo(now));
    assertThat(timemachine.getCells().length).isEqualTo(0);
  }

  @Test
  public void unknownResource() {
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics("notfound:notfound", "lines"));
    assertThat(timemachine).isNull();
  }

  @Test
  public void test_measure_variations() {
    Resource project = getProject("files", "ncloc", "violations");

    // period 1 : previous analysis
    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");
    assertThat(project.getPeriod1Date()).isNotNull();

    // variations from previous analysis
    assertThat(project.getMeasure("files").getVariation1()).isEqualTo(1.0);
    assertThat(project.getMeasure("ncloc").getVariation1()).isEqualTo(16.0);
    assertThat(project.getMeasure("violations").getVariation1()).isGreaterThan(0.0);
  }

  /**
   * SONAR-4962
   */
  @Test
  public void measure_variations_are_only_meaningful_when_includetrends() {
    String[] metricKeys = {"violations", "new_violations"};

    Resource projectWithTrends = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
    assertThat(projectWithTrends.getMeasure("violations")).isNotNull();
    assertThat(projectWithTrends.getMeasure("new_violations")).isNotNull();

    Resource projectWithoutTrends = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(false));
    assertThat(projectWithoutTrends.getMeasure("violations")).isNotNull();
    assertThat(projectWithoutTrends.getMeasure("new_violations")).isNull();
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }
}
