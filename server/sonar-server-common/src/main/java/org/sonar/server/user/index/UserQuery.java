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
package org.sonar.server.user.index;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.apache.commons.lang.StringUtils.isBlank;

@Immutable
public class UserQuery {
  private final String textQuery;
  private final String organizationUuid;
  private final String excludedOrganizationUuid;
  private List<String> organizationUuids;
  private final boolean active;

  private UserQuery(Builder builder) {
    this.textQuery = builder.textQuery;
    this.organizationUuid = builder.organizationUuid;
    this.excludedOrganizationUuid = builder.excludedOrganizationUuid;
    this.organizationUuids = ImmutableList.copyOf(builder.organizationUuids);
    this.active = builder.active;
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

  public List<String> getOrganizationUuids() {
    return organizationUuids;
  }

  public boolean isActive() {
    return active;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String textQuery;
    private String organizationUuid;
    private String excludedOrganizationUuid;
    private List<String> organizationUuids = new ArrayList<>();
    private boolean active = true;

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

    /**
     * Include only users that are members of at least one of the OrganizationUuids
     */
    public Builder addOrganizationUuids(List<String> organizationUuids) {
      this.organizationUuids.addAll(organizationUuids);
      return this;
    }

    public Builder setActive(boolean active) {
      this.active = active;
      return this;
    }
  }
}
