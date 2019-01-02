/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class SearchUsersRequest {

  private String organization;
  private String qualityProfile;
  private String language;
  private String selected;
  private String query;
  private Integer page;
  private Integer pageSize;

  private SearchUsersRequest(Builder builder) {
    this.organization = builder.organization;
    this.qualityProfile = builder.qualityProfile;
    this.language = builder.language;
    this.selected = builder.selected;
    this.query = builder.query;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public String getQualityProfile() {
    return qualityProfile;
  }

  public String getLanguage() {
    return language;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public String getSelected() {
    return selected;
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
    private String organization;
    private String qualityProfile;
    private String language;
    private String selected;
    private String query;
    private Integer page;
    private Integer pageSize;

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setQualityProfile(String qualityProfile) {
      this.qualityProfile = qualityProfile;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setSelected(String selected) {
      this.selected = selected;
      return this;
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

    public SearchUsersRequest build() {
      return new SearchUsersRequest(this);
    }
  }
}
