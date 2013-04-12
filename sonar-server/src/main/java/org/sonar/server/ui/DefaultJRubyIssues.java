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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
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
    builder.keys(toStringList(props.get("keys")));
    builder.severities(toStringList(props.get("severities")));
    builder.statuses(toStringList(props.get("statuses")));
    builder.resolutions(toStringList(props.get("resolutions")));
    builder.components(toStringList(props.get("components")));
    builder.userLogins(toStringList(props.get("userLogins")));
    builder.assigneeLogins(toStringList(props.get("assigneeLogins")));
    builder.limit(toInteger(props.get("limit")));
    builder.offset(toInteger(props.get("offset")));
    return builder.build();
  }

  List<String> toStringList(Object o) {
    List<String> result = null;
    if (o != null) {
      if (o instanceof List) {
        // assume that it contains only strings
        result = (List) o;
      } else if (o instanceof CharSequence) {
        result = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split((CharSequence) o));
      }
    }
    return result;
  }

  Integer toInteger(Object o) {
    if (o instanceof Integer) {
      return (Integer) o;
    }
    if (o instanceof String) {
      return Integer.parseInt((String) o);
    }
    return null;
  }

  public void start() {
    // used to force pico to instantiate the singleton at startup
  }
}
