/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.user;

import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

/**
 * @since 3.6
 */
public class UserQuery {

  public static final UserQuery ALL_ACTIVES = UserQuery.builder().build();

  private final Collection<String> logins;
  private final boolean includeDeactivated;
  private final String searchText;
  private final Boolean mustBeRoot;

  // for internal use in MyBatis
  final String searchTextSql;

  private UserQuery(Builder builder) {
    this.logins = builder.logins;
    this.includeDeactivated = builder.includeDeactivated;
    this.searchText = builder.searchText;
    this.mustBeRoot = builder.mustBeRoot;

    this.searchTextSql = searchTextToSql(searchText);
  }

  private static String searchTextToSql(@Nullable String s) {
    String sql = null;
    if (s != null) {
      sql = StringUtils.replace(s, "%", "/%");
      sql = StringUtils.replace(sql, "_", "/_");
      sql = "%" + sql + "%";
    }
    return sql;
  }

  @CheckForNull
  public Collection<String> logins() {
    return logins;
  }

  public boolean includeDeactivated() {
    return includeDeactivated;
  }

  /**
   * Search for logins or names containing a given string
   */
  @CheckForNull
  public String searchText() {
    return searchText;
  }

  @CheckForNull
  public Boolean mustBeRoot() {
    return mustBeRoot;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean includeDeactivated = false;
    private Collection<String> logins;
    private String searchText;
    private Boolean mustBeRoot;

    private Builder() {
    }

    public Builder includeDeactivated() {
      this.includeDeactivated = true;
      return this;
    }

    public Builder logins(@Nullable Collection<String> logins) {
      // TODO clone logins
      this.logins = logins;
      return this;
    }

    public Builder logins(String... logins) {
      this.logins = Arrays.asList(logins);
      return this;
    }

    public Builder searchText(@Nullable String s) {
      this.searchText = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    public Builder mustBeRoot() {
      this.mustBeRoot = true;
      return this;
    }

    public Builder mustNotBeRoot() {
      this.mustBeRoot = false;
      return this;
    }

    public UserQuery build() {
      if (logins != null && logins.size() >= 1000) {
        throw new IllegalArgumentException("Max number of logins is 1000. Got " + logins.size());
      }
      return new UserQuery(this);
    }
  }
}
