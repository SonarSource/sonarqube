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
package org.sonar.batch.issue;

import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Shared issues among all project modules
 */
public class IssueCache implements BatchComponent {

  // component key -> issue key -> issue
  private final Map<String, Map<String, Issue>> componentIssues = Maps.newHashMap();

  public Collection<Issue> componentIssues(String componentKey) {
    Map<String, Issue> issuesByKey = componentIssues.get(componentKey);
    return issuesByKey == null ? Collections.<Issue>emptyList() : issuesByKey.values();
  }

  public Issue componentIssue(String componentKey, String issueKey) {
    Map<String, Issue> issuesByKey = componentIssues.get(componentKey);
    if (issuesByKey != null) {
      return issuesByKey.get(issueKey);
    }
    return null;
  }

  public IssueCache addOrUpdate(Issue issue) {
    Map<String, Issue> issuesByKey = componentIssues.get(issue.componentKey());
    if (issuesByKey == null) {
      issuesByKey = Maps.newHashMap();
      componentIssues.put(issue.componentKey(), issuesByKey);
    }
    issuesByKey.put(issue.key(), issue);
    return this;
  }
}
