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

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.issue.*;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#issueClient()}.
 */
public class DefaultIssueClient implements IssueClient {

  private static final String SEARCH_URL = "/api/issues/search";
  private static final String ASSIGN_URL = "/api/issues/assign";

  private final HttpRequestFactory requestFactory;
  private final IssueJsonParser parser;

  public DefaultIssueClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    this.parser = new IssueJsonParser();
  }

  @Override
  public Issues find(IssueQuery query) {
    String json = requestFactory.get(SEARCH_URL, query.urlParams());
    return parser.parseIssues(json);
  }

  @Override
  public Issue create(NewIssue newIssue) {
    String json = requestFactory.post("/api/issues/create", newIssue.urlParams());
    return jsonToIssue(json);
  }

  @Override
  public Issue setSeverity(String issueKey, String severity) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "severity", severity);
    String json = requestFactory.post("/api/issues/set_severity", params);
    return jsonToIssue(json);
  }

  @Override
  public Issue assign(String issueKey, @Nullable String assignee) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "assignee", assignee);
    String json = requestFactory.post(ASSIGN_URL, params);
    return jsonToIssue(json);
  }

  @Override
  public Issue assignToMe(String issueKey) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "me", "true");
    String json = requestFactory.post(ASSIGN_URL, params);
    return jsonToIssue(json);
  }

  @Override
  public Issue plan(String issueKey, @Nullable String actionPlanKey) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "plan", actionPlanKey);
    String json = requestFactory.post("/api/issues/plan", params);
    return jsonToIssue(json);
  }

  @Override
  public IssueComment addComment(String issueKey, String markdownText) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "text", markdownText);
    String json = requestFactory.post("/api/issues/add_comment", params);
    Map rootJson = (Map) JSONValue.parse(json);
    return new DefaultIssueComment((Map) rootJson.get("comment"));
  }

  @Override
  public List<String> transitions(String issueKey) {
    Map<String, Object> queryParams = EncodingUtils.toMap("issue", issueKey);
    String json = requestFactory.get("/api/issues/transitions", queryParams);
    return parser.parseTransitions(json);
  }

  @Override
  public Issue doTransition(String issueKey, String transition) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "transition", transition);
    String json = requestFactory.post("/api/issues/do_transition", params);
    return jsonToIssue(json);
  }

  @Override
  public List<String> actions(String issueKey) {
    Map<String, Object> queryParams = EncodingUtils.toMap("issue", issueKey);
    String json = requestFactory.get("/api/issues/actions", queryParams);
    return parser.parseActions(json);
  }

  @Override
  public Issue doAction(String issueKey, String action) {
    Map<String, Object> params = EncodingUtils.toMap("issue", issueKey, "actionKey", action);
    String json = requestFactory.post("/api/issues/do_action", params);
    return jsonToIssue(json);
  }

  @Override
  public BulkChange bulkChange(BulkChangeQuery query) {
    String json = requestFactory.post("/api/issues/bulk_change", query.urlParams());
    return parser.parseBulkChange(json);
  }

  @Override
  public List<IssueChange> changes(String issueKey) {
    Map<String, Object> queryParams = EncodingUtils.toMap("issue", issueKey);
    String json = requestFactory.post("/api/issues/changelog", queryParams);
    return parser.parseChangelog(json);
  }

  private Issue jsonToIssue(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultIssue((Map) jsonRoot.get("issue"));
  }

}
