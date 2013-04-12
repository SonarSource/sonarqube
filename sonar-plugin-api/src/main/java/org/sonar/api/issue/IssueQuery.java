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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.List;

/**
 * @since 3.6
 */
public class IssueQuery {

  private final List<String> keys;
  private final List<String> severities;
  private final List<String> statuses;
  private final List<String> resolutions;
  private final List<String> components;
  private final List<String> userLogins;
  private final List<String> assigneeLogins;
  private final int limit, offset;

  private IssueQuery(Builder builder) {
    this.keys = builder.keys;
    this.severities = builder.severities;
    this.statuses = builder.statuses;
    this.resolutions = builder.resolutions;
    this.components = builder.components;
    this.userLogins = builder.userLogins;
    this.assigneeLogins = builder.assigneeLogins;
    this.limit = builder.limit;
    this.offset = builder.offset;
  }

  public List<String> keys() {
    return keys;
  }

  public List<String> severities() {
    return severities;
  }

  public List<String> statuses() {
    return statuses;
  }

  public List<String> resolutions() {
    return resolutions;
  }

  public List<String> components() {
    return components;
  }

  public List<String> userLogins() {
    return userLogins;
  }

  public List<String> assigneeLogins() {
    return assigneeLogins;
  }

  public int limit() {
    return limit;
  }

  public int offset() {
    return offset;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Builder builder() {
    return new Builder();
  }


  /**
   * @since 3.6
   */
  public static class Builder {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_OFFSET = 0;

    private List<String> keys;
    private List<String> severities;
    private List<String> statuses;
    private List<String> resolutions;
    private List<String> components;
    private List<String> userLogins;
    private List<String> assigneeLogins;
    private int limit = DEFAULT_LIMIT;
    private int offset = DEFAULT_OFFSET;

    private Builder() {
    }

    public Builder keys(List<String> l) {
      this.keys = l;
      return this;
    }

    public Builder severities(List<String> l) {
      this.severities = l;
      return this;
    }

    public Builder statuses(List<String> l) {
      this.statuses = l;
      return this;
    }

    public Builder resolutions(List<String> l) {
      this.resolutions = l;
      return this;
    }

    public Builder components(List<String> l) {
      this.components = l;
      return this;
    }

    public Builder userLogins(List<String> l) {
      this.userLogins = l;
      return this;
    }

    public Builder assigneeLogins(List<String> l) {
      this.assigneeLogins = l;
      return this;
    }

    public Builder limit(Integer i) {
      Preconditions.checkArgument(i == null || i.intValue() > 0, "Limit must be greater than 0 (got " + i + ")");
      Preconditions.checkArgument(i == null || i.intValue() < MAX_LIMIT, "Limit must be less than " + MAX_LIMIT + " (got " + i + ")");
      this.limit = (i == null ? DEFAULT_LIMIT : i.intValue());
      return this;
    }

    public Builder offset(Integer i) {
      Preconditions.checkArgument(i == null || i.intValue() >= 0, "Offset must be greater than or equal to 0 (got " + i + ")");
      this.offset = (i == null ? DEFAULT_OFFSET : i.intValue());
      return this;
    }

    public IssueQuery build() {
      return new IssueQuery(this);
    }
  }
}
