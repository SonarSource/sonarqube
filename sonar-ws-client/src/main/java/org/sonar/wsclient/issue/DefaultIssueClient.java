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

import com.github.kevinsawicki.http.HttpRequest;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#issueClient()}.
 */
public class DefaultIssueClient implements IssueClient {

  private final HttpRequestFactory requestFactory;
  private final IssueParser parser;

  /**
   * For internal use. Use {@link org.sonar.wsclient.SonarClient} to get an instance.
   */
  public DefaultIssueClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    this.parser = new IssueParser();
  }

  public Issues find(IssueQuery query) {
    HttpRequest request = requestFactory.get(IssueQuery.BASE_URL, query.urlParams());
    if (!request.ok()) {
      throw new IllegalStateException("Fail to search for issues. Bad HTTP response status: " + request.code());
    }
    String json = request.body("UTF-8");
    return parser.parseIssues(json);
  }

  @Override
  public void create(NewIssue newIssue) {
    HttpRequest request = requestFactory.post(NewIssue.BASE_URL, newIssue.urlParams());
    if (!request.ok()) {
      throw new IllegalStateException("Fail to create issue. Bad HTTP response status: " + request.code());
    }
  }

  @Override
  public void setSeverity(String issueKey, String severity) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "severity", severity);
    HttpRequest request = requestFactory.post("/api/issues/set_severity", params);
    if (!request.ok()) {
      throw new IllegalStateException("Fail to set severity. Bad HTTP response status: " + request.code());
    }
  }

  @Override
  public void assign(String issueKey, @Nullable String assignee) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "assignee", assignee);
    HttpRequest request = requestFactory.post("/api/issues/assign", params);
    if (!request.ok()) {
      throw new IllegalStateException("Fail to assign issue to user. Bad HTTP response status: " + request.code());
    }
  }

  @Override
  public List<String> transitions(String issueKey) {
    Map<String, Object> queryParams = EncodingUtils.toMap("issue", issueKey);
    HttpRequest request = requestFactory.get("/api/issues/transitions", queryParams);
    if (!request.ok()) {
      throw new IllegalStateException("Fail to return transition for issue. Bad HTTP response status: " + request.code());
    }
    String json = request.body("UTF-8");
    return parser.parseTransitions(json);
  }

  @Override
  public void doTransition(String issueKey, String transition) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "transition", transition);
    HttpRequest request = requestFactory.post("/api/issues/do_transition", params);
    if (!request.ok()) {
      throw new IllegalStateException("Fail to execute transition on issue " + issueKey + ".Bad HTTP response status: " + request.code());
    }
  }


}
