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
package org.sonar.server.common.user.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.server.exceptions.ServerException;

public class UsersSearchRequest {
  private final Integer page;
  private final Integer pageSize;
  private final String query;
  private final boolean deactivated;
  private final Boolean managed;
  private final OffsetDateTime lastConnectionDateFrom;
  private final OffsetDateTime lastConnectionDateTo;
  private final OffsetDateTime sonarLintLastConnectionDateFrom;
  private final OffsetDateTime sonarLintLastConnectionDateTo;
  private final String externalLogin;
  private final String groupUuid;
  private final String excludedGroupUuid;

  private UsersSearchRequest(Builder builder) {
    this.page = builder.page;
    this.pageSize = builder.pageSize;
    this.query = builder.query;
    this.deactivated = builder.deactivated;
    this.managed = builder.managed;
    this.externalLogin = builder.externalLogin;
    this.groupUuid = builder.groupUuid;
    this.excludedGroupUuid = builder.excludedGroupUuid;
    try {
      this.lastConnectionDateFrom = Optional.ofNullable(builder.lastConnectionDateFrom).map(DateUtils::parseOffsetDateTime).orElse(null);
      this.lastConnectionDateTo = Optional.ofNullable(builder.lastConnectionDateTo).map(DateUtils::parseOffsetDateTime).orElse(null);
      this.sonarLintLastConnectionDateFrom = Optional.ofNullable(builder.sonarLintLastConnectionDateFrom).map(DateUtils::parseOffsetDateTime).orElse(null);
      this.sonarLintLastConnectionDateTo = Optional.ofNullable(builder.sonarLintLastConnectionDateTo).map(DateUtils::parseOffsetDateTime).orElse(null);
    } catch (MessageException me) {
      throw new ServerException(400, me.getMessage());
    }
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public boolean isDeactivated() {
    return deactivated;
  }

  @CheckForNull
  public Boolean isManaged() {
    return managed;
  }

  public Optional<OffsetDateTime> getLastConnectionDateFrom() {
    return Optional.ofNullable(lastConnectionDateFrom);
  }

  public Optional<OffsetDateTime> getLastConnectionDateTo() {
    return Optional.ofNullable(lastConnectionDateTo);
  }

  public Optional<OffsetDateTime> getSonarLintLastConnectionDateFrom() {
    return Optional.ofNullable(sonarLintLastConnectionDateFrom);
  }

  public Optional<OffsetDateTime> getSonarLintLastConnectionDateTo() {
    return Optional.ofNullable(sonarLintLastConnectionDateTo);
  }

  public Optional<String> getExternalLogin() {
    return Optional.ofNullable(externalLogin);
  }

  public Optional<String> getGroupUuid() {
    return Optional.ofNullable(groupUuid);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<String> getExcludedGroupUuid() {
    return Optional.ofNullable(excludedGroupUuid);
  }

  public static class Builder {
    private Integer page;
    private Integer pageSize;
    private String query;
    private boolean deactivated;
    private Boolean managed;
    private String lastConnectionDateFrom;
    private String lastConnectionDateTo;
    private String sonarLintLastConnectionDateFrom;
    private String sonarLintLastConnectionDateTo;
    private String externalLogin;
    private String groupUuid;
    private String excludedGroupUuid;

    private Builder() {
      // enforce factory method use
    }

    public Builder setPage(Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setDeactivated(boolean deactivated) {
      this.deactivated = deactivated;
      return this;
    }

    public Builder setManaged(@Nullable Boolean managed) {
      this.managed = managed;
      return this;
    }

    public Builder setLastConnectionDateFrom(@Nullable String lastConnectionDateFrom) {
      this.lastConnectionDateFrom = lastConnectionDateFrom;
      return this;
    }

    public Builder setLastConnectionDateTo(@Nullable String lastConnectionDateTo) {
      this.lastConnectionDateTo = lastConnectionDateTo;
      return this;
    }

    public Builder setSonarLintLastConnectionDateFrom(@Nullable String sonarLintLastConnectionDateFrom) {
      this.sonarLintLastConnectionDateFrom = sonarLintLastConnectionDateFrom;
      return this;
    }

    public Builder setSonarLintLastConnectionDateTo(@Nullable String sonarLintLastConnectionDateTo) {
      this.sonarLintLastConnectionDateTo = sonarLintLastConnectionDateTo;
      return this;
    }

    public Builder setExternalLogin(@Nullable String externalLogin) {
      this.externalLogin = externalLogin;
      return this;
    }

    public Builder setGroupUuid(@Nullable String groupUuid) {
      this.groupUuid = groupUuid;
      return this;
    }

    public Builder setExcludedGroupUuid(@Nullable String excludedGroupUuid) {
      this.excludedGroupUuid = excludedGroupUuid;
      return this;
    }

    public UsersSearchRequest build() {
      return new UsersSearchRequest(this);
    }
  }
}
