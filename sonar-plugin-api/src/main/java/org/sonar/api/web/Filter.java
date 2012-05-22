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
package org.sonar.api.web;

import com.google.common.collect.Lists;

import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Definition of a filter.
 *
 * <p>Its name can be retrieved using the i18n mechanism, using the keys "filter.&lt;id&gt;.name".</p>
 *
 * @since 3.1
 */
public class Filter {
  public static final String LIST = "list";
  public static final String TREEMAP = "treemap";

  private boolean shared;
  private boolean favouritesOnly;
  private String defaultPeriod;
  private String resourceKeyLike;
  private String resourceNameLike;
  private String language;
  private String searchFor;
  private int pageSize;
  private List<Criterion> criteria;
  private List<FilterColumn> columns;

  private Filter() {
    criteria = Lists.newArrayList();
    columns = Lists.newArrayList();
  }

  /**
   * Creates a new {@link Filter}.
   */
  public static Filter create() {
    return new Filter();
  }

  public List<Criterion> getCriteria() {
    return criteria;
  }

  public Filter add(Criterion criterion) {
    this.criteria.add(criterion);
    return this;
  }

  public List<FilterColumn> getColumns() {
    return columns;
  }

  public Filter add(FilterColumn column) {
    this.columns.add(column);
    return this;
  }

  public boolean isShared() {
    return shared;
  }

  public Filter setShared(boolean shared) {
    this.shared = shared;
    return this;
  }

  public boolean isFavouritesOnly() {
    return favouritesOnly;
  }

  public Filter setFavouritesOnly(boolean favouritesOnly) {
    this.favouritesOnly = favouritesOnly;
    return this;
  }

  public String getDefaultPeriod() {
    return defaultPeriod;
  }

  public Filter setDefaultPeriod(String defaultPeriod) {
    Preconditions.checkArgument(LIST.equals(defaultPeriod) || TREEMAP.equals(defaultPeriod), "Default period should be either %s or %s, not %s", LIST, TREEMAP, defaultPeriod);
    this.defaultPeriod = defaultPeriod;
    return this;
  }

  public String getResourceKeyLike() {
    return resourceKeyLike;
  }

  public Filter setResourceKeyLike(String resourceKeyLike) {
    this.resourceKeyLike = resourceKeyLike;
    return this;
  }

  public String getResourceNameLike() {
    return resourceNameLike;
  }

  public Filter setResourceNameLike(String resourceNameLike) {
    this.resourceNameLike = resourceNameLike;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public Filter setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getSearchFor() {
    return searchFor;
  }

  public Filter setSearchFor(String searchFor) {
    this.searchFor = searchFor;
    return this;
  }

  public int getPageSize() {
    return pageSize;
  }

  public Filter setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }
}
