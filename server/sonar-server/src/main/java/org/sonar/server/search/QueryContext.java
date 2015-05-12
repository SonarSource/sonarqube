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
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;

/**
 * Various Elasticsearch request options: paging, fields and facets
 *
 * @since 4.4
 */
public class QueryContext {

  public static final int DEFAULT_OFFSET = 0;
  public static final int DEFAULT_LIMIT = 10;
  public static final int MAX_LIMIT = 500;
  public static final boolean DEFAULT_FACET = false;

  private int offset = DEFAULT_OFFSET;
  private int limit = DEFAULT_LIMIT;
  private Set<String> facets = newLinkedHashSet();
  private Set<String> fieldsToReturn = newHashSet();
  private boolean scroll = false;
  private boolean showFullResult = false;
  private String userLogin;
  private Set<String> userGroups = newHashSet();

  public QueryContext(UserSession userSession) {
    this.userLogin = userSession.getLogin();
    this.userGroups = userSession.getUserGroups();
  }

  /**
   * Whether or not the search returns facets for the domain. Defaults to {@link #DEFAULT_FACET}
   */
  public boolean isFacet() {
    return !facets.isEmpty();
  }

  /**
   * Selects facets to return for the domain.
   */
  public QueryContext addFacets(Collection<String> facets) {
    this.facets.addAll(facets);
    return this;
  }

  /**
   * Lists selected facets.
   */
  public Collection<String> facets() {
    return facets;
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
  public QueryContext setScroll(boolean scroll) {
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
  public QueryContext setOffset(int offset) {
    Preconditions.checkArgument(offset >= 0, "Offset must be positive");
    this.offset = offset;
    return this;
  }

  /**
   * Set offset and limit according to page approach
   */
  public QueryContext setPage(int page, int pageSize) {
    Preconditions.checkArgument(page >= 1, "Page must be greater or equal to 1 (got " + page + ")");
    Preconditions.checkArgument(pageSize >= 0, "Page size must be greater or equal to 0 (got " + pageSize + ")");
    setLimit(pageSize);
    setOffset((page * getLimit()) - getLimit());
    return this;
  }

  public int getPage() {
    int currentLimit = getLimit();
    return currentLimit > 0 ? (int) Math.ceil((double) (getOffset() + 1) / (double) currentLimit) : 0;
  }

  /**
   * Limit on the number of results to return. Defaults to {@link #DEFAULT_LIMIT}.
   */
  public int getLimit() {
    return showFullResult ? 999999 : limit;
  }

  /**
   * Sets the limit on the number of results to return.
   */
  public QueryContext setLimit(int limit) {
    this.limit = Math.min(limit, MAX_LIMIT);
    return this;
  }

  public QueryContext setMaxLimit() {
    this.limit = MAX_LIMIT;
    return this;
  }

  /**
   * Careful use, this could lead to massive data transport !
   */
  public void setShowFullResult(boolean showFullResult) {
    this.showFullResult = showFullResult;
  }

  public Set<String> getFieldsToReturn() {
    return fieldsToReturn;
  }

  public QueryContext setFieldsToReturn(@Nullable Collection<String> c) {
    fieldsToReturn.clear();
    if (c != null) {
      this.fieldsToReturn = newHashSet(c);
    }
    return this;
  }

  public QueryContext addFieldsToReturn(@Nullable Collection<String> c) {
    if (c != null) {
      fieldsToReturn.addAll(c);
    }
    return this;
  }

  public QueryContext addFieldsToReturn(String... c) {
    return addFieldsToReturn(Arrays.asList(c));
  }

  public String getUserLogin() {
    return userLogin;
  }

  public Set<String> getUserGroups() {
    return userGroups;
  }
}
