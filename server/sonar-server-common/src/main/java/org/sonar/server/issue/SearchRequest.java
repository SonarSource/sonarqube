/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchRequest {
  private List<String> additionalFields;
  private Boolean asc;
  private Boolean assigned;
  private List<String> assigneesUuid;
  private List<String> authors;
  private List<String> componentUuids;
  private List<String> components;
  private String createdAfter;
  private String createdAt;
  private String createdBefore;
  private String createdInLast;
  private List<String> directories;
  private String facetMode;
  private List<String> facets;
  private List<String> files;
  private List<String> issues;
  private Boolean inNewCodePeriod;
  private Set<String> scopes;
  private List<String> languages;
  private Boolean onComponentOnly;
  private String branch;
  private String pullRequest;
  private int page;
  private int pageSize;
  private List<String> projects;
  private List<String> resolutions;
  private Boolean resolved;
  private List<String> rules;
  private Boolean sinceLeakPeriod;
  private String sort;
  private List<String> severities;
  private List<String> statuses;
  private List<String> tags;
  private Set<String> types;
  private List<String> pciDss32;
  private List<String> pciDss40;
  private List<String> owaspTop10;
  private List<String> owaspAsvs40;
  private List<String> owaspTop10For2021;
  private List<String> sansTop25;
  private List<String> sonarsourceSecurity;
  private List<String> cwe;
  private String searchAfter;
  private String timeZone;

  private Integer owaspAsvsLevel;

  private String organization;

  public SearchRequest() {
    // nothing to do here
  }

  @CheckForNull
  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public SearchRequest setAdditionalFields(@Nullable List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public SearchRequest setAsc(boolean asc) {
    this.asc = asc;
    return this;
  }

  @CheckForNull
  public Boolean getAssigned() {
    return assigned;
  }

  public SearchRequest setAssigned(@Nullable Boolean assigned) {
    this.assigned = assigned;
    return this;
  }

  @CheckForNull
  public List<String> getAssigneeUuids() {
    return assigneesUuid;
  }

  public SearchRequest setAssigneesUuid(@Nullable List<String> assigneesUuid) {
    this.assigneesUuid = assigneesUuid;
    return this;
  }

  @CheckForNull
  public List<String> getAuthors() {
    return authors;
  }

  public SearchRequest setAuthors(@Nullable List<String> authors) {
    this.authors = authors;
    return this;
  }

  @CheckForNull
  public List<String> getComponentUuids() {
    return componentUuids;
  }

  public SearchRequest setComponentUuids(@Nullable List<String> componentUuids) {
    this.componentUuids = componentUuids;
    return this;
  }

  @CheckForNull
  public String getCreatedAfter() {
    return createdAfter;
  }

  public SearchRequest setCreatedAfter(@Nullable String createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  @CheckForNull
  public String getCreatedAt() {
    return createdAt;
  }

  public SearchRequest setCreatedAt(@Nullable String createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public String getCreatedBefore() {
    return createdBefore;
  }

  public SearchRequest setCreatedBefore(@Nullable String createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  @CheckForNull
  public String getCreatedInLast() {
    return createdInLast;
  }

  public SearchRequest setCreatedInLast(@Nullable String createdInLast) {
    this.createdInLast = createdInLast;
    return this;
  }

  @CheckForNull
  public List<String> getDirectories() {
    return directories;
  }

  public SearchRequest setDirectories(@Nullable List<String> directories) {
    this.directories = directories;
    return this;
  }

  @CheckForNull
  public String getFacetMode() {
    return facetMode;
  }

  public SearchRequest setFacetMode(@Nullable String facetMode) {
    this.facetMode = facetMode;
    return this;
  }

  @CheckForNull
  public String getSearchAfter() {
    return searchAfter;
  }

  public SearchRequest setSearchAfter(@Nullable String searchAfter) {
    this.searchAfter = searchAfter;
    return this;
  }

  @CheckForNull
  public List<String> getFacets() {
    return facets;
  }

  public SearchRequest setFacets(@Nullable List<String> facets) {
    this.facets = facets;
    return this;
  }

  @CheckForNull
  public List<String> getFiles() {
    return files;
  }

  public SearchRequest setFiles(@Nullable List<String> files) {
    this.files = files;
    return this;
  }

  @CheckForNull
  public List<String> getIssues() {
    return issues;
  }

  public SearchRequest setIssues(@Nullable List<String> issues) {
    this.issues = issues;
    return this;
  }

  @CheckForNull
  public Set<String> getScopes() {
    return scopes;
  }

  public SearchRequest setScopes(@Nullable Collection<String> scopes) {
    this.scopes = scopes == null ? null : ImmutableSet.copyOf(scopes);
    return this;
  }

  @CheckForNull
  public List<String> getLanguages() {
    return languages;
  }

  public SearchRequest setLanguages(@Nullable List<String> languages) {
    this.languages = languages;
    return this;
  }

  @CheckForNull
  public Boolean getOnComponentOnly() {
    return onComponentOnly;
  }

  public SearchRequest setOnComponentOnly(@Nullable Boolean onComponentOnly) {
    this.onComponentOnly = onComponentOnly;
    return this;
  }

  public int getPage() {
    return page;
  }

  public SearchRequest setPage(int page) {
    this.page = page;
    return this;
  }

  public int getPageSize() {
    return pageSize;
  }

  public SearchRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public List<String> getResolutions() {
    return resolutions;
  }

  public SearchRequest setResolutions(@Nullable List<String> resolutions) {
    this.resolutions = resolutions;
    return this;
  }

  @CheckForNull
  public Boolean getResolved() {
    return resolved;
  }

  public SearchRequest setResolved(@Nullable Boolean resolved) {
    this.resolved = resolved;
    return this;
  }

  @CheckForNull
  public List<String> getRules() {
    return rules;
  }

  public SearchRequest setRules(@Nullable List<String> rules) {
    this.rules = rules;
    return this;
  }

  /**
   * @deprecated since 9.4 - replaced by getInNewCodePeriod()
   */
  @Deprecated(since = "9.4")
  @CheckForNull
  public Boolean getSinceLeakPeriod() {
    return sinceLeakPeriod;
  }

  /**
   * @deprecated since 9.4 - replaced by setInNewCodePeriod()
   */
  @Deprecated(since = "9.4")
  public SearchRequest setSinceLeakPeriod(@Nullable Boolean sinceLeakPeriod) {
    this.sinceLeakPeriod = sinceLeakPeriod;
    return this;
  }

  @CheckForNull
  public String getSort() {
    return sort;
  }

  public SearchRequest setSort(@Nullable String sort) {
    this.sort = sort;
    return this;
  }

  @CheckForNull
  public List<String> getSeverities() {
    return severities;
  }

  public SearchRequest setSeverities(@Nullable List<String> severities) {
    this.severities = severities;
    return this;
  }

  @CheckForNull
  public List<String> getStatuses() {
    return statuses;
  }

  public SearchRequest setStatuses(@Nullable List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  @CheckForNull
  public List<String> getTags() {
    return tags;
  }

  public SearchRequest setTags(@Nullable List<String> tags) {
    this.tags = tags;
    return this;
  }

  @CheckForNull
  public Set<String> getTypes() {
    return types;
  }

  public SearchRequest setTypes(@Nullable Collection<String> types) {
    this.types = types == null ? null : ImmutableSet.copyOf(types);
    return this;
  }

  @CheckForNull
  public List<String> getPciDss32() {
    return pciDss32;
  }

  public SearchRequest setPciDss32(@Nullable List<String> pciDss32) {
    this.pciDss32 = pciDss32;
    return this;
  }

  @CheckForNull
  public List<String> getPciDss40() {
    return pciDss40;
  }

  public SearchRequest setPciDss40(@Nullable List<String> pciDss40) {
    this.pciDss40 = pciDss40;
    return this;
  }

  @CheckForNull
  public List<String> getOwaspAsvs40() {
    return owaspAsvs40;
  }

  public SearchRequest setOwaspAsvs40(@Nullable List<String> owaspAsvs40) {
    this.owaspAsvs40 = owaspAsvs40;
    return this;
  }

  @CheckForNull
  public List<String> getOwaspTop10() {
    return owaspTop10;
  }

  public SearchRequest setOwaspTop10(@Nullable List<String> owaspTop10) {
    this.owaspTop10 = owaspTop10;
    return this;
  }

  @CheckForNull
  public List<String> getOwaspTop10For2021() {
    return owaspTop10For2021;
  }

  public SearchRequest setOwaspTop10For2021(@Nullable List<String> owaspTop10For2021) {
    this.owaspTop10For2021 = owaspTop10For2021;
    return this;
  }

  @CheckForNull
  public List<String> getSansTop25() {
    return sansTop25;
  }

  public SearchRequest setSansTop25(@Nullable List<String> sansTop25) {
    this.sansTop25 = sansTop25;
    return this;
  }

  @CheckForNull
  public List<String> getCwe() {
    return cwe;
  }

  public SearchRequest setCwe(@Nullable List<String> cwe) {
    this.cwe = cwe;
    return this;
  }

  @CheckForNull
  public List<String> getSonarsourceSecurity() {
    return sonarsourceSecurity;
  }

  public SearchRequest setSonarsourceSecurity(@Nullable List<String> sonarsourceSecurity) {
    this.sonarsourceSecurity = sonarsourceSecurity;
    return this;
  }

  @CheckForNull
  public List<String> getComponents() {
    return components;
  }

  public SearchRequest setComponents(@Nullable List<String> components) {
    this.components = components;
    return this;
  }

  @CheckForNull
  public List<String> getProjects() {
    return projects;
  }

  public SearchRequest setProjects(@Nullable List<String> projects) {
    this.projects = projects;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public SearchRequest setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  @CheckForNull
  public String getPullRequest() {
    return pullRequest;
  }

  public SearchRequest setPullRequest(@Nullable String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  @CheckForNull
  public String getTimeZone() {
    return timeZone;
  }

  public SearchRequest setTimeZone(@Nullable String timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  @CheckForNull
  public Boolean getInNewCodePeriod() {
    return inNewCodePeriod;
  }

  public SearchRequest setInNewCodePeriod(@Nullable Boolean inNewCodePeriod) {
    this.inNewCodePeriod = inNewCodePeriod;
    return this;
  }

  public Integer getOwaspAsvsLevel() {
    return owaspAsvsLevel;
  }

  public SearchRequest setOwaspAsvsLevel(@Nullable Integer owaspAsvsLevel) {
    this.owaspAsvsLevel = owaspAsvsLevel;
    return this;
  }

  public SearchRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }
}
