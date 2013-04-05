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

  public final static String STATUS_REOPENED = "REOPENED";
  public final static String STATUS_RESOLVED = "RESOLVED";
  public final static String STATUS_CLOSED = "CLOSED";

  public final static String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";
  public final static String RESOLUTION_FIXED = "FIXED";

  public final static String SEVERITY_INFO = "INFO";
  public final static String SEVERITY_MINOR = "MINOR";
  public final static String SEVERITY_MAJOR = "MAJOR";
  public final static String SEVERITY_CRITICAL = "CRITICAL";
  public final static String SEVERITY_BLOCKER = "BLOCKER";

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
  private Date createdAt;

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
    this.createdAt = builder.createdAt;
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

  public Date createdAt() {
    return createdAt;
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
    private Date createdAt;

    public Builder() {
      uuid = UUID.randomUUID().toString();
      createdAt = new Date();
    }

    public Builder(Issue issue) {
      uuid = issue.uuid();
      createdAt = issue.createdAt();
      componentKey(issue.componentKey());
      ruleKey(issue.ruleKey());
      ruleRepositoryKey(issue.ruleRepositoryKey());
      severity(issue.severity());
      message(issue.message());
      line(issue.line());
      cost(issue.cost());
      status(issue.status());
      resolution(issue.resolution());
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

    public Issue build() {
      return new Issue(this);
    }
  }
}
