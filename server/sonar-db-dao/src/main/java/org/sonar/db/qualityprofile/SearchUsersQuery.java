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
package org.sonar.db.qualityprofile;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public class SearchUsersQuery {

  public static final String ANY = "ANY";
  public static final String IN = "IN";
  public static final String OUT = "OUT";
  public static final Set<String> AVAILABLE_MEMBERSHIPS = ImmutableSet.of(ANY, IN, OUT);

  private final String organizationUuid;
  private final String qProfileUuid;
  private final String query;
  private final String membership;

  // for internal use in MyBatis
  final String querySql;
  final String querySqlLowercase;

  private SearchUsersQuery(Builder builder) {
    this.organizationUuid = builder.organization.getUuid();
    this.qProfileUuid = builder.profile.getKee();
    this.query = builder.query;
    this.membership = builder.membership;
    this.querySql = query == null ? null : buildLikeValue(query, BEFORE_AND_AFTER);
    this.querySqlLowercase = querySql == null ? null : querySql.toLowerCase(Locale.ENGLISH);
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String getQProfileUuid() {
    return qProfileUuid;
  }

  public String getMembership() {
    return membership;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private OrganizationDto organization;
    private QProfileDto profile;
    private String query;
    private String membership;

    private Builder() {
    }

    public Builder setOrganization(OrganizationDto organization) {
      this.organization = organization;
      return this;
    }

    public Builder setProfile(QProfileDto profile) {
      this.profile = profile;
      return this;
    }

    public Builder setMembership(@Nullable String membership) {
      this.membership = membership;
      return this;
    }

    public Builder setQuery(@Nullable String s) {
      this.query = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    private void initMembership() {
      membership = firstNonNull(membership, ANY);
      checkArgument(AVAILABLE_MEMBERSHIPS.contains(membership),
        "Membership is not valid (got " + membership + "). Availables values are " + AVAILABLE_MEMBERSHIPS);
    }

    public SearchUsersQuery build() {
      requireNonNull(organization, "Organization cannot be null");
      requireNonNull(profile, "Quality profile cant be null.");
      initMembership();
      return new SearchUsersQuery(this);
    }
  }
}
