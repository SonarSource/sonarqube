/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DaoUtils;
import org.sonar.db.WildcardPosition;

public class GroupQuery {
  private final String searchText;
  private final String isManagedSqlClause;

  GroupQuery(@Nullable String searchText, @Nullable String isManagedSqlClause) {
    this.searchText = searchTextToSearchTextSql(searchText);
    this.isManagedSqlClause = isManagedSqlClause;
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

  public static GroupQueryBuilder builder() {
    return new GroupQueryBuilder();
  }

  public static final class GroupQueryBuilder {
    private String searchText = null;
    private String isManagedSqlClause = null;

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

    public GroupQuery build() {
      return new GroupQuery(searchText, isManagedSqlClause);
    }
  }
}
