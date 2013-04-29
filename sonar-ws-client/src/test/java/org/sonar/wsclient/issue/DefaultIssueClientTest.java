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
    IssueQuery query = IssueQuery.create().keys("ABCDE");
    Issues issues = client.find(query);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/search?keys=ABCDE");
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
  public void should_apply_change() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);

    IssueClient client = new DefaultIssueClient(requestFactory);
    client.change("ABCDE", IssueChange.create().severity("BLOCKER").comment("because!"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/change?newSeverity=BLOCKER&comment=because!&key=ABCDE");
  }

  @Test
  public void should_not_apply_empty_change() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);

    IssueClient client = new DefaultIssueClient(requestFactory);
    client.change("ABCDE", IssueChange.create());

    assertThat(httpServer.requestedPath()).isNull();
  }

  @Test
  public void should_create_issue() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);

    IssueClient client = new DefaultIssueClient(requestFactory);
    client.create(NewIssue.create().component("Action.java").rule("squid:AvoidCycle"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/create?component=Action.java&rule=squid:AvoidCycle");
  }

  @Test
  public void should_get_transitions() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);
    httpServer.doReturnBody("{\n" +
      "  \"transitions\": [\n" +
      "    \"close\",\n" +
      "    \"resolve\",\n" +
      "    \"falsepositive\"\n" +
      "  ]\n" +
      "}");

    IssueClient client = new DefaultIssueClient(requestFactory);
    List<String> transitions = client.transitions("ABCDE");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/transitions?issue=ABCDE");
    assertThat(transitions).hasSize(3);
    assertThat(transitions).containsOnly("close", "resolve", "falsepositive");
  }

  @Test
  public void should_apply_transition() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url(), null, null);

    IssueClient client = new DefaultIssueClient(requestFactory);
    client.transition("ABCDE", IssueTransition.create().transition("resolve"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/issues/do_transition?transition=resolve&issue=ABCDE");
  }
}
