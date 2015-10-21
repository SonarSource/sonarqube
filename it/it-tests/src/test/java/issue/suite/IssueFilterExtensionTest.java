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
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static issue.suite.IssueTestSuite.ORCHESTRATOR;
import static issue.suite.IssueTestSuite.searchIssues;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

/**
 * Tests the extension point IssueFilter
 */
public class IssueFilterExtensionTest {

  private static final String MULTI_MODULES_SAMPLE_PROJECT_NAME = "com.sonarsource.it.samples:multi-modules-sample";

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/IssueFilterExtensionTest/xoo-with-many-rules.xml"));
  }

  @Test
  public void should_filter_files() throws Exception {
    orchestrator.getServer().provisionProject(MULTI_MODULES_SAMPLE_PROJECT_NAME, "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULES_SAMPLE_PROJECT_NAME, "xoo", "with-many-rules");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.exclusions", "**/HelloA1.xoo");

    List<Issue> issues = searchIssues();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      // verify exclusion to avoid false positive
      assertThat(issue.componentKey()).doesNotContain("HelloA1");
    }

    assertThat(getMeasure(MULTI_MODULES_SAMPLE_PROJECT_NAME, "violations").getIntValue()).isEqualTo(issues.size());
  }

  @Test
  public void should_filter_issues() {
    // first analysis without isssue-filter
    orchestrator.getServer().provisionProject(MULTI_MODULES_SAMPLE_PROJECT_NAME, "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULES_SAMPLE_PROJECT_NAME, "xoo", "with-many-rules");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample");

    // Issue filter removes issues on lines < 5
    // Deprecated violation filter removes issues detected by PMD
    List<Issue> unresolvedIssues = searchResolvedIssues(MULTI_MODULES_SAMPLE_PROJECT_NAME);
    int issuesBeforeLine5 = countIssuesBeforeLine5(unresolvedIssues);
    int pmdIssues = countModuleIssues(unresolvedIssues);
    assertThat(issuesBeforeLine5).isGreaterThan(0);
    assertThat(pmdIssues).isGreaterThan(0);

    // Enable issue filters
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "enableIssueFilters", "true");

    unresolvedIssues = searchResolvedIssues(MULTI_MODULES_SAMPLE_PROJECT_NAME);
    List<Issue> resolvedIssues = searchUnresolvedIssues(MULTI_MODULES_SAMPLE_PROJECT_NAME);
    assertThat(countIssuesBeforeLine5(unresolvedIssues)).isZero();
    assertThat(countModuleIssues(unresolvedIssues)).isZero();
    assertThat(countModuleIssues(resolvedIssues)).isGreaterThan(0);
    for (Issue issue : resolvedIssues) {
      // SONAR-6364 no line number on closed issues
      assertThat(issue.line()).isNull();
    }
  }

  private static List<Issue> searchUnresolvedIssues(String projectName) {
    return searchIssues(IssueQuery.create().componentRoots(projectName).resolved(true));
  }

  private static List<Issue> searchResolvedIssues(String projectName) {
    return searchIssues(IssueQuery.create().componentRoots(projectName).resolved(false));
  }

  private static Measure getMeasure(String projectKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, metricKey));
    return resource == null ? null : resource.getMeasure(metricKey);
  }

  private static int countModuleIssues(List<Issue> issues) {
    int count = 0;
    for (Issue issue : issues) {
      if (issue.ruleKey().equals("xoo:OneIssuePerModule")) {
        count++;
      }
    }
    return count;
  }

  private static int countIssuesBeforeLine5(List<Issue> issues) {
    int count = 0;
    for (Issue issue : issues) {
      if (issue.line() != null && issue.line() < 5) {
        count++;
      }
    }
    return count;
  }
}
