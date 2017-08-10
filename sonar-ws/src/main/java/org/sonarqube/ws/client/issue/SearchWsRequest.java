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
package org.sonarqube.ws.client.issue;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchWsRequest {
  private List<String> actionPlans;
  private List<String> additionalFields;
  private Boolean asc;
  private Boolean assigned;
  private List<String> assignees;
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
  private String organization;
  private Integer page;
  private Integer pageSize;
  private List<String> projectKeys;
  private List<String> projectUuids;
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

  @CheckForNull
  public List<String> getActionPlans() {
    return actionPlans;
  }

  public SearchWsRequest setActionPlans(@Nullable List<String> actionPlans) {
    this.actionPlans = actionPlans;
    return this;
  }

  @CheckForNull
  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public SearchWsRequest setAdditionalFields(@Nullable List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public SearchWsRequest setAsc(boolean asc) {
    this.asc = asc;
    return this;
  }

  @CheckForNull
  public Boolean getAssigned() {
    return assigned;
  }

  public SearchWsRequest setAssigned(@Nullable Boolean assigned) {
    this.assigned = assigned;
    return this;
  }

  @CheckForNull
  public List<String> getAssignees() {
    return assignees;
  }

  public SearchWsRequest setAssignees(@Nullable List<String> assignees) {
    this.assignees = assignees;
    return this;
  }

  @CheckForNull
  public List<String> getAuthors() {
    return authors;
  }

  public SearchWsRequest setAuthors(@Nullable List<String> authors) {
    this.authors = authors;
    return this;
  }

  @CheckForNull
  public List<String> getComponentKeys() {
    return componentKeys;
  }

  public SearchWsRequest setComponentKeys(@Nullable List<String> componentKeys) {
    this.componentKeys = componentKeys;
    return this;
  }

  @CheckForNull
  public List<String> getComponentUuids() {
    return componentUuids;
  }

  public SearchWsRequest setComponentUuids(@Nullable List<String> componentUuids) {
    this.componentUuids = componentUuids;
    return this;
  }

  @CheckForNull
  public String getCreatedAfter() {
    return createdAfter;
  }

  public SearchWsRequest setCreatedAfter(@Nullable String createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  @CheckForNull
  public String getCreatedAt() {
    return createdAt;
  }

  public SearchWsRequest setCreatedAt(@Nullable String createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public String getCreatedBefore() {
    return createdBefore;
  }

  public SearchWsRequest setCreatedBefore(@Nullable String createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  @CheckForNull
  public String getCreatedInLast() {
    return createdInLast;
  }

  public SearchWsRequest setCreatedInLast(@Nullable String createdInLast) {
    this.createdInLast = createdInLast;
    return this;
  }

  @CheckForNull
  public List<String> getDirectories() {
    return directories;
  }

  public SearchWsRequest setDirectories(@Nullable List<String> directories) {
    this.directories = directories;
    return this;
  }

  @CheckForNull
  public String getFacetMode() {
    return facetMode;
  }

  public SearchWsRequest setFacetMode(@Nullable String facetMode) {
    this.facetMode = facetMode;
    return this;
  }

  @CheckForNull
  public List<String> getFacets() {
    return facets;
  }

  public SearchWsRequest setFacets(@Nullable List<String> facets) {
    this.facets = facets;
    return this;
  }

  @CheckForNull
  public List<String> getFileUuids() {
    return fileUuids;
  }

  public SearchWsRequest setFileUuids(@Nullable List<String> fileUuids) {
    this.fileUuids = fileUuids;
    return this;
  }

  @CheckForNull
  public List<String> getIssues() {
    return issues;
  }

  public SearchWsRequest setIssues(@Nullable List<String> issues) {
    this.issues = issues;
    return this;
  }

  @CheckForNull
  public List<String> getLanguages() {
    return languages;
  }

  public SearchWsRequest setLanguages(@Nullable List<String> languages) {
    this.languages = languages;
    return this;
  }

  @CheckForNull
  public List<String> getModuleUuids() {
    return moduleUuids;
  }

  public SearchWsRequest setModuleUuids(@Nullable List<String> moduleUuids) {
    this.moduleUuids = moduleUuids;
    return this;
  }

  @CheckForNull
  public Boolean getOnComponentOnly() {
    return onComponentOnly;
  }

  public SearchWsRequest setOnComponentOnly(Boolean onComponentOnly) {
    this.onComponentOnly = onComponentOnly;
    return this;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public SearchWsRequest setOrganization(@Nullable String s) {
    this.organization = s;
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
  public List<String> getProjectKeys() {
    return projectKeys;
  }

  public SearchWsRequest setProjectKeys(@Nullable List<String> projectKeys) {
    this.projectKeys = projectKeys;
    return this;
  }

  @CheckForNull
  public List<String> getProjectUuids() {
    return projectUuids;
  }

  public SearchWsRequest setProjectUuids(@Nullable List<String> projectUuids) {
    this.projectUuids = projectUuids;
    return this;
  }

  @CheckForNull
  public List<String> getResolutions() {
    return resolutions;
  }

  public SearchWsRequest setResolutions(@Nullable List<String> resolutions) {
    this.resolutions = resolutions;
    return this;
  }

  @CheckForNull
  public Boolean getResolved() {
    return resolved;
  }

  public SearchWsRequest setResolved(@Nullable Boolean resolved) {
    this.resolved = resolved;
    return this;
  }

  @CheckForNull
  public List<String> getRules() {
    return rules;
  }

  public SearchWsRequest setRules(@Nullable List<String> rules) {
    this.rules = rules;
    return this;
  }

  @CheckForNull
  public Boolean getSinceLeakPeriod() {
    return sinceLeakPeriod;
  }

  public SearchWsRequest setSinceLeakPeriod(@Nullable Boolean sinceLeakPeriod) {
    this.sinceLeakPeriod = sinceLeakPeriod;
    return this;
  }

  @CheckForNull
  public String getSort() {
    return sort;
  }

  public SearchWsRequest setSort(@Nullable String sort) {
    this.sort = sort;
    return this;
  }

  @CheckForNull
  public List<String> getSeverities() {
    return severities;
  }

  public SearchWsRequest setSeverities(@Nullable List<String> severities) {
    this.severities = severities;
    return this;
  }

  @CheckForNull
  public List<String> getStatuses() {
    return statuses;
  }

  public SearchWsRequest setStatuses(@Nullable List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  @CheckForNull
  public List<String> getTags() {
    return tags;
  }

  public SearchWsRequest setTags(@Nullable List<String> tags) {
    this.tags = tags;
    return this;
  }

  @CheckForNull
  public List<String> getTypes() {
    return types;
  }

  public SearchWsRequest setTypes(@Nullable List<String> types) {
    this.types = types;
    return this;
  }

  @CheckForNull
  public List<String> getComponentRootUuids() {
    return componentRootUuids;
  }

  public SearchWsRequest setComponentRootUuids(List<String> componentRootUuids) {
    this.componentRootUuids = componentRootUuids;
    return this;
  }

  @CheckForNull
  public List<String> getComponentRoots() {
    return componentRoots;
  }

  public SearchWsRequest setComponentRoots(@Nullable List<String> componentRoots) {
    this.componentRoots = componentRoots;
    return this;
  }

  @CheckForNull
  public List<String> getComponents() {
    return components;
  }

  public SearchWsRequest setComponents(@Nullable List<String> components) {
    this.components = components;
    return this;
  }

  @CheckForNull
  public List<String> getProjects() {
    return projects;
  }

  public SearchWsRequest setProjects(@Nullable List<String> projects) {
    this.projects = projects;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public SearchWsRequest setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }
}
