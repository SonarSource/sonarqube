/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.organization;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.core.util.stream.MoreCollectors.toSet;

public class OrganizationQuery {
  private static final OrganizationQuery NO_FILTER = newOrganizationQueryBuilder().build();
  private final Set<String> keys;
  @Nullable
  private final Integer userId;
  private final boolean withAnalyses;
  @Nullable
  private final Long analyzedAfter;

  private OrganizationQuery(Builder builder) {
    this.keys = builder.keys;
    this.userId = builder.member;
    this.withAnalyses = builder.withAnalyses;
    this.analyzedAfter = builder.analyzedAfter;
  }

  @CheckForNull
  public Set<String> getKeys() {
    return keys;
  }

  @CheckForNull
  public Integer getMember() {
    return userId;
  }

  public boolean isWithAnalyses() {
    return withAnalyses;
  }

  @CheckForNull
  public Long getAnalyzedAfter() {
    return analyzedAfter;
  }

  public static OrganizationQuery returnAll() {
    return NO_FILTER;
  }

  public static Builder newOrganizationQueryBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Set<String> keys;
    @Nullable
    private Integer member;
    private boolean withAnalyses = false;
    @Nullable
    private Long analyzedAfter;

    private Builder() {
      // use static factory method
    }

    public Builder setKeys(@Nullable Collection<String> keys) {
      if (keys != null && !keys.isEmpty()) {
        this.keys = keys.stream()
          .filter(Objects::nonNull)
          .collect(toSet(keys.size()));
      }
      return this;
    }

    public Builder setMember(@Nullable Integer userId) {
      this.member = userId;
      return this;
    }

    public Builder setWithAnalyses() {
      this.withAnalyses = true;
      return this;
    }

    public Builder setAnalyzedAfter(long l) {
      this.analyzedAfter = l;
      return this;
    }

    public OrganizationQuery build() {
      return new OrganizationQuery(this);
    }
  }
}
