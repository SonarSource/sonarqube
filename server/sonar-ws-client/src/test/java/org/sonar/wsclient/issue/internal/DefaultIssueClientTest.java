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
package org.sonar.wsclient.issue.internal;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.BulkChange;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewIssue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DefaultIssueClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_find_issues() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issues\": [{\"key\": \"ABCDE\"}]}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    IssueQuery query = IssueQuery.create().issues("ABCDE");
    Issues issues = client.find(query);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/search?issues=ABCDE");
    assertThat(issues.list()).hasSize(1);
    assertThat(issues.list().get(0).key()).isEqualTo("ABCDE");
  }

  @Test
  public void should_fail_to_find_issues() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubStatusCode(500);

    IssueClient client = new DefaultIssueClient(requestFactory);
    try {
      client.find(IssueQuery.create());
      fail();
    } catch (HttpException e) {
      assertThat(e.status()).isEqualTo(500);
      assertThat(e.url()).startsWith("http://localhost");
      assertThat(e.url()).endsWith("/api/issues/search");
    }
  }

  @Test
  public void should_set_severity() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.setSeverity("ABCDE", "BLOCKER");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/set_severity");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("severity", "BLOCKER");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_assign() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.assign("ABCDE", "emmerik");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/assign");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("assignee", "emmerik");
    assertThat(result).isNotNull();
  }

  @Test
  public void assign_to_me() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.assignToMe("ABCDE");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/assign");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("me", "true");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_unassign() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.assign("ABCDE", null);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/assign");
    assertThat(httpServer.requestParams()).containsEntry("issue", "ABCDE");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.plan("ABCDE", "DEFGH");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/plan");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("plan", "DEFGH");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_unplan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.plan("ABCDE", null);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/plan");
    assertThat(httpServer.requestParams()).containsEntry("issue", "ABCDE");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_create_issue() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.create(NewIssue.create().component("Action.java").rule("squid:AvoidCycle"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/create");
    assertThat(httpServer.requestParams())
      .containsEntry("component", "Action.java")
      .containsEntry("rule", "squid:AvoidCycle");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_get_transitions() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\n" +
      "  \"transitions\": [\n" +
      "    \"resolve\",\n" +
      "    \"falsepositive\"\n" +
      "  ]\n" +
      "}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    List<String> transitions = client.transitions("ABCDE");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/transitions?issue=ABCDE");
    assertThat(transitions).hasSize(2);
    assertThat(transitions).containsOnly("resolve", "falsepositive");
  }

  @Test
  public void should_apply_transition() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.doTransition("ABCDE", "resolve");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/do_transition");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("transition", "resolve");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_add_comment() throws Exception {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/internal/DefaultIssueClientTest/add_comment_result.json")));

    IssueClient client = new DefaultIssueClient(requestFactory);
    IssueComment comment = client.addComment("ISSUE-1", "this is my comment");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/add_comment");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ISSUE-1")
      .containsEntry("text", "this is my comment");
    assertThat(comment).isNotNull();
    assertThat(comment.key()).isEqualTo("COMMENT-123");
    assertThat(comment.htmlText()).isEqualTo("this is my comment");
    assertThat(comment.login()).isEqualTo("admin");
    assertThat(comment.createdAt().getDate()).isEqualTo(18);
  }

  @Test
  public void should_get_actions() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\n" +
      "  \"actions\": [\n" +
      "    \"link-to-jira\",\n" +
      "    \"tweet\"\n" +
      "  ]\n" +
      "}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    List<String> actions = client.actions("ABCDE");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/actions?issue=ABCDE");
    assertThat(actions).hasSize(2);
    assertThat(actions).containsOnly("link-to-jira", "tweet");
  }

  @Test
  public void should_apply_action() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.doAction("ABCDE", "tweet");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/do_action");
    assertThat(httpServer.requestParams())
      .containsEntry("issue", "ABCDE")
      .containsEntry("actionKey", "tweet");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_do_bulk_change() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody("{\"issuesChanged\": {\"total\": 2}, \"issuesNotChanged\": {\"total\": 1, \"issues\": [\"06ed4db6-fd96-450a-bcb0-e0184db50105\"]} }");

    BulkChangeQuery query = BulkChangeQuery.create()
      .issues("ABCD", "EFGH")
      .actions("assign")
      .actionParameter("assign", "assignee", "geoffrey");

    IssueClient client = new DefaultIssueClient(requestFactory);
    BulkChange result = client.bulkChange(query);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/bulk_change");
    assertThat(httpServer.requestParams())
      .containsEntry("assign.assignee", "geoffrey")
      .containsEntry("issues", "ABCD,EFGH")
      .containsEntry("actions", "assign");
    assertThat(result).isNotNull();
  }
}
