/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.measure.ws;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class ComponentTreeRequest {

  private String baseComponentId;
  private String component;
  private String branch;
  private String strategy;
  private List<String> qualifiers;
  private List<String> additionalFields;
  private String query;
  private List<String> sort;
  private Boolean asc;
  private String metricSort;
  private Integer metricPeriodSort;
  private String metricSortFilter;
  private List<String> metricKeys;
  private Integer page;
  private Integer pageSize;
  private String developerId;
  private String developerKey;

  /**
   * @deprecated since 6.6, please use {@link #getComponent()} instead
   */
  @Deprecated
  @CheckForNull
  public String getBaseComponentId() {
    return baseComponentId;
  }

  /**
   * @deprecated since 6.6, please use {@link #setComponent(String)} instead
   */
  @Deprecated
  public ComponentTreeRequest setBaseComponentId(@Nullable String baseComponentId) {
    this.baseComponentId = baseComponentId;
    return this;
  }

  @CheckForNull
  public String getComponent() {
    return component;
  }

  public ComponentTreeRequest setComponent(@Nullable String component) {
    this.component = component;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public ComponentTreeRequest setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  @CheckForNull
  public String getStrategy() {
    return strategy;
  }

  public ComponentTreeRequest setStrategy(String strategy) {
    this.strategy = strategy;
    return this;
  }

  @CheckForNull
  public List<String> getQualifiers() {
    return qualifiers;
  }

  public ComponentTreeRequest setQualifiers(@Nullable List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  @CheckForNull
  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public ComponentTreeRequest setAdditionalFields(@Nullable List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public ComponentTreeRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  @CheckForNull
  public List<String> getSort() {
    return sort;
  }

  public ComponentTreeRequest setSort(@Nullable List<String> sort) {
    this.sort = sort;
    return this;
  }

  @CheckForNull
  public String getMetricSort() {
    return metricSort;
  }

  public ComponentTreeRequest setMetricSort(@Nullable String metricSort) {
    this.metricSort = metricSort;
    return this;
  }

  @CheckForNull
  public String getMetricSortFilter() {
    return metricSortFilter;
  }

  public ComponentTreeRequest setMetricSortFilter(@Nullable String metricSortFilter) {
    this.metricSortFilter = metricSortFilter;
    return this;
  }

  @CheckForNull
  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public ComponentTreeRequest setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public ComponentTreeRequest setAsc(@Nullable Boolean asc) {
    this.asc = asc;
    return this;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public ComponentTreeRequest setPage(int page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public ComponentTreeRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public Integer getMetricPeriodSort() {
    return metricPeriodSort;
  }

  public ComponentTreeRequest setMetricPeriodSort(@Nullable Integer metricPeriodSort) {
    this.metricPeriodSort = metricPeriodSort;
    return this;
  }

  @CheckForNull
  public String getDeveloperId() {
    return developerId;
  }

  public ComponentTreeRequest setDeveloperId(@Nullable String developerId) {
    this.developerId = developerId;
    return this;
  }

  @CheckForNull
  public String getDeveloperKey() {
    return developerKey;
  }

  public ComponentTreeRequest setDeveloperKey(@Nullable String developerKey) {
    this.developerKey = developerKey;
    return this;
  }
}
