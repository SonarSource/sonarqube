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

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.verifyHttpException;

@Category(QaOnly.class)
public class IssueActionTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  Issue issue;
  ProjectAnalysis projectAnalysis;

  @Before
  public void setup() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");

    this.projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey).withQualityProfile(qualityProfileKey);
    this.projectAnalysis.run();
    this.issue = searchRandomIssue();
  }

  @Test
  public void no_comments_by_default() throws Exception {
    assertThat(issue.comments()).isEmpty();
  }

  @Test
  public void add_comment() throws Exception {
    IssueComment comment = adminIssueClient().addComment(issue.key(), "this is my *comment*");
    assertThat(comment.key()).isNotNull();
    assertThat(comment.htmlText()).isEqualTo("this is my <strong>comment</strong>");
    assertThat(comment.login()).isEqualTo("admin");
    assertThat(comment.createdAt()).isNotNull();

    // reload issue
    Issue reloaded = searchIssue(issue.key(), true);

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).key()).isEqualTo(comment.key());
    assertThat(reloaded.comments().get(0).htmlText()).isEqualTo("this is my <strong>comment</strong>");
    assertThat(reloaded.updateDate().before(issue.creationDate())).isFalse();
  }

  /**
   * SONAR-4450
   */
  @Test
  public void should_reject_blank_comment() throws Exception {
    try {
      adminIssueClient().addComment(issue.key(), "  ");
      fail();
    } catch (HttpException ex) {
      assertThat(ex.status()).isEqualTo(400);
    }

    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.comments()).hasSize(0);
  }

  /**
   * SONAR-4352
   */
  @Test
  public void change_severity() {
    String componentKey = "sample";

    // there are no blocker issues
    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).isEmpty();

    // increase the severity of an issue
    adminIssueClient().setSeverity(issue.key(), "BLOCKER");

    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.severity()).isEqualTo("BLOCKER");
    assertThat(reloaded.status()).isEqualTo("OPEN");
    assertThat(reloaded.resolution()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloaded.creationDate().before(reloaded.updateDate())).isTrue();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void assign() {
    assertThat(issue.assignee()).isNull();
    Issues issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.users()).isEmpty();

    adminIssueClient().assign(issue.key(), "admin");
    Assertions.assertThat(searchIssues(IssueQuery.create().assignees("admin"))).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isEqualTo("admin");
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());

    issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.user("admin")).isNotNull();
    assertThat(issues.user("admin").name()).isEqualTo("Administrator");

    // unassign
    adminIssueClient().assign(issue.key(), null);
    reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isNull();
    Assertions.assertThat(searchIssues(IssueQuery.create().assignees("admin"))).isEmpty();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void fail_assign_if_assignee_does_not_exist() {
    assertThat(issue.assignee()).isNull();
    try {
      adminIssueClient().assign(issue.key(), "unknown");
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  private static List<Issue> searchIssuesBySeverities(String componentKey, String... severities) {
    return searchIssues(IssueQuery.create().componentRoots(componentKey).severities(severities));
  }

}
