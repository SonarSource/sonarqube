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
package org.sonar.server.user.index;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.apache.commons.lang.StringUtils.isBlank;

@Immutable
public class UserQuery {
  private final String textQuery;
  private final String organizationUuid;
  private final String excludedOrganizationUuid;

  private UserQuery(Builder builder) {
    this.textQuery = builder.textQuery;
    this.organizationUuid = builder.organizationUuid;
    this.excludedOrganizationUuid = builder.excludedOrganizationUuid;
  }

  public Optional<String> getTextQuery() {
    return Optional.ofNullable(textQuery);
  }

  public Optional<String> getOrganizationUuid() {
    return Optional.ofNullable(organizationUuid);
  }

  public Optional<String> getExcludedOrganizationUuid() {
    return Optional.ofNullable(excludedOrganizationUuid);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String textQuery;
    private String organizationUuid;
    private String excludedOrganizationUuid;

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

    /**
     * Include only users that are members of the organizationUuid
     */
    public Builder setOrganizationUuid(@Nullable String organizationUuid) {
      this.organizationUuid = organizationUuid;
      return this;
    }

    /**
     * Include only users that are not members of the excludedOrganizationUuid
     */
    public Builder setExcludedOrganizationUuid(@Nullable String excludedOrganizationUuid) {
      this.excludedOrganizationUuid = excludedOrganizationUuid;
      return this;
    }
  }
}
