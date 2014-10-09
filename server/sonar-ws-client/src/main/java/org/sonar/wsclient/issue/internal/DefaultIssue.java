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

import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.CheckForNull;

import java.util.*;

/**
 * @since 3.6
 */
public class DefaultIssue implements Issue {

  private final Map json;

  DefaultIssue(Map json) {
    this.json = json;
  }

  /**
   * Unique key
   */
  @Override
  public String key() {
    return JsonUtils.getString(json, "key");
  }

  @Override
  public Long componentId() {
    return JsonUtils.getLong(json, "componentId");
  }

  @Override
  public String componentKey() {
    return JsonUtils.getString(json, "component");
  }

  @Override
  public String projectKey() {
    return JsonUtils.getString(json, "project");
  }

  @Override
  public String ruleKey() {
    return JsonUtils.getString(json, "rule");
  }

  @Override
  public String severity() {
    return JsonUtils.getString(json, "severity");
  }

  @Override
  @CheckForNull
  public String message() {
    return JsonUtils.getString(json, "message");
  }

  @Override
  @CheckForNull
  public Integer line() {
    return JsonUtils.getInteger(json, "line");
  }

  @Override
  @CheckForNull
  public String debt() {
    return JsonUtils.getString(json, "debt");
  }

  @Override
  public String status() {
    return JsonUtils.getString(json, "status");
  }

  /**
   * The resolution type. Null if the issue is not resolved.
   */
  @Override
  @CheckForNull
  public String resolution() {
    return JsonUtils.getString(json, "resolution");
  }

  @Override
  @CheckForNull
  public String reporter() {
    return JsonUtils.getString(json, "reporter");
  }

  /**
   * Login of assignee. Null if issue is not assigned.
   */
  @Override
  @CheckForNull
  public String assignee() {
    return JsonUtils.getString(json, "assignee");
  }

  /**
   * SCM account
   */
  @Override
  @CheckForNull
  public String author() {
    return JsonUtils.getString(json, "author");
  }

  @Override
  @CheckForNull
  public String actionPlan() {
    return JsonUtils.getString(json, "actionPlan");
  }

  @Override
  public Date creationDate() {
    return JsonUtils.getDateTime(json, "creationDate");
  }

  @Override
  public Date updateDate() {
    return JsonUtils.getDateTime(json, "updateDate");
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return JsonUtils.getDateTime(json, "closeDate");
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes().get(key);
  }

  @Override
  public Map<String, String> attributes() {
    Map<String, String> attr = (Map<String, String>) json.get("attr");
    if (attr == null) {
      return Collections.emptyMap();
    }
    return attr;
  }

  /**
   * Non-null list of comments
   */
  @Override
  public List<IssueComment> comments() {
    List<IssueComment> comments = new ArrayList<IssueComment>();
    List<Map> jsonComments = (List<Map>) json.get("comments");
    if (jsonComments != null) {
      for (Map jsonComment : jsonComments) {
        comments.add(new DefaultIssueComment(jsonComment));
      }
    }
    return comments;
  }
}
