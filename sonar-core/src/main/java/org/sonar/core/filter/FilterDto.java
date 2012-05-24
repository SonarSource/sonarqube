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
  private Long userId;
  private String defaultView;
  private Boolean favourites;
  private String key;
  private String name;
  private Long pageSize;
  private Long periodIndex;
  private Long resourceId;
  private Boolean shared;
  private List<CriterionDto> criteria = Lists.newArrayList();
  private List<FilterColumnDto> filterColumns = Lists.newArrayList();

  /**
   * Add a {@link CriterionDto} to the list.
   * 
   * @param criterion the criterion to add
   */
  public FilterDto add(CriterionDto criterion) {
    criteria.add(criterion);
    return this;
  }

  /**
   * Add a {@link FilterColumnDto} to the list.
   * 
   * @param filterColumn the column to add
   */
  public FilterDto add(FilterColumnDto filterColumn) {
    filterColumns.add(filterColumn);
    return this;
  }

  /**
   * @return the column list
   */
  public Collection<FilterColumnDto> getColumns() {
    return filterColumns;
  }

  /**
   * @return the criterion list
   */
  public Collection<CriterionDto> getCriteria() {
    return criteria;
  }

  /**
   * @return the defaut view
   */
  public String getDefaultView() {
    return defaultView;
  }

  /**
   * @return <code>true</code> if the filter returns only favourites
   */
  public Boolean getFavourites() {
    return favourites;
  }

  /**
   * @return the column list
   */
  public List<FilterColumnDto> getFilterColumns() {
    return filterColumns;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the page size
   */
  public Long getPageSize() {
    return pageSize;
  }

  /**
   * @return the period index
   */
  public Long getPeriodIndex() {
    return periodIndex;
  }

  /**
   * @return the resource id
   */
  public Long getResourceId() {
    return resourceId;
  }

  /**
   * @return <code>true</code> if the filter is shared
   */
  public Boolean getShared() {
    return shared;
  }

  /**
   * @return the user id
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * @return <code>true</code> if the filter displays only favourite resources.
   */
  public Boolean isFavourites() {
    return favourites;
  }

  /**
   * @return <code>true</code> if the filter is shared
   */
  public Boolean isShared() {
    return shared;
  }

  /**
   * @param defaultView the defaultView to set
   */
  public FilterDto setDefaultView(String defaultView) {
    this.defaultView = defaultView;
    return this;
  }

  /**
   * @param favourites the favourites to set
   */
  public FilterDto setFavourites(Boolean favourites) {
    this.favourites = favourites;
    return this;
  }

  /**
   * @param key the key to set
   */
  public FilterDto setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @param name the name to set
   */
  public FilterDto setName(String name) {
    this.name = name;
    return this;
  }

  public FilterDto setPageSize(Long pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * @param shared the shared to set
   */
  public FilterDto setShared(Boolean shared) {
    this.shared = shared;
    return this;
  }
}
