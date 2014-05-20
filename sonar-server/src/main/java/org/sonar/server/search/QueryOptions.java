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
package org.sonar.server.search;

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Options about paging, sorting and fields to return
 *
 * @since 4.4
 */
public class QueryOptions {

  public static final QueryOptions DEFAULT = new QueryOptions();

  public static final int DEFAULT_OFFSET = 0;
  public static final int DEFAULT_LIMIT = 10;
  public static final boolean DEFAULT_FACET = true;

  private int offset = DEFAULT_OFFSET;
  private int limit = DEFAULT_LIMIT;

  private boolean facet = DEFAULT_FACET;

  private Set<String> fieldsToReturn = new HashSet<String>();

  /**
   * Whether or not the search returns facets for the domain. Defaults to {@link #DEFAULT_OFFSET}
   */
  public boolean isFacet() {
    return facet;
  }

  /**
   * Sets whether or not the search returns facets for the domain.
   */
  public QueryOptions setFacet(boolean facet) {
    this.facet = facet;
    return this;
  }

  /**
   * Offset of the first result to return. Defaults to {@link #DEFAULT_OFFSET}
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Sets the offset of the first result to return (zero-based).
   */
  public QueryOptions setOffset(int offset) {
    Preconditions.checkArgument(offset >= 0, "Offset must be positive");
    this.offset = offset;
    return this;
  }

  /**
   * Set offset and limit according to page approach
   */
  public QueryOptions setPage(int page, int pageSize) {
    Preconditions.checkArgument(page > 0, "Page must be positive");
    Preconditions.checkArgument(pageSize >= 0, "Page size must be positive or greater than 0");
    this.offset = (page * pageSize) - pageSize;
    this.limit = pageSize;
    return this;
  }

  /**
   * Limit on the number of results to return. Defaults to {@link #DEFAULT_LIMIT}.
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Sets the limit on the number of results to return.
   */
  public QueryOptions setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  @CheckForNull
  public Set<String> getFieldsToReturn() {
    return fieldsToReturn;
  }

  public QueryOptions setFieldsToReturn(@Nullable Collection<String> c) {
    this.fieldsToReturn.clear();
    if (c != null) {
      this.fieldsToReturn.addAll(c);
    }
    return this;
  }

  public QueryOptions addFieldsToReturn(@Nullable Collection<String> c) {
    if (c != null) {
      fieldsToReturn.addAll(c);
    }
    return this;
  }

  public QueryOptions addFieldsToReturn(String... c) {
    fieldsToReturn.addAll(Arrays.asList(c));
    return this;
  }

  public boolean hasFieldToReturn(String key) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(key);
  }
}
