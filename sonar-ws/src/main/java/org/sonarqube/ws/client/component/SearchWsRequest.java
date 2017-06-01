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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class SearchWsRequest {
  private String organization;
  private Boolean allOrganizations;
  private List<String> qualifiers;
  private Integer page;
  private Integer pageSize;
  private String query;
  private String language;

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public SearchWsRequest setOrganization(@Nullable String organization) {
    this.organization = organization;
    return this;
  }

  @CheckForNull
  public Boolean getAllOrganizations() {
    return allOrganizations;
  }

  public SearchWsRequest setAllOrganizations(@Nullable Boolean allOrganizations) {
    this.allOrganizations = allOrganizations;
    return this;
  }

  public List<String> getQualifiers() {
    return qualifiers;
  }

  public SearchWsRequest setQualifiers(List<String> qualifiers) {
    this.qualifiers = requireNonNull(qualifiers);
    return this;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public SearchWsRequest setPage(int page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public SearchWsRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public SearchWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public SearchWsRequest setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public SearchWsRequest validate() {
    if (Boolean.TRUE.equals(allOrganizations) && organization != null) {
      throw new IllegalArgumentException("Parameter organization must not be set, if allOrganizations is set to true.");
    }
    return this;
  }
}
