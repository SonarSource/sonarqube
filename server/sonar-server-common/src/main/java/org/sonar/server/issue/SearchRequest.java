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
package org.sonar.server.issue;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchRequest {
  private List<String> actionPlans;
  private List<String> additionalFields;
  private Boolean asc;
  private Boolean assigned;
  private List<String> assigneesUuid;
  private List<String> authors;
  private List<String> componentKeys;
  private List<String> componentRootUuids;
  private List<String> componentRoots;
  private List<String> componentUuids;
  private List<String> components;
  private String createdAfter;
  private String createdAt;
  private String createdBefore;
  private String createdInLast;
  private List<String> directories;
  private String facetMode;
  private List<String> facets;
  private List<String> fileUuids;
  private List<String> issues;
  private List<String> languages;
  private List<String> moduleUuids;
  private Boolean onComponentOnly;
  private String branch;
  private String pullRequest;
  private String organization;
  private int page;
  private int pageSize;
  private List<String> projectKeys;
  private List<String> projects;
  private List<String> resolutions;
  private Boolean resolved;
  private List<String> rules;
  private Boolean sinceLeakPeriod;
  private String sort;
  private List<String> severities;
  private List<String> statuses;
  private List<String> tags;
  private List<String> types;
  private List<String> owaspTop10;
  private List<String> sansTop25;
  private List<String> sonarsourceSecurity;
  private List<String> cwe;

  @CheckForNull
  public List<String> getActionPlans() {
    return actionPlans;
  }

  public SearchRequest setActionPlans(@Nullable List<String> actionPlans) {
    this.actionPlans = actionPlans;
    return this;
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
  public List<String> getComponentKeys() {
    return componentKeys;
  }

  public SearchRequest setComponentKeys(@Nullable List<String> componentKeys) {
    this.componentKeys = componentKeys;
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
  public List<String> getFacets() {
    return facets;
  }

  public SearchRequest setFacets(@Nullable List<String> facets) {
    this.facets = facets;
    return this;
  }

  @CheckForNull
  public List<String> getFileUuids() {
    return fileUuids;
  }

  public SearchRequest setFileUuids(@Nullable List<String> fileUuids) {
    this.fileUuids = fileUuids;
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
  public List<String> getLanguages() {
    return languages;
  }

  public SearchRequest setLanguages(@Nullable List<String> languages) {
    this.languages = languages;
    return this;
  }

  @CheckForNull
  public List<String> getModuleUuids() {
    return moduleUuids;
  }

  public SearchRequest setModuleUuids(@Nullable List<String> moduleUuids) {
    this.moduleUuids = moduleUuids;
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

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public SearchRequest setOrganization(@Nullable String s) {
    this.organization = s;
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
  public List<String> getProjectKeys() {
    return projectKeys;
  }

  public SearchRequest setProjectKeys(@Nullable List<String> projectKeys) {
    this.projectKeys = projectKeys;
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

  @CheckForNull
  public Boolean getSinceLeakPeriod() {
    return sinceLeakPeriod;
  }

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
  public List<String> getTypes() {
    return types;
  }

  public SearchRequest setTypes(@Nullable List<String> types) {
    this.types = types;
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
  public List<String> getComponentRootUuids() {
    return componentRootUuids;
  }

  public SearchRequest setComponentRootUuids(@Nullable List<String> componentRootUuids) {
    this.componentRootUuids = componentRootUuids;
    return this;
  }

  @CheckForNull
  public List<String> getComponentRoots() {
    return componentRoots;
  }

  public SearchRequest setComponentRoots(@Nullable List<String> componentRoots) {
    this.componentRoots = componentRoots;
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
}
