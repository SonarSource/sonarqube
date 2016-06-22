/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarqube.ws.client.measure;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ComponentTreeWsRequest {

  private String baseComponentId;
  private String baseComponentKey;
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

  @CheckForNull
  public String getBaseComponentId() {
    return baseComponentId;
  }

  public ComponentTreeWsRequest setBaseComponentId(@Nullable String baseComponentId) {
    this.baseComponentId = baseComponentId;
    return this;
  }

  @CheckForNull
  public String getBaseComponentKey() {
    return baseComponentKey;
  }

  public ComponentTreeWsRequest setBaseComponentKey(@Nullable String baseComponentKey) {
    this.baseComponentKey = baseComponentKey;
    return this;
  }

  @CheckForNull
  public String getStrategy() {
    return strategy;
  }

  public ComponentTreeWsRequest setStrategy(String strategy) {
    this.strategy = strategy;
    return this;
  }

  @CheckForNull
  public List<String> getQualifiers() {
    return qualifiers;
  }

  public ComponentTreeWsRequest setQualifiers(@Nullable List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  @CheckForNull
  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  public ComponentTreeWsRequest setAdditionalFields(@Nullable List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public ComponentTreeWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  public List<String> getSort() {
    return sort;
  }

  public ComponentTreeWsRequest setSort(List<String> sort) {
    this.sort = sort;
    return this;
  }

  @CheckForNull
  public String getMetricSort() {
    return metricSort;
  }

  public ComponentTreeWsRequest setMetricSort(@Nullable String metricSort) {
    this.metricSort = metricSort;
    return this;
  }

  @CheckForNull
  public String getMetricSortFilter() {
    return metricSortFilter;
  }

  public ComponentTreeWsRequest setMetricSortFilter(@Nullable String metricSortFilter) {
    this.metricSortFilter = metricSortFilter;
    return this;
  }

  @CheckForNull
  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public ComponentTreeWsRequest setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  @CheckForNull
  public Boolean getAsc() {
    return asc;
  }

  public ComponentTreeWsRequest setAsc(boolean asc) {
    this.asc = asc;
    return this;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public ComponentTreeWsRequest setPage(int page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public ComponentTreeWsRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public Integer getMetricPeriodSort() {
    return metricPeriodSort;
  }

  public ComponentTreeWsRequest setMetricPeriodSort(@Nullable Integer metricPeriodSort) {
    this.metricPeriodSort = metricPeriodSort;
    return this;
  }

  @CheckForNull
  public String getDeveloperId() {
    return developerId;
  }

  public ComponentTreeWsRequest setDeveloperId(@Nullable String developerId) {
    this.developerId = developerId;
    return this;
  }

  @CheckForNull
  public String getDeveloperKey() {
    return developerKey;
  }

  public ComponentTreeWsRequest setDeveloperKey(@Nullable String developerKey) {
    this.developerKey = developerKey;
    return this;
  }
}
