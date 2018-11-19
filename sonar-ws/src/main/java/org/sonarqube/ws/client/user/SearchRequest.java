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
package org.sonarqube.ws.client.user;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchRequest {

  private final Integer page;
  private final Integer pageSize;
  private final String query;
  private final List<String> possibleFields;

  private SearchRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.query = builder.query;
    this.possibleFields = builder.additionalFields;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public List<String> getPossibleFields() {
    return possibleFields;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Integer page;
    private Integer pageSize;
    private String query;
    private List<String> additionalFields = new ArrayList<>();

    private Builder() {
      // enforce factory method use
    }

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setPossibleFields(List<String> possibleFields) {
      this.additionalFields = possibleFields;
      return this;
    }

    public SearchRequest build() {
      return new SearchRequest(this);
    }
  }
}
