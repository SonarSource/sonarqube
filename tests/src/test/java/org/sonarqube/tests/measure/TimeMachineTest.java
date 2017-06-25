/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.WsMeasures.SearchHistoryResponse;
import org.sonarqube.ws.WsMeasures.SearchHistoryResponse.HistoryValue;
import org.sonarqube.ws.client.measure.MeasuresService;
import org.sonarqube.ws.client.measure.SearchHistoryRequest;
import util.ItUtils;
import util.ItUtils.ComponentNavigation;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.getComponentNavigation;
import static util.ItUtils.getMeasuresByMetricKey;
import static util.ItUtils.getMeasuresWithVariationsByMetricKey;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class TimeMachineTest {

  private static final String PROJECT = "sample";
  private static final String FIRST_ANALYSIS_DATE = "2014-10-19";
  private static final String SECOND_ANALYSIS_DATE = "2014-11-13";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  private static MeasuresService wsMeasures;

  @BeforeClass
  public static void initialize() {
    orchestrator.resetData();
    initPeriod();
    ItUtils.restoreProfile(orchestrator, TimeMachineTest.class.getResource("/measure/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    analyzeProject("measure/xoo-history-v1", FIRST_ANALYSIS_DATE);
    analyzeProject("measure/xoo-history-v2", SECOND_ANALYSIS_DATE);

    wsMeasures = newAdminWsClient(orchestrator).measures();
  }

  private static void initPeriod() {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");
  }

  @AfterClass
  public static void resetPeriod() throws Exception {
    ItUtils.resetPeriod(orchestrator);
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
    SearchHistoryResponse response = searchHistory("blocker_violations", "critical_violations", "info_violations", "major_violations", "minor_violations");
    assertThat(response.getPaging().getTotal()).isEqualTo(2);

    assertHistory(response, "blocker_violations", "0", "0");
    assertHistory(response, "critical_violations", "0", "0");
    assertHistory(response, "info_violations", "0", "0");
    assertHistory(response, "major_violations", "0", "0");
    assertHistory(response, "minor_violations", "26", "43");
  }

  @Test
  public void testHistoryOfMeasures() {
    SearchHistoryResponse response = searchHistory("lines", "ncloc");

    assertThat(response.getPaging().getTotal()).isEqualTo(2);
    assertHistory(response, "lines", "26", "43");
    assertHistory(response, "ncloc", "24", "40");
  }

  @Test
  public void noDataForInterval() {
    Date now = new Date();

    SearchHistoryResponse response = wsMeasures.searchHistory(SearchHistoryRequest.builder()
      .setComponent(PROJECT)
      .setMetrics(singletonList("lines"))
      .setFrom(formatDate(now))
      .setTo(formatDate(now))
      .build());

    assertThat(response.getPaging().getTotal()).isEqualTo(0);
    assertThat(response.getMeasures(0).getHistoryList()).isEmpty();
  }

  /**
   * SONAR-4962
   */
  @Test
  public void measure_variations_are_only_meaningful_when_additional_fields_contains_periods() {
    Map<String, Measure> measures = getMeasuresWithVariationsByMetricKey(orchestrator, PROJECT, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNotNull();
    SearchHistoryResponse response = searchHistory("new_violations");
    assertThat(response.getMeasures(0).getHistoryCount()).isGreaterThan(0);

    measures = getMeasuresByMetricKey(orchestrator, PROJECT, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNull();
  }

  private static SearchHistoryResponse searchHistory(String... metrics) {
    return wsMeasures.searchHistory(SearchHistoryRequest.builder()
      .setComponent(PROJECT)
      .setMetrics(Arrays.asList(metrics))
      .build());
  }

  private static void assertHistory(SearchHistoryResponse response, String metric, String... expectedMeasures) {
    for (SearchHistoryResponse.HistoryMeasure measures : response.getMeasuresList()) {
      if (metric.equals(measures.getMetric())) {
        assertThat(measures.getHistoryList()).extracting(HistoryValue::getValue).containsExactly(expectedMeasures);
        return;
      }
    }

    throw new IllegalArgumentException("Metric not found");
  }
}
