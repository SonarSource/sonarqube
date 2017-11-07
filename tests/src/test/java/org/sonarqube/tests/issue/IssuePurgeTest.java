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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.tests.Category2Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.tests.issue.AbstractIssueTest.searchIssues;
import static util.ItUtils.toDate;
import static util.ItUtils.toDatetime;

public class IssuePurgeTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;
  private static final String ISSUE_STATUS_OPEN = "OPEN";
  @Rule
  public Tester tester = new Tester(orchestrator);
  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(orchestrator);

  private ProjectAnalysis xooSampleAnalysis;
  private ProjectAnalysis xooMultiModuleAnalysis;

  @Before
  public void setUp() throws Exception {
    String manyRulesProfile = projectAnalysisRule.registerProfile("/issue/IssuePurgeTest/with-many-rules.xml");
    String xooSampleProjectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    this.xooSampleAnalysis = projectAnalysisRule.newProjectAnalysis(xooSampleProjectKey)
      .withQualityProfile(manyRulesProfile);
    String xooMultiModuleProjectKey = projectAnalysisRule.registerProject("shared/xoo-multi-modules-sample");
    this.xooMultiModuleAnalysis = projectAnalysisRule.newProjectAnalysis(xooMultiModuleProjectKey)
      .withQualityProfile(manyRulesProfile);
  }

  /**
   * SONAR-4308
   */
  @Test
  public void purge_old_closed_issues() throws Exception {
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    xooSampleAnalysis.withProperties(
      "sonar.dynamicAnalysis", "false",
      "sonar.projectDate", "2014-10-01")
      .run();

    // All the issues are open
    List<org.sonarqube.ws.Issues.Issue> issuesList = search(new SearchWsRequest().setStatuses(singletonList(ISSUE_STATUS_OPEN)));
    issuesList.forEach(i -> assertThat(i.getResolution()).isNullOrEmpty());

    // Second scan with empty profile -> all issues are resolved and closed
    // -> Not deleted because less than 5000 days long
    xooSampleAnalysis
      .withXooEmptyProfile()
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.projectDate", "2014-10-15")
      .run();
    issuesList = search(new SearchWsRequest().setStatuses(singletonList(ISSUE_STATUS_OPEN)));
    assertThat(issuesList).isNotEmpty();
    for (org.sonarqube.ws.Issues.Issue issue : issuesList) {
      assertThat(issue.getResolution()).isNotEmpty();
      assertThat(issue.getStatus()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");

    xooSampleAnalysis.withXooEmptyProfile()
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.projectDate", "2014-10-20")
      .run();
    List<org.sonarqube.ws.Issues.Issue> issues = search(new SearchWsRequest());
    assertThat(issues).isEmpty();
  }

  /**
   * SONAR-7108
   */
  @Test
  public void purge_old_closed_issues_when_zero_closed_issues_wanted() throws Exception {
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    xooSampleAnalysis.withProperties(
      "sonar.dynamicAnalysis", "false",
      "sonar.projectDate", "2014-10-01")
      .run();

    // All the issues are open
    List<Issue> issueList = searchIssues();
    for (Issue issue : issueList) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolved and closed
    // -> Not deleted because less than 5000 days long
    xooSampleAnalysis
      .withXooEmptyProfile()
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.projectDate", "2014-10-15")
      .run();
    issueList = searchIssues();
    assertThat(issueList).isNotEmpty();
    for (Issue issue : issueList) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "0");

    xooSampleAnalysis.withXooEmptyProfile()
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.projectDate", "2014-10-20")
      .run();

    List<org.sonarqube.ws.Issues.Issue> issues = search(new SearchWsRequest());
    assertThat(issues).isEmpty();
  }

  /**
   * SONAR-5200
   */
  @Test
  public void resolve_issues_when_removing_module() throws Exception {
    // Generate some issues
    xooMultiModuleAnalysis
      .withProperties("sonar.dynamicAnalysis", "false")
      .run();

    // All the issues are open
    List<org.sonarqube.ws.Issues.Issue> issues = search(new SearchWsRequest().setStatuses(singletonList(ISSUE_STATUS_OPEN)));
    issues.forEach(i -> assertThat(i.getResolution()).isNullOrEmpty());
    org.sonarqube.ws.Issues.Issue issue = issues.get(0);

    int issuesOnModuleB = searchIssues(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_b")).size();
    assertThat(issuesOnModuleB).isEqualTo(28);

    // Second scan without module B -> issues on module B are resolved as removed and closed
    xooMultiModuleAnalysis
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.modules", "module_a")
      .run();

    // Resolved should should all be mark as REMOVED and affect to module b
    List<org.sonarqube.ws.Issues.Issue> reloadedIssues = search(new SearchWsRequest().setResolved(true));
    assertThat(reloadedIssues).hasSize(issuesOnModuleB);
    for (org.sonarqube.ws.Issues.Issue reloadedIssue : reloadedIssues) {
      assertThat(reloadedIssue.getResolution()).isEqualTo("FIXED");
      assertThat(reloadedIssue.getStatus()).isEqualTo("CLOSED");
      assertThat(reloadedIssue.getComponent()).contains("com.sonarsource.it.samples:multi-modules-sample:module_b");
      assertThat(toDatetime(reloadedIssue.getUpdateDate()).before(toDate(issue.getUpdateDate()))).isFalse();
      assertThat(reloadedIssue.getCloseDate()).isNotNull();
      assertThat(toDatetime(reloadedIssue.getCloseDate()).before(toDatetime(reloadedIssue.getCreationDate()))).isFalse();
    }
  }

  private List<org.sonarqube.ws.Issues.Issue> search(SearchWsRequest request) {
    return tester.wsClient().issues().search(request).getIssuesList();
  }
}
