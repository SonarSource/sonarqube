/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.project;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchMyProjectsRequest {
  private final String query;
  private final Integer page;
  private final Integer pageSize;

  public SearchMyProjectsRequest(Builder builder) {
    this.query = builder.query;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String query;
    private Integer page;
    private Integer pageSize;

    private Builder() {
      // enforce method constructor
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setPage(Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SearchMyProjectsRequest build() {
      return new SearchMyProjectsRequest(this);
    }
  }
}
