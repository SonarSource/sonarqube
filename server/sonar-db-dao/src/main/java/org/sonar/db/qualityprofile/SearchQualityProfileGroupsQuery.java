/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Locale;
import org.sonar.db.user.SearchGroupsQuery;

import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public class SearchQualityProfileGroupsQuery extends SearchGroupsQuery {

  private final String qProfileUuid;

  public SearchQualityProfileGroupsQuery(Builder builder) {
    this.qProfileUuid = builder.profile.getKee();
    this.query = builder.getQuery();
    this.membership = builder.getMembership();
    this.querySqlLowercase = query == null ? null : buildLikeValue(query, BEFORE_AND_AFTER).toLowerCase(Locale.ENGLISH);
  }

  public String getQProfileUuid() {
    return qProfileUuid;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends SearchGroupsQuery.Builder<Builder> {
    private QProfileDto profile;

    public Builder setProfile(QProfileDto profile) {
      this.profile = profile;
      return this;
    }

    public SearchQualityProfileGroupsQuery build() {
      initMembership();
      return new SearchQualityProfileGroupsQuery(this);
    }
  }
}
