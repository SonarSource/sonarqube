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
package org.sonarqube.ws.client.rule;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchWsRequest {
  private Boolean activation;
  private List<String> activeSeverities;
  private Boolean asc;
  private String availableSince;
  private List<String> fields;
  private List<String> facets;
  private List<String> inheritance;
  private Boolean isTemplate;
  private List<String> languages;
  private Integer page;
  private Integer pageSize;
  private String query;
  private String qProfile;
  private String compareToProfile;
  private List<String> repositories;
  private String ruleKey;
  private String sort;
  private List<String> severities;
  private List<String> statuses;
  private List<String> tags;
  private String templateKey;
  private List<String> types;

  @CheckForNull
  public Boolean getActivation() {
    return activation;
  }

  public SearchWsRequest setActivation(@Nullable Boolean activation) {
    this.activation = activation;
    return this;
  }

  @CheckForNull
  public List<String> getActiveSeverities() {
    return activeSeverities;
  }

  public SearchWsRequest setActiveSeverities(@Nullable List<String> activeSeverities) {
    this.activeSeverities = activeSeverities;
    return this;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public SearchWsRequest setAsc(Boolean asc) {
    this.asc = asc;
    return this;
  }

  @CheckForNull
  public String getAvailableSince() {
    return availableSince;
  }

  public SearchWsRequest setAvailableSince(@Nullable String availableSince) {
    this.availableSince = availableSince;
    return this;
  }

  @CheckForNull
  public List<String> getFields() {
    return fields;
  }

  public SearchWsRequest setFields(@Nullable List<String> fields) {
    this.fields = fields;
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
  public List<String> getInheritance() {
    return inheritance;
  }

  public SearchWsRequest setInheritance(@Nullable List<String> inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public Boolean getIsTemplate() {
    return isTemplate;
  }

  public SearchWsRequest setIsTemplate(@Nullable Boolean isTemplate) {
    this.isTemplate = isTemplate;
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
  public Integer getPage() {
    return page;
  }

  public SearchWsRequest setPage(@Nullable Integer page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public SearchWsRequest setPageSize(@Nullable Integer pageSize) {
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
  public String getQProfile() {
    return qProfile;
  }

  public SearchWsRequest setQProfile(@Nullable String qProfile) {
    this.qProfile = qProfile;
    return this;
  }

  @CheckForNull
  public String getCompareToProfile() {
    return compareToProfile;
  }

  public SearchWsRequest setCompareToProfile(@Nullable String compareToProfile) {
    this.compareToProfile = compareToProfile;
    return this;
  }

  @CheckForNull
  public List<String> getRepositories() {
    return repositories;
  }

  public SearchWsRequest setRepositories(@Nullable List<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  @CheckForNull
  public String getRuleKey() {
    return ruleKey;
  }

  public SearchWsRequest setRuleKey(@Nullable String ruleKey) {
    this.ruleKey = ruleKey;
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
  public String getTemplateKey() {
    return templateKey;
  }

  public SearchWsRequest setTemplateKey(@Nullable String templateKey) {
    this.templateKey = templateKey;
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
}
