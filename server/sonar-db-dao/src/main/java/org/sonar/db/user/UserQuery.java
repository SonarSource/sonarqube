/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class UserQuery {
  private final String searchText;
  private final Boolean isActive;

  public UserQuery(@Nullable String searchText, @Nullable Boolean isActive) {
    this.searchText = searchTextToSearchTextSql(searchText);
    this.isActive = isActive;
  }

  private static String searchTextToSearchTextSql(@Nullable String text) {
    String sql = null;
    if (text != null) {
      sql = StringUtils.replace(text, "%", "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  @CheckForNull
  private String getSearchText() {
    return searchText;
  }

  @CheckForNull
  private Boolean isActive() {
    return isActive;
  }

  public static UserQueryBuilder builder() {
    return new UserQueryBuilder();
  }

  public static final class UserQueryBuilder {
    private String searchText;
    private Boolean isActive;

    private UserQueryBuilder() {
    }

    public UserQueryBuilder searchText(@Nullable String searchText) {
      this.searchText = searchText;
      return this;
    }

    public UserQueryBuilder isActive(@Nullable Boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public UserQuery build() {
      return new UserQuery(searchText, isActive);
    }
  }
}
