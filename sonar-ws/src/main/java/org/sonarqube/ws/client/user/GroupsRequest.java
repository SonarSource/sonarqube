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
package org.sonarqube.ws.client.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@Immutable
public class GroupsRequest {

  private final String login;
  private final String organization;
  private final String query;
  private final String selected;
  private final Integer page;
  private final Integer pageSize;

  private GroupsRequest(Builder builder) {
    this.login = builder.login;
    this.organization = builder.organization;
    this.query = builder.query;
    this.selected = builder.selected;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public String getLogin() {
    return login;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  @CheckForNull
  public String getSelected() {
    return selected;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String login;
    private String organization;
    private String query;
    private String selected;
    private Integer page;
    private Integer pageSize;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setSelected(@Nullable String selected) {
      this.selected = selected;
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

    public GroupsRequest build() {
      checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
      return new GroupsRequest(this);
    }
  }
}
