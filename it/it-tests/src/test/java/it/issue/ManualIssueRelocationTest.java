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

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.NewIssue;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

@Category(QaOnly.class)
public class ManualIssueRelocationTest extends AbstractIssueTest {

  private final static String COMPONENT_KEY = "sample:src/main/xoo/sample/Sample.xoo";

  private Date issueCreationDate;

  @Before
  public void before() {
    ORCHESTRATOR.resetData();
    analyzeInitialProject();
    createManualRule();
    createManualIssue();
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_move_manual_issue_if_same_line_found() {
    analyzeModifiedProject("issue/xoo-sample-v2");
    checkManualIssueOpenAt(6);
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_not_touch_issue_if_same_line_not_found() {
    analyzeModifiedProject("issue/xoo-sample-v3");
    checkManualIssueOpenAt(3);
  }

  /**
   * SONAR-3387
   */
  @Test
  @Ignore("DO WE REALLY WANT THAT -> TO BE DISCUSSED WITH PO")
  public void should_not_touch_issue_if_same_line_found_multiple_times() {
    analyzeModifiedProject("issue/xoo-sample-v4");
    checkManualIssueOpenAt(3);
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_close_issue_if_same_line_not_found_and_old_line_out_of_new_source() {
    analyzeModifiedProject("issue/xoo-sample-v5");
    checkManualIssueStatus(null, "CLOSED", "FIXED");
  }

  private void checkManualIssueOpenAt(int line) {
    checkManualIssueStatus(line, "OPEN", null);
  }

  private void checkManualIssueStatus(@Nullable Integer line, String status, String resolution) {
    List<Issue> issues = searchIssuesByComponent(COMPONENT_KEY);
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.ruleKey()).isEqualTo("manual:invalidclassname");
    if (line == null) {
      assertThat(issue.line()).isNull();
    } else {
      assertThat(issue.line()).isEqualTo(line);
    }
    assertThat(issue.severity()).isEqualTo(("MAJOR"));
    assertThat(issue.message()).isEqualTo(("The name 'Sample' is too generic"));
    assertThat(issue.status()).isEqualTo(status);
    if (resolution == null) {
      assertThat(issue.resolution()).isNull();
    } else {
      assertThat(issue.resolution()).isEqualTo(resolution);
    }
    assertThat(issue.creationDate()).isEqualTo(issueCreationDate);
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.reporter()).isEqualTo("admin");
  }

  private void analyzeInitialProject() {
    // no active rules
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");
  }

  private void analyzeModifiedProject(String project) {
    runProjectAnalysis(ORCHESTRATOR, project);
  }

  private void createManualIssue() {
    ORCHESTRATOR.getServer().adminWsClient().issueClient().create(
      NewIssue.create()
        .component(COMPONENT_KEY)
        .line(3)
        .rule("manual:invalidclassname")
        .message("The name 'Sample' is too generic")
      );
    this.issueCreationDate = searchIssuesByComponent(COMPONENT_KEY).get(0).creationDate();
  }

  private static void createManualRule() {
    ORCHESTRATOR.getServer().adminWsClient().post("/api/rules/create", ImmutableMap.<String, Object>of(
      "manual_key", "invalidclassname",
      "name", "InvalidClassName",
      "markdown_description", "Invalid class name"
      ));
  }

  private static List<Issue> searchIssuesByComponent(String componentKey) {
    return search(IssueQuery.create().components(componentKey)).list();
  }
}
