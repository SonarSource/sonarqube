/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.filter;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.1
 */
public final class FilterDto {
  private Long id;
  private String name;
  private Long userId;
  private Boolean shared;
  private Boolean favourites;
  private Long resourceId;
  private String defaultView;
  private Long pageSize;
  private Long periodIndex;
  private List<CriterionDto> criteria = Lists.newArrayList();
  private List<FilterColumnDto> filterColumns = Lists.newArrayList();

  public Long getId() {
    return id;
  }

  public FilterDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public FilterDto setName(String name) {
    this.name = name;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public FilterDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public Boolean isShared() {
    return shared;
  }

  public FilterDto setShared(Boolean shared) {
    this.shared = shared;
    return this;
  }

  public Boolean isFavourites() {
    return favourites;
  }

  public FilterDto setFavourites(Boolean favourites) {
    this.favourites = favourites;
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public FilterDto setResourceId(Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getDefaultView() {
    return defaultView;
  }

  public FilterDto setDefaultView(String defaultView) {
    this.defaultView = defaultView;
    return this;
  }

  public Long getPageSize() {
    return pageSize;
  }

  public FilterDto setPageSize(Long pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public Long getPeriodIndex() {
    return periodIndex;
  }

  public FilterDto setPeriodIndex(Long periodIndex) {
    this.periodIndex = periodIndex;
    return this;
  }

  public Collection<CriterionDto> getCriteria() {
    return criteria;
  }

  public FilterDto add(CriterionDto criterion) {
    criteria.add(criterion);
    return this;
  }

  public Collection<FilterColumnDto> getColumns() {
    return filterColumns;
  }

  public FilterDto add(FilterColumnDto filterColumn) {
    filterColumns.add(filterColumn);
    return this;
  }
}
