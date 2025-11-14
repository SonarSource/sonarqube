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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.apache.commons.lang3.Strings.CS;

public class UserQuery {
  private final String searchText;
  private final Boolean isActive;
  private final String isManagedSqlClause;
  private final Long lastConnectionDateFrom;
  private final Long lastConnectionDateTo;
  private final Long sonarLintLastConnectionDateFrom;
  private final Long sonarLintLastConnectionDateTo;
  private final String externalLogin;
  private final String groupUuid;
  private final String excludedGroupUuid;
  private final Set<String> userUuids;

  private UserQuery(UserQuery userQuery, Collection<String> userUuids) {
    this.searchText = userQuery.getSearchText();
    this.isActive = userQuery.isActive();
    this.isManagedSqlClause = userQuery.getIsManagedSqlClause();
    this.lastConnectionDateFrom = userQuery.getLastConnectionDateFrom();
    this.lastConnectionDateTo = userQuery.getLastConnectionDateTo();
    this.sonarLintLastConnectionDateTo = userQuery.getSonarLintLastConnectionDateTo();
    this.sonarLintLastConnectionDateFrom = userQuery.getSonarLintLastConnectionDateFrom();
    this.externalLogin = userQuery.externalLogin;
    this.groupUuid = userQuery.groupUuid;
    this.excludedGroupUuid = userQuery.excludedGroupUuid;
    this.userUuids = new HashSet<>(userUuids);
  }

  private UserQuery(@Nullable String searchText, @Nullable Boolean isActive, @Nullable String isManagedSqlClause,
    @Nullable OffsetDateTime lastConnectionDateFrom, @Nullable OffsetDateTime lastConnectionDateTo,
    @Nullable OffsetDateTime sonarLintLastConnectionDateFrom, @Nullable OffsetDateTime sonarLintLastConnectionDateTo, @Nullable String externalLogin,
    @Nullable String groupUuid, @Nullable String excludedGroupUuid, @Nullable Set<String> userUuids) {
    this.searchText = searchTextToSearchTextSql(searchText);
    this.isActive = isActive;
    this.isManagedSqlClause = isManagedSqlClause;
    this.lastConnectionDateFrom = parseDateToLong(lastConnectionDateFrom);
    this.lastConnectionDateTo = formatDateToInput(lastConnectionDateTo);
    this.sonarLintLastConnectionDateFrom = parseDateToLong(sonarLintLastConnectionDateFrom);
    this.sonarLintLastConnectionDateTo = formatDateToInput(sonarLintLastConnectionDateTo);
    this.externalLogin = externalLogin;
    this.groupUuid = groupUuid;
    this.excludedGroupUuid = excludedGroupUuid;
    this.userUuids = userUuids;
  }

  public static UserQuery copyWithNewRangeOfUserUuids(UserQuery userQuery, Collection<String> userUuids) {
    return new UserQuery(userQuery, userUuids);
  }

  private static Long formatDateToInput(@Nullable OffsetDateTime dateTo) {
    if (dateTo == null) {
      return null;
    } else {
      // add 1 second to include all timestamp at the second precision.
      return dateTo.toInstant().plus(1, ChronoUnit.SECONDS).toEpochMilli();
    }
  }

  private static Long parseDateToLong(@Nullable OffsetDateTime date) {
    if (date == null) {
      return null;
    } else {
      return date.toInstant().toEpochMilli();
    }
  }

  private static String searchTextToSearchTextSql(@Nullable String text) {
    String sql = null;
    if (text != null) {
      sql = CS.replace(text, "%", "/%");
      sql = CS.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  @CheckForNull
  public String getSearchText() {
    return searchText;
  }

  @CheckForNull
  public Boolean isActive() {
    return isActive;
  }

  @CheckForNull
  public String getIsManagedSqlClause() {
    return isManagedSqlClause;
  }

  @CheckForNull
  public Long getLastConnectionDateFrom() {
    return lastConnectionDateFrom;
  }

  @CheckForNull
  public Long getLastConnectionDateTo() {
    return lastConnectionDateTo;
  }

  @CheckForNull
  public Long getSonarLintLastConnectionDateFrom() {
    return sonarLintLastConnectionDateFrom;
  }

  @CheckForNull
  public Long getSonarLintLastConnectionDateTo() {
    return sonarLintLastConnectionDateTo;
  }

  @CheckForNull
  public String getExternalLogin() {
    return externalLogin;
  }

  @CheckForNull
  public Set<String> getUserUuids() {
    return userUuids;
  }

  @CheckForNull
  private String getGroupUuid() {
    return groupUuid;
  }

  @CheckForNull
  private String getExcludedGroupUuid() {
    return excludedGroupUuid;
  }

  public static UserQueryBuilder builder() {
    return new UserQueryBuilder();
  }

  public static final class UserQueryBuilder {
    private String searchText = null;
    private Boolean isActive = null;
    private String isManagedSqlClause = null;
    private OffsetDateTime lastConnectionDateFrom = null;
    private OffsetDateTime lastConnectionDateTo = null;
    private OffsetDateTime sonarLintLastConnectionDateFrom = null;
    private OffsetDateTime sonarLintLastConnectionDateTo = null;
    private String externalLogin = null;
    private String groupUuid = null;
    private String excludedGroupUuid;
    private Set<String> userUuids = null;

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

    public UserQueryBuilder isManagedClause(@Nullable String isManagedSqlClause) {
      this.isManagedSqlClause = isManagedSqlClause;
      return this;
    }

    public UserQueryBuilder lastConnectionDateFrom(@Nullable OffsetDateTime lastConnectionDateFrom) {
      this.lastConnectionDateFrom = lastConnectionDateFrom;
      return this;
    }

    public UserQueryBuilder lastConnectionDateTo(@Nullable OffsetDateTime lastConnectionDateTo) {
      this.lastConnectionDateTo = lastConnectionDateTo;
      return this;
    }

    public UserQueryBuilder sonarLintLastConnectionDateFrom(@Nullable OffsetDateTime sonarLintLastConnectionDateFrom) {
      this.sonarLintLastConnectionDateFrom = sonarLintLastConnectionDateFrom;
      return this;
    }

    public UserQueryBuilder sonarLintLastConnectionDateTo(@Nullable OffsetDateTime sonarLintLastConnectionDateTo) {
      this.sonarLintLastConnectionDateTo = sonarLintLastConnectionDateTo;
      return this;
    }

    public UserQueryBuilder externalLogin(@Nullable String externalLogin) {
      this.externalLogin = externalLogin;
      return this;
    }

    public UserQueryBuilder groupUuid(@Nullable String groupUuid) {
      this.groupUuid = groupUuid;
      return this;
    }

    public UserQueryBuilder excludedGroupUuid(@Nullable String excludedGroupUuid) {
      this.excludedGroupUuid = excludedGroupUuid;
      return this;
    }

    public UserQueryBuilder userUuids(@Nullable Set<String> userUuids) {
      this.userUuids = userUuids;
      return this;
    }

    public UserQuery build() {
      return new UserQuery(
        searchText, isActive, isManagedSqlClause, lastConnectionDateFrom, lastConnectionDateTo,
        sonarLintLastConnectionDateFrom, sonarLintLastConnectionDateTo, externalLogin, groupUuid, excludedGroupUuid, userUuids);
    }
  }
}
