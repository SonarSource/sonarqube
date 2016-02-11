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

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.sonar.orchestrator.locator.FileLocation;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class IssueTrackingTest extends AbstractIssueTest {

  private static final String SAMPLE_PROJECT_KEY = "sample";

  private static final String OLD_DATE = "2014-03-01";
  private static final String NEW_DATE = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

  @Before
  public void prepareData() {
    ORCHESTRATOR.resetData();
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/issue-on-tag-foobar.xml"));
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/IssueTrackingTest/one-issue-per-module-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);
  }

  @Test
  public void close_issues_on_removed_components() throws Exception {
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");

    // version 1
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", OLD_DATE);

    List<Issue> issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(1);

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", NEW_DATE,
      "sonar.exclusions", "**/*.xoo");

    issues = searchIssuesByProject("sample");
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).status()).isEqualTo("CLOSED");
    assertThat(issues.get(0).resolution()).isEqualTo("FIXED");
  }

  /**
   * SONAR-3072
   */
  @Test
  public void track_issues_based_on_blocks_recognition() throws Exception {
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");

    // version 1
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", OLD_DATE);

    List<Issue> issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(1);
    Date issueDate = issues.iterator().next().creationDate();

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v2",
      "sonar.projectDate", NEW_DATE);

    issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(3);

    // issue created during the first scan and moved during the second scan
    assertThat(getIssueOnLine(6, "xoo", "HasTag", issues).creationDate()).isEqualTo(issueDate);

    // issues created during the second scan
    assertThat(getIssueOnLine(10, "xoo", "HasTag", issues).creationDate()).isAfter(issueDate);
    assertThat(getIssueOnLine(14, "xoo", "HasTag", issues).creationDate()).isAfter(issueDate);
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_module() throws Exception {
    // The custom rule on module is enabled

    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "one-issue-per-module");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    // Only one issue is created
    assertThat(search(IssueQuery.create()).list()).hasSize(1);
    Issue issue = searchRandomIssue();

    // Re analysis of the same project
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    // No new issue should be created
    assertThat(search(IssueQuery.create()).list()).hasSize(1);

    // The issue on module should stay open and be the same from the first analysis
    Issue reloadIssue = searchIssueByKey(issue.key());
    assertThat(reloadIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloadIssue.status()).isEqualTo("OPEN");
    assertThat(reloadIssue.resolution()).isNull();
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_multi_modules() throws Exception {
    // The custom rule on module is enabled
    ORCHESTRATOR.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-module");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    // One issue by module are created
    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).hasSize(4);

    // Re analysis of the same project
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    // No new issue should be created
    assertThat(search(IssueQuery.create()).list()).hasSize(issues.size());

    // Issues on modules should stay open and be the same from the first analysis
    for (Issue issue : issues) {
      Issue reloadIssue = searchIssueByKey(issue.key());
      assertThat(reloadIssue.status()).isEqualTo("OPEN");
      assertThat(reloadIssue.resolution()).isNull();
      assertThat(reloadIssue.creationDate()).isEqualTo(issue.creationDate());
      assertThat(reloadIssue.updateDate()).isEqualTo(issue.updateDate());
    }
  }

  private Issue getIssueOnLine(final Integer line, final String repoKey, final String ruleKey, List<Issue> issues) {
    return Iterables.find(issues, new Predicate<Issue>() {
      public boolean apply(Issue issue) {
        return Objects.equal(issue.line(), line) &&
          Objects.equal(issue.ruleKey(), repoKey + ":" + ruleKey);
      }
    });
  }

  private List<Issue> searchUnresolvedIssuesByComponent(String componentKey) {
    return search(IssueQuery.create().components(componentKey).resolved(false)).list();
  }

}
