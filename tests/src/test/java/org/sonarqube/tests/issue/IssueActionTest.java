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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.*;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;
import util.issue.IssueRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonarqube.ws.Common.Severity.BLOCKER;
import static util.ItUtils.toDatetime;

public class IssueActionTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  @ClassRule
  public static final IssueRule issueRule = IssueRule.from(ORCHESTRATOR);

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);

  private ProjectAnalysis projectAnalysis;
  private IssuesService issuesService;

  private Issue randomIssue;

  @Before
  public void setup() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");

    this.projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey).withQualityProfile(qualityProfileKey);
    this.projectAnalysis.run();
    this.issuesService = tester.wsClient().issues();
    this.randomIssue = issueRule.getRandomIssue();
  }

  @Test
  public void no_comments_by_default() {
    assertThat(randomIssue.getComments().getCommentsList()).isEmpty();
  }

  @Test
  public void add_comment() {
    Issues.Comment comment = issuesService.addComment(new AddCommentRequest().setIssue(randomIssue.getKey()).setText("this is my *comment*")).getIssue().getComments().getComments(0);
    assertThat(comment.getKey()).isNotNull();
    assertThat(comment.getHtmlText()).isEqualTo("this is my <strong>comment</strong>");
    assertThat(comment.getLogin()).isEqualTo("admin");
    assertThat(comment.getCreatedAt()).isNotNull();

    // reload issue
    Issue reloaded = issueRule.getByKey(randomIssue.getKey());
    assertThat(reloaded.getComments().getCommentsList()).hasSize(1);
    assertThat(reloaded.getComments().getComments(0).getKey()).isEqualTo(comment.getKey());
    assertThat(reloaded.getComments().getComments(0).getHtmlText()).isEqualTo("this is my <strong>comment</strong>");
    assertThat(toDatetime(reloaded.getUpdateDate())).isAfter(toDatetime(randomIssue.getUpdateDate()));
  }

  /**
   * SONAR-4450
   */
  @Test
  public void should_reject_blank_comment() {
    try {
      issuesService.addComment(new AddCommentRequest().setIssue(randomIssue.getKey()).setText("  "));
      fail();
    } catch (org.sonarqube.ws.client.HttpException ex) {
      assertThat(ex.code()).isEqualTo(400);
    }

    Issue reloaded = issueRule.getByKey(randomIssue.getKey());
    assertThat(reloaded.getComments().getCommentsList()).isEmpty();
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
    issuesService.setSeverity(new SetSeverityRequest().setIssue(randomIssue.getKey()).setSeverity("BLOCKER"));

    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = issueRule.getByKey(randomIssue.getKey());
    assertThat(reloaded.getSeverity()).isEqualTo(BLOCKER);
    assertThat(reloaded.getStatus()).isEqualTo("OPEN");
    assertThat(reloaded.hasResolution()).isFalse();
    assertThat(reloaded.getCreationDate()).isEqualTo(randomIssue.getCreationDate());
    assertThat(toDatetime(reloaded.getCreationDate())).isBefore(toDatetime(reloaded.getUpdateDate()));
  }

  /**
   * SONAR-4287
   */
  @Test
  public void assign() {
    assertThat(randomIssue.hasAssignee()).isFalse();
    Issues.SearchWsResponse response = issueRule.search(new SearchRequest().setIssues(singletonList(randomIssue.getKey())));
    assertThat(response.getUsers().getUsersList()).isEmpty();

    issuesService.assign(new AssignRequest().setIssue(randomIssue.getKey()).setAssignee("admin"));
    assertThat(issueRule.search(new SearchRequest().setAssignees(singletonList("admin"))).getIssuesList()).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = issueRule.getByKey(randomIssue.getKey());
    assertThat(reloaded.getAssignee()).isEqualTo("admin");
    assertThat(reloaded.getCreationDate()).isEqualTo(randomIssue.getCreationDate());

    response = issueRule.search(new SearchRequest().setIssues(singletonList(randomIssue.getKey())).setAdditionalFields(singletonList("users")));
    assertThat(response.getUsers().getUsersList().stream().filter(user -> "admin".equals(user.getLogin())).findFirst()).isPresent();
    assertThat(response.getUsers().getUsersList().stream().filter(user -> "Administrator".equals(user.getName())).findFirst()).isPresent();

    // unassign
    issuesService.assign(new AssignRequest().setIssue(randomIssue.getKey()).setAssignee(null));
    reloaded = issueRule.getByKey(randomIssue.getKey());
    assertThat(reloaded.hasAssignee()).isFalse();
    assertThat(issueRule.search(new SearchRequest().setAssignees(singletonList("admin"))).getIssuesList()).isEmpty();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void fail_assign_if_assignee_does_not_exist() {
    assertThat(randomIssue.hasAssignee()).isFalse();
    try {
      issuesService.assign(new AssignRequest().setIssue(randomIssue.getKey()).setAssignee("unknown"));
      fail();
    } catch (org.sonarqube.ws.client.HttpException ex) {
      assertThat(ex.code()).isEqualTo(404);
    }
  }

  private static List<Issue> searchIssuesBySeverities(String projectKey, String severity) {
    return issueRule.search(new SearchRequest().setProjects(singletonList(projectKey)).setSeverities(singletonList(severity))).getIssuesList();
  }

}
