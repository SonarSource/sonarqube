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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the extension point IssueFilter
 */
public class IssueFilterExtensionTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private final String manyRuleProfileKey = projectAnalysisRule.registerProfile("/issue/IssueFilterExtensionTest/xoo-with-many-rules.xml");
  private final String xooMultiModuleProjectKey = projectAnalysisRule.registerProject("shared/xoo-multi-modules-sample");
  private final ProjectAnalysis analysis = projectAnalysisRule.newProjectAnalysis(xooMultiModuleProjectKey)
    .withQualityProfile(manyRuleProfileKey);

  @Test
  public void should_filter_files() throws Exception {
    analysis.withProperties("sonar.exclusions", "**/HelloA1.xoo").run();

    List<Issue> issues = searchIssues();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      // verify exclusion to avoid false positive
      assertThat(issue.componentKey()).doesNotContain("HelloA1");
    }

    assertThat(getMeasure(xooMultiModuleProjectKey, "violations").getIntValue()).isEqualTo(issues.size());
  }

  @Test
  public void should_filter_issues() {
    // first analysis without issue-filter
    analysis.run();

    // Issue filter removes issues on lines < 5
    // Deprecated violation filter removes issues detected by PMD
    List<Issue> unresolvedIssues = searchResolvedIssues(xooMultiModuleProjectKey);
    int issuesBeforeLine5 = countIssuesBeforeLine5(unresolvedIssues);
    int pmdIssues = countModuleIssues(unresolvedIssues);
    assertThat(issuesBeforeLine5).isGreaterThan(0);
    assertThat(pmdIssues).isGreaterThan(0);

    // Enable issue filters
    analysis.withProperties("enableIssueFilters", "true").run();

    unresolvedIssues = searchResolvedIssues(xooMultiModuleProjectKey);
    List<Issue> resolvedIssues = searchUnresolvedIssues(xooMultiModuleProjectKey);
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
    Resource resource = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, metricKey));
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
