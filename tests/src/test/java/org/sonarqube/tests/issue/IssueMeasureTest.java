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
package org.sonarqube.tests.issue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.runProjectAnalysis;

public class IssueMeasureTest extends AbstractIssueTest {

  private static final String MULTI_MODULE_SAMPLE_PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String SAMPLE_PROJECT_KEY = "sample";

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void issues_by_severity_measures() {
    ORCHESTRATOR.getServer().provisionProject(MULTI_MODULE_SAMPLE_PROJECT_KEY, MULTI_MODULE_SAMPLE_PROJECT_KEY);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/with-many-rules.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(MULTI_MODULE_SAMPLE_PROJECT_KEY, "xoo", "with-many-rules");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    assertThat(search(IssueQuery.create().componentRoots(MULTI_MODULE_SAMPLE_PROJECT_KEY)).paging().total()).isEqualTo(128);

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(ORCHESTRATOR, MULTI_MODULE_SAMPLE_PROJECT_KEY, "violations", "info_violations", "minor_violations",
      "major_violations",
      "blocker_violations", "critical_violations");
    assertThat(measures.get("violations")).isEqualTo(128);
    assertThat(measures.get("info_violations")).isEqualTo(2);
    assertThat(measures.get("minor_violations")).isEqualTo(61);
    assertThat(measures.get("major_violations")).isEqualTo(57);
    assertThat(measures.get("blocker_violations")).isEqualTo(4);
    assertThat(measures.get("critical_violations")).isEqualTo(4);
  }

  /**
   * SONAR-4330
   * SONAR-7555
   */
  @Test
  public void issues_by_resolution_and_status_measures() {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "one-issue-per-line-profile");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    List<Issue> issues = searchIssuesByProject(SAMPLE_PROJECT_KEY);
    assertThat(issues).hasSize(17);

    // 1 is a false-positive, 1 is a won't fix, 1 is confirmed, 1 is reopened, and the remaining ones stays open
    adminIssueClient().doTransition(issues.get(0).key(), "falsepositive");
    adminIssueClient().doTransition(issues.get(1).key(), "wontfix");
    adminIssueClient().doTransition(issues.get(2).key(), "confirm");
    adminIssueClient().doTransition(issues.get(3).key(), "resolve");
    adminIssueClient().doTransition(issues.get(3).key(), "reopen");

    // Re analyze the project to compute measures
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(ORCHESTRATOR, SAMPLE_PROJECT_KEY, "false_positive_issues", "wont_fix_issues", "open_issues", "reopened_issues",
      "confirmed_issues");
    assertThat(measures.get("false_positive_issues")).isEqualTo(1);
    assertThat(measures.get("wont_fix_issues")).isEqualTo(1);
    assertThat(measures.get("open_issues")).isEqualTo(13);
    assertThat(measures.get("reopened_issues")).isEqualTo(1);
    assertThat(measures.get("confirmed_issues")).isEqualTo(1);
  }

  @Test
  public void no_issue_are_computed_on_empty_profile() {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);

    // no active rules
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    assertThat(searchIssuesByProject(SAMPLE_PROJECT_KEY)).isEmpty();

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(ORCHESTRATOR, SAMPLE_PROJECT_KEY, "violations", "blocker_violations");
    assertThat(measures.get("violations")).isEqualTo(0);
    assertThat(measures.get("blocker_violations")).isEqualTo(0);
  }

  /**
   * SONAR-3746
   */
  @Test
  public void issues_measures_on_test_files() {
    String projectKey = "sample-with-tests";
    String testKey = "sample-with-tests:src/test/xoo/sample/SampleTest.xoo";

    ORCHESTRATOR.getServer().provisionProject(projectKey, projectKey);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/one-issue-per-file-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(projectKey, "xoo", "one-issue-per-file-profile");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample-with-tests");

    // Store current number of issues
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(ORCHESTRATOR, testKey, "violations");
    assertThat(measures.get("violations")).isEqualTo(1);
  }

}
