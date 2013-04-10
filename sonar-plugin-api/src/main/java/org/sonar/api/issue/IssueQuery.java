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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class IssueQuery {

  private List<String> keys;
  private List<String> severities;
  private String minSeverity;
  private List<String> status;
  private List<String> resolutions;
  private List<String> componentKeys;
  private List<String> rules;
  private List<String> userLogins;
  private List<String> assigneeLogins;
  private Integer limit;

  private IssueQuery(Builder builder) {
    this.keys = builder.keys;
    this.severities = builder.severities;
    this.minSeverity = builder.minSeverity;
    this.status = builder.status;
    this.resolutions = builder.resolutions;
    this.componentKeys = builder.componentKeys;
    this.rules = builder.rules;
    this.userLogins = builder.userLogins;
    this.assigneeLogins = builder.assigneeLogins;
    this.limit = builder.limit;
  }

  public List<String> keys() {
    return keys;
  }

  public List<String> severities() {
    return severities;
  }

  public String minSeverity() {
    return minSeverity;
  }

  public List<String> status() {
    return status;
  }

  public List<String> resolutions() {
    return resolutions;
  }

  public List<String> componentKeys() {
    return componentKeys;
  }

  public List<String> rules() {
    return rules;
  }

  public List<String> userLogins() {
    return userLogins;
  }

  public List<String> assigneeLogins() {
    return assigneeLogins;
  }

  public Integer limit() {
    return limit;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  /**
   * @since 3.6
   */
  public static class Builder {
    private List<String> keys;
    private List<String> severities;
    private String minSeverity;
    private List<String> status;
    private List<String> resolutions;
    private List<String> componentKeys;
    private List<String> rules;
    private List<String> userLogins;
    private List<String> assigneeLogins;
    private Integer limit;

    public Builder() {
      componentKeys = newArrayList();
    }

    public Builder keys(List<String> keys) {
      this.keys = keys;
      return this;
    }

    public Builder severities(List<String> severities) {
      this.severities = severities;
      return this;
    }

    public Builder minSeverity(String minSeverity) {
      this.minSeverity = minSeverity;
      return this;
    }

    public Builder status(List<String> status) {
      this.status = status;
      return this;
    }

    public Builder resolutions(List<String> resolutions) {
      this.resolutions = resolutions;
      return this;
    }

    public Builder componentKeys(List<String> componentKeys) {
      this.componentKeys = componentKeys;
      return this;
    }

    public Builder rules(List<String> rules) {
      this.rules = rules;
      return this;
    }

    public Builder userLogins(List<String> userLogins) {
      this.userLogins = userLogins;
      return this;
    }

    public Builder assigneeLogins(List<String> assigneeLogins) {
      this.assigneeLogins = assigneeLogins;
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public IssueQuery build() {
      return new IssueQuery(this);
    }

    @Override
    public String toString() {
      return ReflectionToStringBuilder.toString(this);
    }
  }
}
