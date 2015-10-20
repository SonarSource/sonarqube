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
package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import static issue.suite.IssueTestSuite.ORCHESTRATOR;
import static issue.suite.IssueTestSuite.searchIssues;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class IssuePurgeTest {

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  @Before
  public void deleteAnalysisData() {
    orchestrator.resetData();
    // reset settings before test
    setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", null);
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/IssuePurgeTest/with-many-rules.xml"));
  }

  /**
   * SONAR-4308
   */
  @Test
  public void purge_old_closed_issues() throws Exception {
    setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.dynamicAnalysis", "false",
      "sonar.projectDate", "2014-10-01");

    // All the issues are open
    List<Issue> issues = searchIssues();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolved and closed
    // -> Not deleted because less than 5000 days long
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "empty");
    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.dynamicAnalysis", "false",
      "sonar.projectDate", "2014-10-15");
    issues = searchIssues();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");

    runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.dynamicAnalysis", "false",
      "sonar.projectDate", "2014-10-20");
    assertThat(searchIssues(IssueQuery.create())).isEmpty();
  }

  /**
   * SONAR-5200
   */
  @Test
  public void resolve_issues_when_removing_module() throws Exception {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "with-many-rules");

    // Generate some issues
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.dynamicAnalysis", "false");

    // All the issues are open
    List<Issue> issues = searchIssues();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }
    Issue issue = issues.get(0);

    int issuesOnModuleB = searchIssues(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_b")).size();
    assertThat(issuesOnModuleB).isEqualTo(28);

    // Second scan without module B -> issues on module B are resolved as removed and closed
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.dynamicAnalysis", "false",
      "sonar.modules", "module_a");

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
