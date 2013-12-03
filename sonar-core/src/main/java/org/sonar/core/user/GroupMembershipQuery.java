/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.user;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Set;

/**
 * @since 4.1
 */
public class GroupMembershipQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public static final String ALL = "ALL";
  public static final String MEMBER_ONLY = "MEMBER_ONLY";
  public static final String NOT_MEMBER = "NOT_MEMBER";
  public static final Set<String> AVAILABLE_MEMBERSHIP = ImmutableSet.of(ALL, MEMBER_ONLY, NOT_MEMBER);

  private final Long userId;
  private final String memberShip;

  private final String searchText;

  // for internal use in MyBatis
  final String searchTextSql;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;


  private GroupMembershipQuery(Builder builder) {
    this.userId = builder.userId;
    this.memberShip = builder.memberShip;
    this.searchText = builder.searchText;
    this.searchTextSql = searchTextToSql(searchText);

    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  private String searchTextToSql(@Nullable String s) {
    String sql = null;
    if (s != null) {
      sql = StringUtils.replace(s, "%", "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  public Long userId() {
    return userId;
  }

  @CheckForNull
  public String memberShip() {
    return memberShip;
  }

  /**
   * Search for groups or names containing a given string
   */
  @CheckForNull
  public String searchText() {
    return searchText;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageIndex() {
    return pageIndex;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long userId;
    private String memberShip = GroupMembershipQuery.ALL;
    private String searchText;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
    }

    public Builder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder memberShip(@Nullable String memberShip) {
      this.memberShip = memberShip;
      return this;
    }

    public Builder searchText(@Nullable String s) {
      this.searchText = StringUtils.defaultIfBlank(s, null);
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

    private void initPageIndex() {
      Preconditions.checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }

    public GroupMembershipQuery build() {
      Preconditions.checkNotNull(userId, "User id cant be null.");
      return new GroupMembershipQuery(this);
    }
  }
}
