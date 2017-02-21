/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.component;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SearchProjectsRequest {

  public static final int MAX_PAGE_SIZE = 500;
  public static final int DEFAULT_PAGE_SIZE = 100;

  private final int page;
  private final int pageSize;
  private final String organization;
  private final String filter;
  private final List<String> facets;
  private final String sort;
  private final Boolean asc;
  private final List<String> additionalFields;

  private SearchProjectsRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.organization = builder.organization;
    this.filter = builder.filter;
    this.facets = builder.facets;
    this.sort = builder.sort;
    this.asc = builder.asc;
    this.additionalFields = builder.additionalFields;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getFilter() {
    return filter;
  }

  public List<String> getFacets() {
    return facets;
  }

  @CheckForNull
  public String getSort() {
    return sort;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPage() {
    return page;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private Integer page;
    private Integer pageSize;
    private String filter;
    private List<String> facets = new ArrayList<>();
    private String sort;
    private Boolean asc;
    private List<String> additionalFields = new ArrayList<>();

    private Builder() {
      // enforce static factory method
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setFilter(@Nullable String filter) {
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

    public Builder setSort(@Nullable String sort) {
      this.sort = sort;
      return this;
    }

    public Builder setAsc(boolean asc) {
      this.asc = asc;
      return this;
    }

    public Builder setAdditionalFields(List<String> additionalFields) {
      this.additionalFields = requireNonNull(additionalFields, "additional fields cannot be null");
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
