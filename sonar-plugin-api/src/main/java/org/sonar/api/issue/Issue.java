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

import java.util.Date;
import java.util.UUID;

/**
 * @since 3.6
 */
public class Issue {

  public static final String STATUS_REOPENED = "REOPENED";
  public static final String STATUS_RESOLVED = "RESOLVED";
  public static final String STATUS_CLOSED = "CLOSED";

  public static final String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";
  public static final String RESOLUTION_FIXED = "FIXED";

  public static final String SEVERITY_INFO = "INFO";
  public static final String SEVERITY_MINOR = "MINOR";
  public static final String SEVERITY_MAJOR = "MAJOR";
  public static final String SEVERITY_CRITICAL = "CRITICAL";
  public static final String SEVERITY_BLOCKER = "BLOCKER";

  private String uuid;
  private String componentKey;
  private String ruleKey;
  private String ruleRepositoryKey;
  private String severity;
  private String message;
  private Integer line;
  private Double cost;
  private String status;
  private String resolution;
  private String userLogin;
  private String assigneeLogin;
  private Date createdAt;
  private Date updatedAt;
  private Date closedAt;

  private Issue(Builder builder) {
    this.uuid = builder.uuid;
    this.componentKey = builder.componentKey;
    this.ruleKey = builder.ruleKey;
    this.ruleRepositoryKey = builder.ruleRepositoryKey;
    this.severity = builder.severity;
    this.message = builder.message;
    this.line = builder.line;
    this.cost = builder.cost;
    this.status = builder.status;
    this.resolution = builder.resolution;
    this.userLogin = builder.userLogin;
    this.assigneeLogin = builder.assigneeLogin;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
    this.closedAt = builder.closedAt;
  }

  public String uuid() {
    return uuid;
  }

  public String componentKey() {
    return componentKey;
  }

  public String ruleKey() {
    return ruleKey;
  }

  public String ruleRepositoryKey() {
    return ruleRepositoryKey;
  }

  public String severity() {
    return severity;
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

  public String status() {
    return status;
  }

  public String resolution() {
    return resolution;
  }

  public String userLogin() {
    return userLogin;
  }

  public String assigneeLogin() {
    return assigneeLogin;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public Date closedAt() {
    return closedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Issue issue = (Issue) o;
    return !(uuid != null ? !uuid.equals(issue.uuid()) : issue.uuid() != null);
  }

  @Override
  public int hashCode() {
    return uuid != null ? uuid.hashCode() : 0;
  }

  /**
   * @since 3.6
   */
  public static class Builder {
    private String uuid;
    private String componentKey;
    private String ruleKey;
    private String ruleRepositoryKey;
    private String severity;
    private String message;
    private Integer line;
    private Double cost;
    private String status;
    private String resolution;
    private String userLogin;
    private String assigneeLogin;
    private Date createdAt;
    private Date updatedAt;
    private Date closedAt;

    public Builder() {
      uuid = UUID.randomUUID().toString();
      createdAt = new Date();
    }

    public Builder(Issue issue) {
      this.uuid = issue.uuid();
      this.componentKey = issue.componentKey();
      this.ruleKey = issue.ruleKey();
      this.ruleRepositoryKey = issue.ruleRepositoryKey();
      this.severity = issue.severity();
      this.message = issue.message();
      this.line = issue.line();
      this.cost = issue.cost();
      this.status = issue.status();
      this.resolution = issue.resolution();
      this.createdAt = issue.createdAt();
      this.updatedAt = issue.updatedAt();
      this.closedAt = issue.closedAt();
    }

    public Builder componentKey(String componentKey) {
      this.componentKey = componentKey;
      return this;
    }

    public Builder ruleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public Builder ruleRepositoryKey(String ruleRepositoryKey) {
      this.ruleRepositoryKey = ruleRepositoryKey;
      return this;
    }

    public Builder severity(String severity) {
      this.severity = severity;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder line(Integer line) {
      this.line = line;
      return this;
    }

    public Builder cost(Double cost) {
      this.cost = cost;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder resolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder userLogin(String userLogin) {
      this.userLogin = userLogin;
      return this;
    }

    public Builder assigneeLogin(String assigneeLogin) {
      this.assigneeLogin = assigneeLogin;
      return this;
    }

    public Issue build() {
      return new Issue(this);
    }
  }
}
