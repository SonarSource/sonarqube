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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.client.issues.DoTransitionRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.issues.SetSeverityRequest;
import org.sonarqube.ws.client.issues.SetTypeRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import util.ItUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class LiveMeasuresTest {

  private static final String PROJECT_KEY = "LiveMeasuresTestExample";
  private static final String PROJECT_DIR = "measure/LiveMeasuresTest";
  private static final String FILE_WITH_ONE_LINE_KEY = PROJECT_KEY + ":src/file_with_one_line.xoo";
  private static final String FILE_WITH_THREE_LINES_KEY = PROJECT_KEY + ":src/file_with_three_lines.xoo";
  private static final String SRC_DIR_KEY = PROJECT_KEY + ":src";

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void refresh_measures_when_touching_ws_from_web_services() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/measure/LiveMeasuresTest/one-bug-per-line-profile.xml"));
    Project project = tester.projects().provision(r -> r.setProject(PROJECT_KEY).setName("LiveMeasuresTestExample"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-bug-per-line-profile");
    // quality gate on Security Rating: Warning when greater than A and Error when greater than C
    Qualitygates.CreateResponse simple = tester.qGates().generate();
    tester.qGates().service().createCondition(new CreateConditionRequest().setGateId(String.valueOf(simple.getId()))
      .setMetric("security_rating").setOp("GT").setWarning("1").setError("3"));
    tester.qGates().associateProject(simple, project);
    scanSample();

    expectMeasures("bugs", 1, 3, 4, 4);
    expectMeasures("vulnerabilities", 0, 0, 0, 0);
    expectMeasures("violations", 1, 3, 4, 4);
    // highest severity is MAJOR --> rating is C (3)
    expectMeasures("reliability_rating", 3, 3, 3, 3);
    // zero vulnerabilities -> rating is A (1)
    expectMeasures("security_rating", 1, 1, 1, 1);
    assertQualityGate("OK");

    // mark a bug as FP
    Issues.Issue issue = getFirstIssue(FILE_WITH_ONE_LINE_KEY, "BUG");
    markAsFalsePositive(issue);
    expectMeasures("bugs", 0, 3, 3, 3);
    expectMeasures("vulnerabilities", 0, 0, 0, 0);
    expectMeasures("violations", 0, 3, 3, 3);
    expectMeasures("reliability_rating", 1, 3, 3, 3);
    expectMeasures("security_rating", 1, 1, 1, 1);

    // convert a bug to a vulnerability
    issue = getFirstIssue(FILE_WITH_THREE_LINES_KEY, "BUG");
    convertToType(issue, "VULNERABILITY");
    expectMeasures("bugs", 0, 2, 2, 2);
    expectMeasures("vulnerabilities", 0, 1, 1, 1);
    expectMeasures("violations", 0, 3, 3, 3);
    // highest severity of bugs is still MAJOR --> C (3)
    expectMeasures("reliability_rating", 1, 3, 3, 3);
    // a file has now a MAJOR vulnerability --> C (3)
    expectMeasures("security_rating", 1, 3, 3, 3);
    assertQualityGate("WARN");

    // increase severity of a vulnerability to BLOCKER
    issue = getFirstIssue(FILE_WITH_THREE_LINES_KEY, "VULNERABILITY");
    changeSeverity(issue, "BLOCKER");
    expectMeasures("bugs", 0, 2, 2, 2);
    expectMeasures("vulnerabilities", 0, 1, 1, 1);
    expectMeasures("violations", 0, 3, 3, 3);
    expectMeasures("reliability_rating", 1, 3, 3, 3);
    // highest severity of vulnerabilities is now BLOCKER --> security rating goes E (5)
    expectMeasures("security_rating", 1, 5, 5, 5);
    assertQualityGate("ERROR");

    // no changes after new analysis
    MeasuresDump dumpBeforeAnalysis = dump();
    scanSample();
    MeasuresDump dumpAfterAnalysis = dump();
    dumpAfterAnalysis.assertEquals(dumpBeforeAnalysis);
  }

  private void markAsFalsePositive(Issues.Issue issue) {
    tester.wsClient().issues().doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("falsepositive"));
  }

  private void convertToType(Issues.Issue issue, String type) {
    tester.wsClient().issues().setType(new SetTypeRequest().setIssue(issue.getKey()).setType(type));
  }

  private void changeSeverity(Issues.Issue issue, String severity) {
    tester.wsClient().issues().setSeverity(new SetSeverityRequest().setIssue(issue.getKey()).setSeverity(severity));
  }

  private Issues.Issue getFirstIssue(String componentKey, String type) {
    return tester.wsClient().issues().search(new SearchRequest()
      .setResolved("false")
      .setTypes(singletonList(type))
      .setComponentKeys(singletonList(componentKey))).getIssuesList().get(0);
  }

  private void scanSample() {
    orchestrator.executeBuildQuietly(SonarScanner.create(ItUtils.projectDir(PROJECT_DIR)));
  }

  private void expectMeasures(String metric, double fileWithOneLineExpectedValue, double fileWithThreeLinesExpectedValue, double srcDirExpectedValue, double projectExpectedValue) {
    assertThat(Double.parseDouble(loadMeasure(FILE_WITH_ONE_LINE_KEY, metric).getValue()))
      .as("Value of measure " + metric)
      .isEqualTo(fileWithOneLineExpectedValue);
    assertThat(Double.parseDouble(loadMeasure(FILE_WITH_THREE_LINES_KEY, metric).getValue()))
      .as("Value of measure " + metric)
      .isEqualTo(fileWithThreeLinesExpectedValue);
    assertThat(Double.parseDouble(loadMeasure(SRC_DIR_KEY, metric).getValue()))
      .as("Value of measure " + metric)
      .isEqualTo(srcDirExpectedValue);
    assertThat(Double.parseDouble(loadMeasure(PROJECT_KEY, metric).getValue()))
      .as("Value of measure " + metric)
      .isEqualTo(projectExpectedValue);
  }

  private void assertQualityGate(String expectedQualityGate) {
    assertThat(loadMeasure(PROJECT_KEY, "alert_status").getValue()).isEqualTo(expectedQualityGate);
  }

  private Measures.Measure loadMeasure(String componentKey, String metricKey) {
    return tester.wsClient().measures().component(
      new ComponentRequest()
        .setMetricKeys(singletonList(metricKey))
        .setComponent(componentKey))
      .getComponent().getMeasuresList().get(0);
  }

  private MeasuresDump dump() {
    MeasuresDump dump = new MeasuresDump();

    asList(FILE_WITH_ONE_LINE_KEY, FILE_WITH_THREE_LINES_KEY, SRC_DIR_KEY, PROJECT_KEY).forEach(componentKey -> {
      List<Measures.Measure> measures = tester.wsClient().measures().component(new ComponentRequest()
        .setComponent(componentKey)
        // TODO request all metrics
        .setMetricKeys(asList("bugs", "vulnerabilities", "reliability_rating", "security_rating", "sqale_rating", "major_violations", "blocker_violations"))
      ).getComponent().getMeasuresList();
      measures.forEach(m -> dump.valuesByComponentAndMetric.put(m.getComponent(), m.getMetric(), m.getValue()));
    });

    return dump;
  }

  private static class MeasuresDump {
    private final Table<String, String, String> valuesByComponentAndMetric = HashBasedTable.create();

    void assertEquals(MeasuresDump dump) {
      assertThat(valuesByComponentAndMetric.size()).isEqualTo(dump.valuesByComponentAndMetric.size());
      for (Table.Cell<String, String, String> cell : valuesByComponentAndMetric.cellSet()) {
        assertThat(dump.valuesByComponentAndMetric.get(cell.getRowKey(), cell.getColumnKey()))
          .as("Measure " + cell.getColumnKey() + " on component " + cell.getRowKey())
          .isEqualTo(cell.getValue());
      }
    }
  }
}
