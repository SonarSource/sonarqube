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

/**
 * Options about paging, sorting and fields to return
 */
public class QueryOptions {

  public static final int DEFAULT_OFFSET = 0;
  public static final int DEFAULT_LIMIT = 10;
  public static final boolean DEFAULT_ASCENDING = true;

  private int offset = DEFAULT_OFFSET;
  private int limit = DEFAULT_LIMIT;
  private boolean ascending = DEFAULT_ASCENDING;
  private String sortField;
  private String[] fieldsToReturn;

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

  /**
   * Is ascending sort ? Defaults to {@link #DEFAULT_ASCENDING}
   */
  public boolean isAscending() {
    return ascending;
  }

  public QueryOptions setAscending(boolean ascending) {
    this.ascending = ascending;
    return this;
  }

  @CheckForNull
  public String getSortField() {
    return sortField;
  }

  public QueryOptions setSortField(@Nullable String sortField) {
    this.sortField = sortField;
    return this;
  }

  @CheckForNull
  public String[] getFieldsToReturn() {
    return fieldsToReturn;
  }

  public QueryOptions setFieldsToReturn(@Nullable String[] fieldsToReturn) {
    this.fieldsToReturn = fieldsToReturn;
    return this;
  }
}
