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
package org.sonar.api.issue;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class IssueChange {
  private String severity = null;
  private String comment = null;
  private Boolean manualSeverity = null;
  private String message = null;
  private boolean lineChanged = false;
  private Integer line = null;
  private boolean costChanged = false;
  private Double cost = null;
  private String resolution = null;
  private boolean assigneeLoginChanged = false;
  private String assigneeLogin = null;
  private Map<String, String> attributes = null;

  private IssueChange() {
  }

  public static IssueChange create() {
    return new IssueChange();
  }

  public boolean hasChanges() {
    return severity != null || comment != null || manualSeverity != null || message != null ||
      lineChanged || costChanged || resolution != null || assigneeLoginChanged || attributes != null;
  }

  public IssueChange setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public IssueChange setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public IssueChange setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public IssueChange setMessage(String message) {
    this.message = message;
    return this;
  }

  public IssueChange setLine(@Nullable Integer line) {
    this.lineChanged = true;
    this.line = line;
    return this;
  }

  public IssueChange setCost(@Nullable Double cost) {
    this.costChanged = true;
    this.cost = cost;
    return this;
  }

  public IssueChange setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public IssueChange setAssigneeLogin(@Nullable String assigneeLogin) {
    this.assigneeLoginChanged = true;
    this.assigneeLogin = assigneeLogin;
    return this;
  }

  public IssueChange setAttribute(String key, @Nullable String value) {
    if (attributes == null && value != null) {
      attributes = new LinkedHashMap<String, String>();
    }
    if (value != null) {
      attributes.put(key, value);
    } else if (attributes != null) {
      attributes.remove(key);
    }
    return this;
  }

  public String severity() {
    return severity;
  }

  public String comment() {
    return comment;
  }

  public Boolean manualSeverity() {
    return manualSeverity;
  }

  public String message() {
    return message;
  }

  public Integer line() {
    return line;
  }

  public Double cost() {
    return cost;
  }

  public String resolution() {
    return resolution;
  }

  public String assigneeLogin() {
    return assigneeLogin;
  }

  public Map<String, String> attributes() {
    return attributes == null ? Collections.<String, String>emptyMap() : new LinkedHashMap<String, String>(attributes);
  }
}
