/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.DaoUtils;
import org.sonar.db.WildcardPosition;

public class GroupQuery {
  private final String searchText;
  private final String isManagedSqlClause;
  private final String userId;
  private final String excludedUserId;

  GroupQuery(@Nullable String searchText, @Nullable String isManagedSqlClause, String userId, String excludedUserId) {
    this.searchText = searchTextToSearchTextSql(searchText);
    this.isManagedSqlClause = isManagedSqlClause;
    this.userId = userId;
    this.excludedUserId = excludedUserId;
  }

  private static String searchTextToSearchTextSql(@Nullable String text) {
    if (text == null) {
      return null;
    }

    String upperCasedNameQuery = StringUtils.upperCase(text, Locale.ENGLISH);
    return DaoUtils.buildLikeValue(upperCasedNameQuery, WildcardPosition.BEFORE_AND_AFTER);
  }

  @CheckForNull
  public String getSearchText() {
    return searchText;
  }

  @CheckForNull
  public String getIsManagedSqlClause() {
    return isManagedSqlClause;
  }

  @CheckForNull
  public String getUserId() {
    return userId;
  }

  @CheckForNull
  public String getExcludedUserId() {
    return excludedUserId;
  }

  public static GroupQueryBuilder builder() {
    return new GroupQueryBuilder();
  }

  public static final class GroupQueryBuilder {
    private String searchText = null;
    private String isManagedSqlClause = null;
    private String userId = null;
    private String excludedUserId = null;

    private GroupQueryBuilder() {
    }

    public GroupQuery.GroupQueryBuilder searchText(@Nullable String searchText) {
      this.searchText = searchText;
      return this;
    }

    public GroupQuery.GroupQueryBuilder isManagedClause(@Nullable String isManagedSqlClause) {
      this.isManagedSqlClause = isManagedSqlClause;
      return this;
    }

    public GroupQuery.GroupQueryBuilder userId(@Nullable String userId) {
      this.userId = userId;
      return this;
    }

    public GroupQuery.GroupQueryBuilder excludedUserId(@Nullable String excludedUserId) {
      this.excludedUserId = excludedUserId;
      return this;
    }

    public GroupQuery build() {
      return new GroupQuery(searchText, isManagedSqlClause, userId, excludedUserId);
    }
  }
}
