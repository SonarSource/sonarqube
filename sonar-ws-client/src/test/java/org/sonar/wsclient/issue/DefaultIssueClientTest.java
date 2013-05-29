/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.wsclient.issue;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DefaultIssueClientTest {
  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_find_issues() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issues\": [{\"key\": \"ABCDE\"}]}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    IssueQuery query = IssueQuery.create().issues("ABCDE");
    Issues issues = client.find(query);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/search?issues=ABCDE");
    assertThat(issues.list()).hasSize(1);
    assertThat(issues.list().get(0).key()).isEqualTo("ABCDE");
  }

  @Test
  public void should_fail_to_find_issues() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnStatus(500);

    IssueClient client = new DefaultIssueClient(requestFactory);
    try {
      client.find(IssueQuery.create());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to search for issues. Bad HTTP response status: 500");
    }
  }

  @Test
  public void should_set_severity() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.setSeverity("ABCDE", "BLOCKER");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/set_severity?issue=ABCDE&severity=BLOCKER");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_assign() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.assign("ABCDE", "emmerik");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/assign?issue=ABCDE&assignee=emmerik");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_unassign() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.assign("ABCDE", null);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/assign?issue=ABCDE");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_plan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.plan("ABCDE", "DEFGH");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/plan?issue=ABCDE&plan=DEFGH");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_unplan() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.plan("ABCDE", null);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/plan?issue=ABCDE");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_create_issue() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.create(NewIssue.create().component("Action.java").rule("squid:AvoidCycle"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/create?component=Action.java&rule=squid:AvoidCycle");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_get_transitions() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\n" +
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
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\"issue\": {\"key\": \"ABCDE\"}}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    Issue result = client.doTransition("ABCDE", "resolve");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/do_transition?issue=ABCDE&transition=resolve");
    assertThat(result).isNotNull();
  }

  @Test
  public void should_add_comment() throws Exception {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody(IOUtils.toString(getClass().getResourceAsStream("/org/sonar/wsclient/issue/DefaultIssueClientTest/add_comment_result.json")));

    IssueClient client = new DefaultIssueClient(requestFactory);
    IssueComment comment = client.addComment("ISSUE-1", "this is my comment");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/add_comment?issue=ISSUE-1&text=this%20is%20my%20comment");
    assertThat(comment).isNotNull();
    assertThat(comment.key()).isEqualTo("COMMENT-123");
    assertThat(comment.htmlText()).isEqualTo("this is my comment");
    assertThat(comment.login()).isEqualTo("admin");
    assertThat(comment.createdAt().getDate()).isEqualTo(18);
  }
}
