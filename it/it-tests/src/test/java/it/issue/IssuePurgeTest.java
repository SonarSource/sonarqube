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
package it.issue;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuePurgeTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

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
    List<Issue> issues = searchIssues();
    for (Issue issue : issues) {
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
    issues = searchIssues();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    projectAnalysisRule.setServerProperty("sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");

    xooSampleAnalysis.withXooEmptyProfile()
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.projectDate", "2014-10-20")
      .run();
    Assertions.assertThat(searchIssues(IssueQuery.create())).isEmpty();
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
    List<Issue> issues = searchIssues();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }
    Issue issue = issues.get(0);

    int issuesOnModuleB = searchIssues(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_b")).size();
    assertThat(issuesOnModuleB).isEqualTo(28);

    // Second scan without module B -> issues on module B are resolved as removed and closed
    xooMultiModuleAnalysis
      .withProperties(
        "sonar.dynamicAnalysis", "false",
        "sonar.modules", "module_a")
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
