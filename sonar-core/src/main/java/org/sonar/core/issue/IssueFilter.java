/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class IssueFilter implements ServerComponent {

  private final IssueFinder issueFinder;

  public IssueFilter(IssueFinder issueFinder) {
    this.issueFinder = issueFinder;
  }

  public List<Issue> execute(Map<String, String> map) {
    IssueQuery issueQueryBuilder = createIssueQuery(map);
    return issueFinder.find(issueQueryBuilder);
  }

  public Issue execute(String key) {
    return !Strings.isNullOrEmpty(key) ? issueFinder.findByKey(key) : null;
  }

  @VisibleForTesting
  IssueQuery createIssueQuery(Map<String, String> map) {
    IssueQuery.Builder issueQueryBuilder = new IssueQuery.Builder();
    if (map != null && !map.isEmpty()) {
      if (isPropertyNotEmpty("keys", map)) {
        issueQueryBuilder.keys(getListProperties("keys", map));
      }
      if (isPropertyNotEmpty("severities", map)) {
        issueQueryBuilder.severities(getListProperties("severities", map));
      }
      if (isPropertyNotEmpty("minSeverity", map)) {
        issueQueryBuilder.minSeverity(map.get("minSeverity"));
      }
      if (isPropertyNotEmpty("status", map)) {
        issueQueryBuilder.status(getListProperties("status", map));
      }
      if (isPropertyNotEmpty("resolutions", map)) {
        issueQueryBuilder.resolutions(getListProperties("resolutions", map));
      }
      if (isPropertyNotEmpty("components", map)) {
        issueQueryBuilder.componentKeys(getListProperties("components", map));
      }
      if (isPropertyNotEmpty("rules", map)) {
        issueQueryBuilder.rules(getListProperties("rules", map));
      }
      if (isPropertyNotEmpty("userLogins", map)) {
        issueQueryBuilder.userLogins(getListProperties("userLogins", map));
      }
      if (isPropertyNotEmpty("assigneeLogins", map)) {
        issueQueryBuilder.assigneeLogins(getListProperties("assigneeLogins", map));
      }
      if (isPropertyNotEmpty("limit", map)) {
        issueQueryBuilder.limit(Integer.parseInt(map.get("limit")));
      }
    }
    return issueQueryBuilder.build();
  }

  private boolean isPropertyNotEmpty(String property, Map<String, String> map) {
    return map.containsKey(property) && !Strings.isNullOrEmpty(map.get(property));
  }

  private List<String> getListProperties(String property, Map<String, String> map) {
    return newArrayList(Splitter.on(',').split(map.get(property)));
  }
}
