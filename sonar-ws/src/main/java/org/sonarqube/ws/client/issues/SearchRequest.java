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
package org.sonarqube.ws.client.issues;

import java.util.List;
import javax.annotation.Generated;

/**
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
  private List<String> author;
  private List<String> authors;
  private String branch;
  private List<String> componentKeys;
  private List<String> componentUuids;
  private String createdAfter;
  private String createdAt;
  private String createdBefore;
  private String createdInLast;
  private List<String> cwe;
  private List<String> directories;
  private String facetMode;
  private List<String> facets;
  private List<String> fileUuids;
  private List<String> issues;
  private List<String> languages;
  private List<String> moduleUuids;
  private String onComponentOnly;
  private String organization;
  private List<String> owaspTop10;
  private String p;
  private List<String> projects;
  private String ps;
  private String pullRequest;
  private List<String> resolutions;
  private String resolved;
  private List<String> rules;
  private String s;
  private List<String> sansTop25;
  private List<String> severities;
  private String sinceLeakPeriod;
  private List<String> sonarsourceSecurity;
  private List<String> statuses;
  private List<String> tags;
  private List<String> types;

  /**
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
   * Example value: "author=torvalds@linux-foundation.org&author=linux@fondation.org"
   */
  public SearchRequest setAuthor(List<String> author) {
    this.author = author;
    return this;
  }

  public List<String> getAuthor() {
    return author;
  }

  /**
   * Example value: "torvalds@linux-foundation.org"
   * @deprecated since 7.7
   */
  @Deprecated
  public SearchRequest setAuthors(List<String> authors) {
    this.authors = authors;
    return this;
  }

  public List<String> getAuthors() {
    return authors;
  }

  /**
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
   * Example value: "12,125,unknown"
   */
  public SearchRequest setCwe(List<String> cwe) {
    this.cwe = cwe;
    return this;
  }

  public List<String> getCwe() {
    return cwe;
  }

  /**
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
   * Possible values:
   * <ul>
   *   <li>"count"</li>
   *   <li>"effort"</li>
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
   * Possible values:
   * <ul>
   *   <li>"projects"</li>
   *   <li>"moduleUuids"</li>
   *   <li>"fileUuids"</li>
   *   <li>"assigned_to_me"</li>
   *   <li>"severities"</li>
   *   <li>"statuses"</li>
   *   <li>"resolutions"</li>
   *   <li>"actionPlans"</li>
   *   <li>"rules"</li>
   *   <li>"assignees"</li>
   *   <li>"reporters"</li>
   *   <li>"authors"</li>
   *   <li>"author"</li>
   *   <li>"directories"</li>
   *   <li>"languages"</li>
   *   <li>"tags"</li>
   *   <li>"types"</li>
   *   <li>"owaspTop10"</li>
   *   <li>"sansTop25"</li>
   *   <li>"cwe"</li>
   *   <li>"createdAt"</li>
   *   <li>"sonarsourceSecurity"</li>
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
   * This is part of the internal API.
   * Example value: "7d8749e8-3070-4903-9188-bdd82933bb92"
   * @deprecated since 7.6
   */
  @Deprecated
  public SearchRequest setModuleUuids(List<String> moduleUuids) {
    this.moduleUuids = moduleUuids;
    return this;
  }

  public List<String> getModuleUuids() {
    return moduleUuids;
  }

  /**
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
   * Possible values:
   * <ul>
   *   <li>"a1"</li>
   *   <li>"a2"</li>
   *   <li>"a3"</li>
   *   <li>"a4"</li>
   *   <li>"a5"</li>
   *   <li>"a6"</li>
   *   <li>"a7"</li>
   *   <li>"a8"</li>
   *   <li>"a9"</li>
   *   <li>"a10"</li>
   *   <li>"unknown"</li>
   * </ul>
   */
  public SearchRequest setOwaspTop10(List<String> owaspTop10) {
    this.owaspTop10 = owaspTop10;
    return this;
  }

  public List<String> getOwaspTop10() {
    return owaspTop10;
  }

  /**
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
   * This is part of the internal API.
   * Example value: "5461"
   */
  public SearchRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }

  /**
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
   * Possible values:
   * <ul>
   *   <li>"insecure-interaction"</li>
   *   <li>"risky-resource"</li>
   *   <li>"porous-defenses"</li>
   * </ul>
   */
  public SearchRequest setSansTop25(List<String> sansTop25) {
    this.sansTop25 = sansTop25;
    return this;
  }

  public List<String> getSansTop25() {
    return sansTop25;
  }

  /**
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
   * Possible values:
   * <ul>
   *   <li>"sql-injection"</li>
   *   <li>"command-injection"</li>
   *   <li>"path-traversal-injection"</li>
   *   <li>"ldap-injection"</li>
   *   <li>"xpath-injection"</li>
   *   <li>"expression-lang-injection"</li>
   *   <li>"rce"</li>
   *   <li>"dos"</li>
   *   <li>"ssrf"</li>
   *   <li>"csrf"</li>
   *   <li>"xss"</li>
   *   <li>"log-injection"</li>
   *   <li>"http-response-splitting"</li>
   *   <li>"open-redirect"</li>
   *   <li>"xxe"</li>
   *   <li>"object-injection"</li>
   *   <li>"weak-cryptography"</li>
   *   <li>"auth"</li>
   *   <li>"insecure-conf"</li>
   *   <li>"file-manipulation"</li>
   * </ul>
   */
  public SearchRequest setSonarsourceSecurity(List<String> sonarsourceSecurity) {
    this.sonarsourceSecurity = sonarsourceSecurity;
    return this;
  }

  public List<String> getSonarsourceSecurity() {
    return sonarsourceSecurity;
  }

  /**
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
   * Example value: "CODE_SMELL,BUG"
   * Possible values:
   * <ul>
   *   <li>"CODE_SMELL"</li>
   *   <li>"BUG"</li>
   *   <li>"VULNERABILITY"</li>
   *   <li>"SECURITY_HOTSPOT"</li>
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
