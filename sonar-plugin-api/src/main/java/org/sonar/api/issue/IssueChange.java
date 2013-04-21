/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
  private String commentLogin = null;
  private Boolean manualSeverity = null;
  private String description = null;
  private boolean lineChanged = false;
  private Integer line = null;
  private boolean costChanged = false;
  private Double cost = null;
  private String resolution = null;
  private boolean assigneeChanged = false;
  private String assignee = null;
  private String title = null;
  private Map<String, String> attributes = null;

  private IssueChange() {
  }

  public static IssueChange create() {
    return new IssueChange();
  }

  public boolean hasChanges() {
    return severity != null || comment != null || manualSeverity != null || description != null ||
      lineChanged || costChanged || resolution != null || assigneeChanged || attributes != null;
  }

  public IssueChange setSeverity(String s) {
    this.severity = s;
    return this;
  }

  public IssueChange setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public IssueChange setCommentLogin(String s) {
    this.commentLogin = s;
    return this;
  }

  public IssueChange setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public IssueChange setTitle(String s) {
    this.title = s;
    return this;
  }

  public IssueChange setDescription(String s) {
    this.description = s;
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

  public IssueChange setAssignee(@Nullable String assigneeLogin) {
    this.assigneeChanged = true;
    this.assignee = assigneeLogin;
    return this;
  }

  public IssueChange setAttribute(String key, @Nullable String value) {
    if (attributes == null) {
      attributes = new LinkedHashMap<String, String>();
    }
    attributes.put(key, value);
    return this;
  }

  public String severity() {
    return severity;
  }

  public String comment() {
    return comment;
  }

  public String commentLogin() {
    return commentLogin;
  }

  public Boolean manualSeverity() {
    return manualSeverity;
  }

  public String description() {
    return description;
  }

  public String title() {
    return title;
  }

  public Integer line() {
    return line;
  }

  public boolean isLineChanged() {
    return lineChanged;
  }

  public Double cost() {
    return cost;
  }

  public boolean isCostChanged() {
    return costChanged;
  }

  public String resolution() {
    return resolution;
  }

  public String assignee() {
    return assignee;
  }

  public boolean isAssigneeChanged() {
    return assigneeChanged;
  }

  public Map<String, String> attributes() {
    return attributes == null ? Collections.<String, String>emptyMap() : new LinkedHashMap<String, String>(attributes);
  }
}
