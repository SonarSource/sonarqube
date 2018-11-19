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
package org.sonarqube.ws.client.organization;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Arrays.asList;

@Immutable
public class SearchWsRequest {
  private final Integer page;
  private final Integer pageSize;
  private final List<String> organizations;

  private SearchWsRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.organizations = builder.organizations;
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
  public List<String> getOrganizations() {
    return organizations;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Integer page;
    private Integer pageSize;
    private List<String> organizations;

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setOrganizations(String... organizations) {
      this.organizations = asList(organizations);
      return this;
    }

    public SearchWsRequest build() {
      return new SearchWsRequest(this);
    }
  }
}
