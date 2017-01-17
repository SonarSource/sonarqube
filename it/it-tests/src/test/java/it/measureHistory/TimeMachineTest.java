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
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.util.Date;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineQuery;
import org.sonarqube.ws.WsMeasures.Measure;
import util.ItUtils;
import util.ItUtils.ComponentNavigation;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getComponentNavigation;
import static util.ItUtils.getMeasuresByMetricKey;
import static util.ItUtils.getMeasuresWithVariationsByMetricKey;
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
    return orchestrator.executeBuild(SonarScanner.create(projectDir(path), "sonar.projectDate", date));
  }

  @Test
  public void projectIsAnalyzed() {
    ComponentNavigation component = getComponentNavigation(orchestrator, PROJECT);
    assertThat(component.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(component.getDate().getMonth()).isEqualTo(10); // November
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
    Map<String, Measure> measures = getMeasuresWithVariationsByMetricKey(orchestrator, PROJECT, "files", "ncloc", "violations");
    // variations from previous analysis
    assertThat(parseDouble(measures.get("files").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(1.0);
    assertThat(parseDouble(measures.get("ncloc").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(16.0);
    assertThat(parseDouble(measures.get("violations").getPeriods().getPeriodsValue(0).getValue())).isGreaterThan(0.0);
  }

  /**
   * SONAR-4962
   */
  @Test
  public void measure_variations_are_only_meaningful_when_additional_fields_contains_periods() {
    Map<String, Measure> measures = getMeasuresWithVariationsByMetricKey(orchestrator, PROJECT, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNotNull();

    measures = getMeasuresByMetricKey(orchestrator, PROJECT, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNull();
  }
}
