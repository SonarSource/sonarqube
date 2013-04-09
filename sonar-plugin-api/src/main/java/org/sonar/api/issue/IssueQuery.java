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

  private String severity;
  private String status;
  private String resolution;
  private List<String> componentKeys;
  private String userLogin;
  private String assigneeLogin;

  private IssueQuery(Builder builder) {
    this.severity = builder.severity;
    this.status = builder.status;
    this.resolution = builder.resolution;
    this.componentKeys = builder.componentKeys;
    this.userLogin = builder.userLogin;
    this.assigneeLogin = builder.assigneeLogin;
  }

  public String severity() {
    return severity;
  }

  public String status() {
    return status;
  }

  public String resolution() {
    return resolution;
  }

  public List<String> componentKeys() {
    return componentKeys;
  }

  public String userLogin() {
    return userLogin;
  }

  public String assigneeLogin() {
    return assigneeLogin;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  /**
   * @since 3.6
   */
  public static class Builder {
    private String severity;
    private String status;
    private String resolution;
    private List<String> componentKeys;
    private String userLogin;
    private String assigneeLogin;

    public Builder() {
      componentKeys = newArrayList();
    }

    public Builder severity(String severity) {
      this.severity = severity;
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

    public Builder componentKeys(List<String> componentKeys) {
      this.componentKeys = componentKeys;
      return this;
    }

    public Builder componentKeys(String... componentKeys) {
      this.componentKeys.addAll(newArrayList(componentKeys));
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

    public IssueQuery build() {
      return new IssueQuery(this);
    }
  }
}
