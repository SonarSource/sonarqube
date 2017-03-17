/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.user.index;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.apache.commons.lang.StringUtils.isBlank;

@Immutable
public class UserQuery {
  private final String textQuery;
  private final List<String> logins;
  private final List<String> excludedLogins;

  private UserQuery(Builder builder) {
    this.textQuery = builder.textQuery;
    this.logins = builder.logins == null ? null : ImmutableList.copyOf(builder.logins);
    this.excludedLogins = builder.excludedLogins == null ? null : ImmutableList.copyOf(builder.excludedLogins);
  }

  public Optional<String> getTextQuery() {
    return Optional.ofNullable(textQuery);
  }

  public Optional<List<String>> getLogins() {
    return Optional.ofNullable(logins);
  }

  public Optional<List<String>> getExcludedLogins() {
    return Optional.ofNullable(excludedLogins);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String textQuery;
    private List<String> logins;
    private List<String> excludedLogins;

    private Builder() {
      // enforce factory method
    }

    public UserQuery build() {
      return new UserQuery(this);
    }

    public Builder setTextQuery(@Nullable String textQuery) {
      this.textQuery = isBlank(textQuery) ? null : textQuery;
      return this;
    }

    public Builder setLogins(@Nullable List<String> logins) {
      this.logins = logins;
      return this;
    }

    public Builder setExcludedLogins(@Nullable List<String> excludedLogins) {
      this.excludedLogins = excludedLogins;
      return this;
    }
  }
}
