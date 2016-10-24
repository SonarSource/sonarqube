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

package org.sonarqube.ws.client.component;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SearchProjectsRequest {
  public static final int MAX_PAGE_SIZE = 500;
  public static final int DEFAULT_PAGE_SIZE = 100;

  private final int page;
  private final int pageSize;
  private final String filter;
  private final List<String> facets;

  private SearchProjectsRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.filter = builder.filter;
    this.facets = builder.facets;
  }

  @CheckForNull
  public String getFilter() {
    return filter;
  }

  public List<String> getFacets() {
    return facets;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPage() {
    return page;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Integer page;
    private Integer pageSize;
    private String filter;
    private List<String> facets = new ArrayList<>();

    private Builder() {
      // enforce static factory method
    }

    public Builder setFilter(String filter) {
      this.filter = filter;
      return this;
    }

    public Builder setFacets(List<String> facets) {
      this.facets = requireNonNull(facets);
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

    public SearchProjectsRequest build() {
      if (page == null) {
        page = 1;
      }
      if (pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
      checkArgument(pageSize <= MAX_PAGE_SIZE, "Page size must not be greater than %s", MAX_PAGE_SIZE);
      return new SearchProjectsRequest(this);
    }
  }
}
