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
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Various Elasticsearch request options: paging, sorting, fields and facets
 *
 * @since 4.4
 */
public class QueryOptions {

  public static final int DEFAULT_OFFSET = 0;
  public static final int DEFAULT_LIMIT = 10;
  public static final int MAX_LIMIT = 500;
  public static final boolean DEFAULT_FACET = false;

  private int offset = DEFAULT_OFFSET;
  private int limit = DEFAULT_LIMIT;
  private boolean facet = DEFAULT_FACET;
  private Set<String> fieldsToReturn = Sets.newHashSet();
  private boolean scroll = false;

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
   * Whether or not the search result will be scrollable using an iterator
   */
  public boolean isScroll() {
    return scroll;
  }

  /**
   * Sets whether or not the search result will be scrollable using an iterator
   */
  public QueryOptions setScroll(boolean scroll) {
    this.scroll = scroll;
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
    Preconditions.checkArgument(page >= 1, "Page must be greater or equal to 1 (got " + page + ")");
    Preconditions.checkArgument(pageSize >= 0, "Page size must be greater or equal to 0 (got " + pageSize + ")");
    setLimit(pageSize);
    setOffset((page * getLimit()) - getLimit());
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
    this.limit = Math.min(limit, MAX_LIMIT);
    return this;
  }

  public QueryOptions setMaxLimit() {
    this.limit = MAX_LIMIT;
    return this;
  }

  public Set<String> getFieldsToReturn() {
    return fieldsToReturn;
  }

  public QueryOptions setFieldsToReturn(@Nullable Collection<String> c) {
    fieldsToReturn.clear();
    if (c != null) {
      this.fieldsToReturn = Sets.newHashSet(c);
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
    return addFieldsToReturn(Arrays.asList(c));
  }
}
