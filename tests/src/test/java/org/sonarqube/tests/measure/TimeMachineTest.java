/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Date;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.Measures.SearchHistoryResponse;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryValue;
import org.sonarqube.ws.client.measures.SearchHistoryRequest;
import util.ItUtils;
import util.ItUtils.ComponentNavigation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.getComponentNavigation;
import static util.ItUtils.getMeasuresByMetricKey;
import static util.ItUtils.getMeasuresWithVariationsByMetricKey;
import static util.ItUtils.projectDir;

public class TimeMachineTest {

  private static final String PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void setUp() {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");
    ItUtils.restoreProfile(orchestrator, TimeMachineTest.class.getResource("/measure/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    Date now = new Date();
    String yesterday = formatDate(addDays(now, -1));
    String aMonthAgo = formatDate(addDays(now, -30));
    analyzeProject("measure/xoo-history-v1", aMonthAgo);
    analyzeProject("measure/xoo-history-v2", yesterday);
  }

  @Test
  public void projectIsAnalyzed() {
    ComponentNavigation component = getComponentNavigation(orchestrator, PROJECT_KEY);
    assertThat(component.getVersion()).isEqualTo("1.0-SNAPSHOT");
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

    SearchHistoryResponse response = tester.wsClient().measures().searchHistory(new SearchHistoryRequest()
      .setComponent(PROJECT_KEY)
      .setMetrics(singletonList("lines"))
      .setFrom(formatDate(now))
      .setTo(formatDate(now)));

    assertThat(response.getPaging().getTotal()).isEqualTo(0);
    assertThat(response.getMeasures(0).getHistoryList()).isEmpty();
  }

  /**
   * SONAR-4962
   */
  @Test
  public void measure_variations_are_only_meaningful_when_additional_fields_contains_periods() {
    Map<String, Measure> measures = getMeasuresWithVariationsByMetricKey(orchestrator, PROJECT_KEY, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNotNull();
    SearchHistoryResponse response = searchHistory("new_violations");
    assertThat(response.getMeasures(0).getHistoryCount()).isGreaterThan(0);

    measures = getMeasuresByMetricKey(orchestrator, PROJECT_KEY, "violations", "new_violations");
    assertThat(measures.get("violations")).isNotNull();
    assertThat(measures.get("new_violations")).isNull();
  }

  private static void analyzeProject(String path, String date) {
    orchestrator.executeBuild(SonarScanner.create(projectDir(path), "sonar.projectDate", date));
  }

  private static SearchHistoryResponse searchHistory(String... metrics) {
    return tester.wsClient().measures().searchHistory(new SearchHistoryRequest()
      .setComponent(PROJECT_KEY)
      .setMetrics(asList(metrics)));
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
