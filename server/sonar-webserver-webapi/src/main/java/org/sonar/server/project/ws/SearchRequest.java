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
package org.sonar.server.project.ws;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.MAX_PAGE_SIZE;

class SearchRequest {

  private final String organization;
  private final String query;
  private final List<String> qualifiers;
  private final String visibility;
  private final Integer page;
  private final Integer pageSize;
  private final String analyzedBefore;
  private final boolean onProvisionedOnly;
  private final List<String> projects;

  public SearchRequest(Builder builder) {
    this.organization = builder.organization;
    this.query = builder.query;
    this.qualifiers = builder.qualifiers;
    this.visibility = builder.visibility;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.analyzedBefore = builder.analyzedBefore;
    this.onProvisionedOnly = builder.onProvisionedOnly;
    this.projects = builder.projects;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public List<String> getQualifiers() {
    return qualifiers;
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

  @CheckForNull
  public String getVisibility() {
    return visibility;
  }

  @CheckForNull
  public String getAnalyzedBefore() {
    return analyzedBefore;
  }

  public boolean isOnProvisionedOnly() {
    return onProvisionedOnly;
  }

  @CheckForNull
  public List<String> getProjects() {
    return projects;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private List<String> qualifiers = singletonList(Qualifiers.PROJECT);
    private Integer page;
    private Integer pageSize;
    private String query;
    private String visibility;
    private String analyzedBefore;
    private boolean onProvisionedOnly = false;
    private List<String> projects;

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setQualifiers(List<String> qualifiers) {
      this.qualifiers = requireNonNull(qualifiers, "Qualifiers cannot be null");
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

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    public Builder setAnalyzedBefore(@Nullable String lastAnalysisBefore) {
      this.analyzedBefore = lastAnalysisBefore;
      return this;
    }

    public Builder setOnProvisionedOnly(boolean onProvisionedOnly) {
      this.onProvisionedOnly = onProvisionedOnly;
      return this;
    }

    public Builder setProjects(@Nullable List<String> projects) {
      this.projects = projects;
      return this;
    }

    public SearchRequest build() {
      checkArgument(projects==null || !projects.isEmpty(), "Project key list must not be empty");
      checkArgument(pageSize == null || pageSize <= MAX_PAGE_SIZE, "Page size must not be greater than %s", MAX_PAGE_SIZE);
      return new SearchRequest(this);
    }
  }
}
