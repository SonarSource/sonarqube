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

package org.sonar.db.permission;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Query used to get users and groups from a permission
 */
public class PermissionQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public static final String ANY = "ANY";
  public static final String IN = "IN";
  public static final String OUT = "OUT";
  public static final Set<String> AVAILABLE_MEMBERSHIP = ImmutableSet.of(ANY, IN, OUT);

  private final String permission;
  private final String component;
  private final String template;
  private final String membership;
  private final String search;

  // for internal use in MyBatis
  final String searchSql;

  // max results per page
  private final int pageSize;
  // index of selected page. Start with 1.
  private final int pageIndex;
  // offset. Starts with 0.
  private final int pageOffset;

  private PermissionQuery(Builder builder) {
    this.permission = builder.permission;
    this.component = builder.component;
    this.template = builder.template;
    this.membership = builder.membership;
    this.search = builder.search;
    this.searchSql = searchToSql(search);

    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
    this.pageOffset = (builder.pageIndex - 1) * builder.pageSize;
  }

  private static String searchToSql(@Nullable String s) {
    String sql = null;
    if (s != null) {
      sql = StringUtils.replace(StringUtils.upperCase(s), "%", "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  public String permission() {
    return permission;
  }

  /**
   * Used only for permission template
   */
  public String template() {
    return template;
  }

  /**
   * Used on project permission
   */
  @CheckForNull
  public String component() {
    return component;
  }

  @CheckForNull
  public String membership() {
    return membership;
  }

  @CheckForNull
  public String search() {
    return search;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageOffset() {
    return pageOffset;
  }

  public int pageIndex() {
    return pageIndex;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String permission;
    private String component;
    private String template;
    private String membership;
    private String search;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
    }

    public Builder permission(String permission) {
      this.permission = permission;
      return this;
    }

    public Builder template(String template) {
      this.template = template;
      return this;
    }

    public Builder component(@Nullable String component) {
      this.component = component;
      return this;
    }

    public Builder membership(@Nullable String membership) {
      this.membership = membership;
      return this;
    }

    public Builder search(@Nullable String s) {
      this.search = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    public Builder pageSize(@Nullable Integer i) {
      this.pageSize = i;
      return this;
    }

    public Builder pageIndex(@Nullable Integer i) {
      this.pageIndex = i;
      return this;
    }

    private void initMembership() {
      if (membership == null) {
        membership = PermissionQuery.ANY;
      } else {
        Preconditions.checkArgument(AVAILABLE_MEMBERSHIP.contains(membership),
          "Membership is not valid (got " + membership + "). Availables values are " + AVAILABLE_MEMBERSHIP);
      }
    }

    private void initPageSize() {
      if (pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
    }

    private void initPageIndex() {
      if (pageIndex == null) {
        pageIndex = DEFAULT_PAGE_INDEX;
      }
      Preconditions.checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }

    public PermissionQuery build() {
      checkNotNull(permission, "Permission cannot be null.");
      initMembership();
      initPageIndex();
      initPageSize();
      return new PermissionQuery(this);
    }
  }
}
