/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
public final class Filter {
  public static final String LIST = "list";
  public static final String TREEMAP = "treemap";

  private boolean favouritesOnly;
  private String displayAs;
  private int pageSize;
  private List<Criterion> criteria;
  private List<FilterColumn> columns;

  private Filter() {
    displayAs = LIST;
    criteria = Lists.newArrayList();
    columns = Lists.newArrayList();
  }

  /**
   * Creates a new {@link Filter}.
   */
  public static Filter create() {
    return new Filter();
  }

  /**
   * Get the list of {@link Criterion} used to narrow down the results of this {@link Filter}.
   * 
   * @return the criteria
   */
  public List<Criterion> getCriteria() {
    return criteria;
  }

  /**
   * Add a {@link Criterion} to the list used to narrow down the results of this {@link Filter}.
   * 
   * @return this filter
   */
  public Filter add(Criterion criterion) {
    this.criteria.add(criterion);
    return this;
  }

  /**
   * Get the list of {@link FilterColumn} displayed by this {@link Filter}.
   * 
   * @return this columns
   */
  public List<FilterColumn> getColumns() {
    return columns;
  }

  /**
   * Add a {@link FilterColumn} to the list of columns displayed by this {@link Filter}.
   * 
   * @return this filter
   */
  public Filter add(FilterColumn column) {
    this.columns.add(column);
    return this;
  }

  /**
   * The {@link Filter} can be configured to return only favourites.
   * 
   * @return <code>true</code> if favourites only are returned
   */
  public boolean isFavouritesOnly() {
    return favouritesOnly;
  }

  /**
   * The {@link Filter} can be configured to return only favourites.
   */
  public Filter setFavouritesOnly(boolean favouritesOnly) {
    this.favouritesOnly = favouritesOnly;
    return this;
  }

  /**
   * Get the type of display used by this {@link Filter}.
   * 
   * <p>Can be either {@code #LIST} or {@code #TREEMAP}</p>
   * 
   * @return the display type
   */
  public String getDisplayAs() {
    return displayAs;
  }

  /**
   * Set the type of display used by this {@link Filter}.
   * 
   * <p>Can be either {@code #LIST} or {@code #TREEMAP}</p>
   * 
   * @return this filter
   * @throws IllegalArgumentException if {@code displayAs} is not {@code #LIST} or {@code #TREEMAP}
   */
  public Filter setDisplayAs(String displayAs) {
    Preconditions.checkArgument(LIST.equals(displayAs) || TREEMAP.equals(displayAs), "Default display should be either %s or %s, not %s", LIST, TREEMAP, displayAs);
    this.displayAs = displayAs;
    return this;
  }

  /**
   * Get the size of a page displayed this {@link Filter}.
   * 
   * <p>The page size is between <code>20</code> and <code>200</code> (included)</p>
   * 
   * @return the display type
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Set the size of a page displayed this {@link Filter}.
   * 
   * <p>The page size should be between <code>20</code> and <code>200</code> (included)</p>
   * 
   * @return the display type
   * @throws IllegalArgumentException if {@code pageSize} is not lower than {@code 20} or greater than {@code 200}
   */
  public Filter setPageSize(int pageSize) {
    Preconditions.checkArgument((pageSize >= 20) && (pageSize <= 200), "page size should be between 20 and 200");
    this.pageSize = pageSize;
    return this;
  }
}
