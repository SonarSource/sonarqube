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
package org.sonarqube.ws.client.issues;

import java.util.List;
import javax.annotation.Generated;

/**
 * Search for issues.<br>At most one of the following parameters can be provided at the same time: componentKeys, componentUuids, components, componentRootUuids, componentRoots.<br>Requires the 'Browse' permission on the specified project(s).
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/search">Further information about this action online (including a response example)</a>
 * @since 3.6
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private List<String> additionalFields;
  private String asc;
  private String assigned;
  private List<String> assignees;
  private List<String> authors;
  private String branch;
  private List<String> componentKeys;
  private String componentRootUuids;
  private String componentRoots;
  private List<String> componentUuids;
  private String components;
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
  private String onComponentOnly;
  private String organization;
  private String p;
  private List<String> projectUuids;
  private List<String> projects;
  private String ps;
  private List<String> resolutions;
  private String resolved;
  private List<String> rules;
  private String s;
  private List<String> severities;
  private String sinceLeakPeriod;
  private List<String> statuses;
  private List<String> tags;
  private List<String> types;

  /**
   * Comma-separated list of the optional fields to be returned in response. Action plans are dropped in 5.5, it is not returned in the response.
   *
   * Possible values:
   * <ul>
   *   <li>"_all"</li>
   *   <li>"comments"</li>
   *   <li>"languages"</li>
   *   <li>"actionPlans"</li>
   *   <li>"rules"</li>
   *   <li>"transitions"</li>
   *   <li>"actions"</li>
   *   <li>"users"</li>
   * </ul>
   */
  public SearchRequest setAdditionalFields(List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  /**
   * Ascending sort
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * To retrieve assigned or unassigned issues
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setAssigned(String assigned) {
    this.assigned = assigned;
    return this;
  }

  public String getAssigned() {
    return assigned;
  }

  /**
   * Comma-separated list of assignee logins. The value '__me__' can be used as a placeholder for user who performs the request
   *
   * Example value: "admin,usera,__me__"
   */
  public SearchRequest setAssignees(List<String> assignees) {
    this.assignees = assignees;
    return this;
  }

  public List<String> getAssignees() {
    return assignees;
  }

  /**
   * Comma-separated list of SCM accounts
   *
   * Example value: "torvalds@linux-foundation.org"
   */
  public SearchRequest setAuthors(List<String> authors) {
    this.authors = authors;
    return this;
  }

  public List<String> getAuthors() {
    return authors;
  }

  /**
   * Branch key
   *
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public SearchRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Comma-separated list of component keys. Retrieve issues associated to a specific list of components (and all its descendants). A component can be a portfolio, project, module, directory or file.
   *
   * Example value: "my_project"
   */
  public SearchRequest setComponentKeys(List<String> componentKeys) {
    this.componentKeys = componentKeys;
    return this;
  }

  public List<String> getComponentKeys() {
    return componentKeys;
  }

  /**
   * If used, will have the same meaning as componentUuids AND onComponentOnly=false.
   *
   * @deprecated since 5.1
   */
  @Deprecated
  public SearchRequest setComponentRootUuids(String componentRootUuids) {
    this.componentRootUuids = componentRootUuids;
    return this;
  }

  public String getComponentRootUuids() {
    return componentRootUuids;
  }

  /**
   * If used, will have the same meaning as componentKeys AND onComponentOnly=false.
   *
   * @deprecated since 5.1
   */
  @Deprecated
  public SearchRequest setComponentRoots(String componentRoots) {
    this.componentRoots = componentRoots;
    return this;
  }

  public String getComponentRoots() {
    return componentRoots;
  }

  /**
   * To retrieve issues associated to a specific list of components their sub-components (comma-separated list of component IDs). This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. A component can be a project, module, directory or file.
   *
   * Example value: "584a89f2-8037-4f7b-b82c-8b45d2d63fb2"
   * @deprecated since 6.5
   */
  @Deprecated
  public SearchRequest setComponentUuids(List<String> componentUuids) {
    this.componentUuids = componentUuids;
    return this;
  }

  public List<String> getComponentUuids() {
    return componentUuids;
  }

  /**
   * If used, will have the same meaning as componentKeys AND onComponentOnly=true.
   *
   * @deprecated since 5.1
   */
  @Deprecated
  public SearchRequest setComponents(String components) {
    this.components = components;
    return this;
  }

  public String getComponents() {
    return components;
  }

  /**
   * To retrieve issues created after the given date (inclusive). <br>Either a date (server timezone) or datetime can be provided. <br>If this parameter is set, createdSince must not be set
   *
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public SearchRequest setCreatedAfter(String createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  public String getCreatedAfter() {
    return createdAfter;
  }

  /**
   * Datetime to retrieve issues created during a specific analysis
   *
   * Example value: "2017-10-19T13:00:00+0200"
   */
  public SearchRequest setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  /**
   * To retrieve issues created before the given date (inclusive). <br>Either a date (server timezone) or datetime can be provided.
   *
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public SearchRequest setCreatedBefore(String createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  public String getCreatedBefore() {
    return createdBefore;
  }

  /**
   * To retrieve issues created during a time span before the current time (exclusive). Accepted units are 'y' for year, 'm' for month, 'w' for week and 'd' for day. If this parameter is set, createdAfter must not be set
   *
   * Example value: "1m2w (1 month 2 weeks)"
   */
  public SearchRequest setCreatedInLast(String createdInLast) {
    this.createdInLast = createdInLast;
    return this;
  }

  public String getCreatedInLast() {
    return createdInLast;
  }

  /**
   * To retrieve issues associated to a specific list of directories (comma-separated list of directory paths). This parameter is only meaningful when a module is selected. This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. 
   *
   * This is part of the internal API.
   * Example value: "src/main/java/org/sonar/server/"
   */
  public SearchRequest setDirectories(List<String> directories) {
    this.directories = directories;
    return this;
  }

  public List<String> getDirectories() {
    return directories;
  }

  /**
   * Choose the returned value for facet items, either count of issues or sum of debt.<br/>Since 5.5, 'debt' mode is deprecated and replaced by 'effort'
   *
   * Possible values:
   * <ul>
   *   <li>"count"</li>
   *   <li>"effort"</li>
   *   <li>"debt"</li>
   * </ul>
   */
  public SearchRequest setFacetMode(String facetMode) {
    this.facetMode = facetMode;
    return this;
  }

  public String getFacetMode() {
    return facetMode;
  }

  /**
   * Comma-separated list of the facets to be computed. No facet is computed by default.<br/>Since 5.5, facet 'actionPlans' is deprecated.<br/>Since 5.5, facet 'reporters' is deprecated.
   *
   * Possible values:
   * <ul>
   *   <li>"severities"</li>
   *   <li>"statuses"</li>
   *   <li>"resolutions"</li>
   *   <li>"actionPlans"</li>
   *   <li>"projectUuids"</li>
   *   <li>"rules"</li>
   *   <li>"assignees"</li>
   *   <li>"assigned_to_me"</li>
   *   <li>"reporters"</li>
   *   <li>"authors"</li>
   *   <li>"moduleUuids"</li>
   *   <li>"fileUuids"</li>
   *   <li>"directories"</li>
   *   <li>"languages"</li>
   *   <li>"tags"</li>
   *   <li>"types"</li>
   *   <li>"createdAt"</li>
   * </ul>
   */
  public SearchRequest setFacets(List<String> facets) {
    this.facets = facets;
    return this;
  }

  public List<String> getFacets() {
    return facets;
  }

  /**
   * To retrieve issues associated to a specific list of files (comma-separated list of file IDs). This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. 
   *
   * This is part of the internal API.
   * Example value: "bdd82933-3070-4903-9188-7d8749e8bb92"
   */
  public SearchRequest setFileUuids(List<String> fileUuids) {
    this.fileUuids = fileUuids;
    return this;
  }

  public List<String> getFileUuids() {
    return fileUuids;
  }

  /**
   * Comma-separated list of issue keys
   *
   * Example value: "5bccd6e8-f525-43a2-8d76-fcb13dde79ef"
   */
  public SearchRequest setIssues(List<String> issues) {
    this.issues = issues;
    return this;
  }

  public List<String> getIssues() {
    return issues;
  }

  /**
   * Comma-separated list of languages. Available since 4.4
   *
   * Example value: "java,js"
   */
  public SearchRequest setLanguages(List<String> languages) {
    this.languages = languages;
    return this;
  }

  public List<String> getLanguages() {
    return languages;
  }

  /**
   * To retrieve issues associated to a specific list of modules (comma-separated list of module IDs). This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. 
   *
   * This is part of the internal API.
   * Example value: "7d8749e8-3070-4903-9188-bdd82933bb92"
   */
  public SearchRequest setModuleUuids(List<String> moduleUuids) {
    this.moduleUuids = moduleUuids;
    return this;
  }

  public List<String> getModuleUuids() {
    return moduleUuids;
  }

  /**
   * Return only issues at a component's level, not on its descendants (modules, directories, files, etc). This parameter is only considered when componentKeys or componentUuids is set. Using the deprecated componentRoots or componentRootUuids parameters will set this parameter to false. Using the deprecated components parameter will set this parameter to true.
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setOnComponentOnly(String onComponentOnly) {
    this.onComponentOnly = onComponentOnly;
    return this;
  }

  public String getOnComponentOnly() {
    return onComponentOnly;
  }

  /**
   * Organization key
   *
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public SearchRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * 1-based page number
   *
   * Example value: "42"
   */
  public SearchRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * To retrieve issues associated to a specific list of projects (comma-separated list of project IDs). This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. Portfolios are not supported. If this parameter is set, 'projects' must not be set.
   *
   * This is part of the internal API.
   * Example value: "7d8749e8-3070-4903-9188-bdd82933bb92"
   */
  public SearchRequest setProjectUuids(List<String> projectUuids) {
    this.projectUuids = projectUuids;
    return this;
  }

  public List<String> getProjectUuids() {
    return projectUuids;
  }

  /**
   * To retrieve issues associated to a specific list of projects (comma-separated list of project keys). This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. If this parameter is set, projectUuids must not be set.
   *
   * This is part of the internal API.
   * Example value: "my_project"
   */
  public SearchRequest setProjects(List<String> projects) {
    this.projects = projects;
    return this;
  }

  public List<String> getProjects() {
    return projects;
  }

  /**
   * Page size. Must be greater than 0 and less than 500
   *
   * Example value: "20"
   */
  public SearchRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Comma-separated list of resolutions
   *
   * Example value: "FIXED,REMOVED"
   * Possible values:
   * <ul>
   *   <li>"FALSE-POSITIVE"</li>
   *   <li>"WONTFIX"</li>
   *   <li>"FIXED"</li>
   *   <li>"REMOVED"</li>
   * </ul>
   */
  public SearchRequest setResolutions(List<String> resolutions) {
    this.resolutions = resolutions;
    return this;
  }

  public List<String> getResolutions() {
    return resolutions;
  }

  /**
   * To match resolved or unresolved issues
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setResolved(String resolved) {
    this.resolved = resolved;
    return this;
  }

  public String getResolved() {
    return resolved;
  }

  /**
   * Comma-separated list of coding rule keys. Format is &lt;repository&gt;:&lt;rule&gt;
   *
   * Example value: "squid:AvoidCycles"
   */
  public SearchRequest setRules(List<String> rules) {
    this.rules = rules;
    return this;
  }

  public List<String> getRules() {
    return rules;
  }

  /**
   * Sort field
   *
   * Possible values:
   * <ul>
   *   <li>"CREATION_DATE"</li>
   *   <li>"UPDATE_DATE"</li>
   *   <li>"CLOSE_DATE"</li>
   *   <li>"ASSIGNEE"</li>
   *   <li>"SEVERITY"</li>
   *   <li>"STATUS"</li>
   *   <li>"FILE_LINE"</li>
   * </ul>
   */
  public SearchRequest setS(String s) {
    this.s = s;
    return this;
  }

  public String getS() {
    return s;
  }

  /**
   * Comma-separated list of severities
   *
   * Example value: "BLOCKER,CRITICAL"
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public SearchRequest setSeverities(List<String> severities) {
    this.severities = severities;
    return this;
  }

  public List<String> getSeverities() {
    return severities;
  }

  /**
   * To retrieve issues created since the leak period.<br>If this parameter is set to a truthy value, createdAfter must not be set and one component id or key must be provided.
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setSinceLeakPeriod(String sinceLeakPeriod) {
    this.sinceLeakPeriod = sinceLeakPeriod;
    return this;
  }

  public String getSinceLeakPeriod() {
    return sinceLeakPeriod;
  }

  /**
   * Comma-separated list of statuses
   *
   * Example value: "OPEN,REOPENED"
   * Possible values:
   * <ul>
   *   <li>"OPEN"</li>
   *   <li>"CONFIRMED"</li>
   *   <li>"REOPENED"</li>
   *   <li>"RESOLVED"</li>
   *   <li>"CLOSED"</li>
   * </ul>
   */
  public SearchRequest setStatuses(List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  public List<String> getStatuses() {
    return statuses;
  }

  /**
   * Comma-separated list of tags.
   *
   * Example value: "security,convention"
   */
  public SearchRequest setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  /**
   * Comma-separated list of types.
   *
   * Example value: "CODE_SMELL,BUG"
   * Possible values:
   * <ul>
   *   <li>"CODE_SMELL"</li>
   *   <li>"BUG"</li>
   *   <li>"VULNERABILITY"</li>
   * </ul>
   */
  public SearchRequest setTypes(List<String> types) {
    this.types = types;
    return this;
  }

  public List<String> getTypes() {
    return types;
  }
}
