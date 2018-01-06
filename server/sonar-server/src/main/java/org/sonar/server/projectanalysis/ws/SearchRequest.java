/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.projectanalysis.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

class SearchRequest {
  static final int DEFAULT_PAGE_SIZE = 100;
  static final int MAX_SIZE = 500;

  private final String project;
  private final String branch;
  private final EventCategory category;
  private final int page;
  private final int pageSize;
  private final String from;
  private final String to;

  private SearchRequest(Builder builder) {
    this.project = builder.project;
    this.branch= builder.branch;
    this.category = builder.category;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.from = builder.from;
    this.to = builder.to;
  }

  public String getProject() {
    return project;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  @CheckForNull
  public EventCategory getCategory() {
    return category;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  @CheckForNull
  public String getFrom() {
    return from;
  }

  @CheckForNull
  public String getTo() {
    return to;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String project;
    private String branch;
    private EventCategory category;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private String from;
    private String to;

    private Builder() {
      // enforce static factory method
    }

    public Builder setProject(String project) {
      this.project = project;
      return this;
    }

    public Builder setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public Builder setCategory(@Nullable EventCategory category) {
      this.category = category;
      return this;
    }

    public Builder setPage(int page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setFrom(@Nullable String from) {
      this.from = from;
      return this;
    }

    public Builder setTo(@Nullable String to) {
      this.to = to;
      return this;
    }

    public SearchRequest build() {
      requireNonNull(project, "Project is required");
      checkArgument(pageSize <= MAX_SIZE, "Page size must be lower than or equal to " + MAX_SIZE);
      return new SearchRequest(this);
    }
  }
}
