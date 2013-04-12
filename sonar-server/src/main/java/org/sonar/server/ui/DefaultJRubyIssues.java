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
package org.sonar.server.ui;

import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.JRubyIssues;

import java.util.List;
import java.util.Map;

/**
 * Facade of issue components for JRuby on Rails webapp
 *
 * @since 3.6
 */
public class DefaultJRubyIssues implements JRubyIssues {

  private final IssueFinder finder;

  public DefaultJRubyIssues(IssueFinder f) {
    this.finder = f;
    JRubyFacades.setIssues(this);
  }

  public IssueFinder.Results find(Map<String, Object> params) {
    return finder.find(newQuery(params));
  }

  IssueQuery newQuery(Map<String, Object> props) {
    IssueQuery.Builder builder = IssueQuery.builder();
    builder.keys((List<String>) props.get("keys"));
    builder.severities((List<String>) props.get("severities"));
    builder.statuses((List<String>) props.get("statuses"));
    builder.resolutions((List<String>) props.get("resolutions"));
    builder.components((List<String>) props.get("components"));
    builder.userLogins((List<String>) props.get("userLogins"));
    builder.assigneeLogins((List<String>) props.get("assigneeLogins"));
    builder.limit((Integer) props.get("limit"));
    builder.offset((Integer) props.get("offset"));
    return builder.build();
  }
}
