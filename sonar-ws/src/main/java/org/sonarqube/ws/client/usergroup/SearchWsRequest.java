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
package org.sonarqube.ws.client.usergroup;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchWsRequest {
  private final String query;
  private final Integer page;
  private final Integer pageSize;
  private final String organization;
  private final List<String> fields;

  private SearchWsRequest(Builder builder) {
    this.query = builder.query;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.organization = builder.organization;
    this.fields = builder.fields;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public List<String> getFields() {
    return fields;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String query;
    private Integer page;
    private Integer pageSize;
    private String organization;
    private List<String> fields;

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setFields(@Nullable List<String> fields) {
      this.fields = fields;
      return this;
    }

    public SearchWsRequest build() {
      return new SearchWsRequest(this);
    }
  }
}
