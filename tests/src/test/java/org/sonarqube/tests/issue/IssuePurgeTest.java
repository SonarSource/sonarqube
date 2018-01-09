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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuePurgeTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private ProjectAnalysis xooSampleAnalysis;
  private ProjectAnalysis xooMultiModuleAnalysis;

  @Before
  public void setUp() {
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
  public void purge_old_closed_issues() {
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    xooSampleAnalysis.withProperties("sonar.projectDate", "2014-10-01")
      .run();

    // All the issues are open
    List<Issue> issuesList = searchIssues();
    for (Issue issue : issuesList) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolved and closed
    // -> Not deleted because less than 5000 days long
    xooSampleAnalysis
      .withXooEmptyProfile()
      .withProperties("sonar.projectDate", "2014-10-15")
      .run();
    issuesList = searchIssues();
    assertThat(issuesList).isNotEmpty();
    for (Issue issue : issuesList) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");

    xooSampleAnalysis.withXooEmptyProfile()
      .withProperties("sonar.projectDate", "2014-10-20")
      .run();
    Issues issues = issueClient().find(IssueQuery.create());
    assertThat(issues.list()).isEmpty();
    assertThat(issues.paging().total()).isZero();
  }

  /**
   * SONAR-7108
   */
  @Test
  public void purge_old_closed_issues_when_zero_closed_issues_wanted() {
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    xooSampleAnalysis.withProperties("sonar.projectDate", "2014-10-01")
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
      .withProperties("sonar.projectDate", "2014-10-15")
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
      .withProperties("sonar.projectDate", "2014-10-20")
      .run();

    Issues issues = issueClient().find(IssueQuery.create());
    assertThat(issues.list()).isEmpty();
    assertThat(issues.paging().total()).isZero();
  }

  /**
   * SONAR-5200
   */
  @Test
  public void resolve_issues_when_removing_module() {
    // Generate some issues
    xooMultiModuleAnalysis.run();

    // All the issues are open
    List<Issue> issues = searchIssues();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }
    Issue issue = issues.get(0);

    int issuesOnModuleB = searchIssues(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_b")).size();
    assertThat(issuesOnModuleB).isEqualTo(28);

    // Second scan without module B -> issues on module B are resolved as removed and closed
    xooMultiModuleAnalysis
      .withProperties("sonar.modules", "module_a")
      .run();

    // Resolved should should all be mark as REMOVED and affect to module b
    List<Issue> reloadedIssues = searchIssues(IssueQuery.create().resolved(true));
    assertThat(reloadedIssues).hasSize(issuesOnModuleB);
    for (Issue reloadedIssue : reloadedIssues) {
      assertThat(reloadedIssue.resolution()).isEqualTo("FIXED");
      assertThat(reloadedIssue.status()).isEqualTo("CLOSED");
      assertThat(reloadedIssue.componentKey()).contains("com.sonarsource.it.samples:multi-modules-sample:module_b");
      assertThat(reloadedIssue.updateDate().before(issue.updateDate())).isFalse();
      assertThat(reloadedIssue.closeDate()).isNotNull();
      assertThat(reloadedIssue.closeDate().before(reloadedIssue.creationDate())).isFalse();
    }
  }
}
