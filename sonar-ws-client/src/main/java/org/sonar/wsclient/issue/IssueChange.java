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
package org.sonar.wsclient.issue;

import javax.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class IssueChange {
  static final String BASE_URL = "/api/issues/change";
  private final Map<String, Object> params = new LinkedHashMap<String, Object>();

  private IssueChange() {
  }

  public static IssueChange create() {
    return new IssueChange();
  }

  Map<String, Object> urlParams() {
    return params;
  }

  public IssueChange severity(String s) {
    params.put("newSeverity", s);
    return this;
  }

  /**
   * Add a comment
   */
  public IssueChange comment(String s) {
    params.put("newComment", s);
    return this;
  }

  public IssueChange cost(Double d) {
    params.put("newCost", d);
    return this;
  }

  public IssueChange resolution(String s) {
    params.put("newResolution", s);
    return this;
  }

  public IssueChange assignee(String login) {
    params.put("newAssignee", login);
    return this;
  }

  public IssueChange description(String s) {
    params.put("newDescription", s);
    return this;
  }

  public IssueChange attribute(String key, @Nullable String value) {
    if (value == null) {
      params.put("newAttr[" + key + "]", "");
    } else {
      params.put("newAttr[" + key + "]", value);
    }
    return this;
  }
}
