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
package org.sonar.db.permission;

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.db.DatabaseUtils.buildLikeValue;

/**
 * Query used to get users and groups permissions
 */
@Immutable
public class PermissionQuery {
  public static final int RESULTS_MAX_SIZE = 100;
  public static final int SEARCH_QUERY_MIN_LENGTH = 3;
  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int DEFAULT_PAGE_INDEX = 1;

  private final String permission;
  private final String componentUuid;
  private final String template;
  private final String searchQuery;
  private final String searchQueryToSql;
  private final boolean withPermissionOnly;

  private final int pageSize;
  private final int pageOffset;

  private PermissionQuery(Builder builder) {
    this.permission = builder.permission;
    this.withPermissionOnly = builder.withPermissionOnly;
    this.componentUuid = builder.componentUuid;
    this.template = builder.template;
    this.searchQuery = builder.searchQuery;
    this.searchQueryToSql = builder.searchQuery == null ? null : buildLikeValue(builder.searchQuery, WildcardPosition.BEFORE_AND_AFTER).toLowerCase(Locale.ENGLISH);
    this.pageSize = builder.pageSize;
    this.pageOffset = offset(builder.pageIndex, builder.pageSize);
  }

  @CheckForNull
  public String getPermission() {
    return permission;
  }

  public boolean withPermissionOnly() {
    return withPermissionOnly;
  }

  // TODO remove it, it should not be in the query, but set as a separate parameter
  @Deprecated
  public String template() {
    return template;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  @CheckForNull
  public String getSearchQuery() {
    return searchQuery;
  }

  @CheckForNull
  public String getSearchQueryToSql() {
    return searchQueryToSql;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPageOffset() {
    return pageOffset;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String permission;
    private String componentUuid;
    private String template;
    private String searchQuery;
    private boolean withPermissionOnly;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
      // enforce method constructor
    }

    public Builder setPermission(@Nullable String permission) {
      this.withPermissionOnly = permission != null;
      this.permission = permission;
      return this;
    }

    public Builder setTemplate(@Nullable String template) {
      this.template = template;
      return this;
    }

    public Builder setComponentUuid(@Nullable String componentUuid) {
      this.componentUuid = componentUuid;
      return this;
    }

    public Builder setSearchQuery(@Nullable String s) {
      this.searchQuery = defaultIfBlank(s, null);
      return this;
    }

    public Builder setPageIndex(@Nullable Integer i) {
      this.pageIndex = i;
      return this;
    }

    public Builder setPageSize(@Nullable Integer i) {
      this.pageSize = i;
      return this;
    }

    public Builder withPermissionOnly() {
      this.withPermissionOnly = true;
      return this;
    }

    public PermissionQuery build() {
      this.pageIndex = firstNonNull(pageIndex, DEFAULT_PAGE_INDEX);
      this.pageSize = firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
      checkArgument(searchQuery == null || searchQuery.length() >= SEARCH_QUERY_MIN_LENGTH, "Search query should contains at least %s characters", SEARCH_QUERY_MIN_LENGTH);
      return new PermissionQuery(this);
    }
  }
}
