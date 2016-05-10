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
package it.issue;

import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
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
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/with-many-rules.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(MULTI_MODULE_SAMPLE_PROJECT_KEY, "xoo", "with-many-rules");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    assertThat(search(IssueQuery.create().componentRoots(MULTI_MODULE_SAMPLE_PROJECT_KEY)).paging().total()).isEqualTo(136);

    Resource project = ORCHESTRATOR.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(MULTI_MODULE_SAMPLE_PROJECT_KEY, "violations", "info_violations", "minor_violations", "major_violations",
        "blocker_violations", "critical_violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(136);
    assertThat(project.getMeasureIntValue("info_violations")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("minor_violations")).isEqualTo(61);
    assertThat(project.getMeasureIntValue("major_violations")).isEqualTo(65);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(4);
    assertThat(project.getMeasureIntValue("critical_violations")).isEqualTo(4);
  }

  /**
   * SONAR-4330
   * SONAR-7555
   */
  @Test
  public void issues_by_resolution_and_status_measures() {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-line-profile.xml"));
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

    Resource project = ORCHESTRATOR.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(SAMPLE_PROJECT_KEY, "false_positive_issues", "wont_fix_issues", "open_issues", "reopened_issues", "confirmed_issues"));
    assertThat(project.getMeasureIntValue("false_positive_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("wont_fix_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("open_issues")).isEqualTo(13);
    assertThat(project.getMeasureIntValue("reopened_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("confirmed_issues")).isEqualTo(1);
  }

  @Test
  public void no_issue_are_computed_on_empty_profile() {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);

    // no active rules
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    assertThat(searchIssuesByProject(SAMPLE_PROJECT_KEY)).isEmpty();

    Resource project = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics(SAMPLE_PROJECT_KEY, "violations", "blocker_violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
  }

  /**
   * SONAR-3746
   */
  @Test
  public void issues_measures_on_test_files() {
    String projectKey = "sample-with-tests";
    String testKey = "sample-with-tests:src/test/xoo/sample/SampleTest.xoo";

    ORCHESTRATOR.getServer().provisionProject(projectKey, projectKey);
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-file-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(projectKey, "xoo", "one-issue-per-file-profile");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample-with-tests");

    Sonar wsClient = ORCHESTRATOR.getServer().getAdminWsClient();

    // Store current number of issues
    Resource project = wsClient.find(ResourceQuery.createForMetrics(testKey, "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(1);
  }

}
