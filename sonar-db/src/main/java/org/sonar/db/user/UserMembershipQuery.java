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
package org.sonar.db.user;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class UserMembershipQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public static final String ANY = "ANY";
  public static final String IN = "IN";
  public static final String OUT = "OUT";
  public static final Set<String> AVAILABLE_MEMBERSHIPS = ImmutableSet.of(ANY, IN, OUT);

  private final Long groupId;
  private final String membership;

  private final String memberSearch;

  // for internal use in MyBatis
  final String memberSearchSql;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;

  private UserMembershipQuery(Builder builder) {
    this.groupId = builder.groupId;
    this.membership = builder.membership;
    this.memberSearch = builder.memberSearch;
    this.memberSearchSql = memberSearchToSql(memberSearch);

    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  private String memberSearchToSql(@Nullable String s) {
    String sql = null;
    if (s != null) {
      sql = StringUtils.replace(StringUtils.upperCase(s), "%", "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  public Long groupId() {
    return groupId;
  }

  @CheckForNull
  public String membership() {
    return membership;
  }

  /**
   * Search for users names/logins containing a given string
   */
  @CheckForNull
  public String memberSearch() {
    return memberSearch;
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
    private Long groupId;
    private String membership;
    private String memberSearch;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
    }

    public Builder groupId(Long groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder membership(@Nullable String membership) {
      this.membership = membership;
      return this;
    }

    public Builder memberSearch(@Nullable String s) {
      this.memberSearch = StringUtils.defaultIfBlank(s, null);
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
      membership = firstNonNull(membership, ANY);
      checkArgument(AVAILABLE_MEMBERSHIPS.contains(membership),
        "Membership is not valid (got " + membership + "). Availables values are " + AVAILABLE_MEMBERSHIPS);
    }

    private void initPageSize() {
      pageSize = firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
    }

    private void initPageIndex() {
      pageIndex = firstNonNull(pageIndex, DEFAULT_PAGE_INDEX);
      checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }

    public UserMembershipQuery build() {
      checkNotNull(groupId, "Group ID cant be null.");
      initMembership();
      initPageIndex();
      initPageSize();
      return new UserMembershipQuery(this);
    }
  }
}
