/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public record ScaReleasesQuery(
  String branchUuid,
  @Nullable Boolean direct,
  @Nullable Boolean productionScope,
  @Nullable List<String> packageManagers,
  @Nullable Boolean newInPullRequest,
  @Nullable String query) {

  /**
   * Used by MyBatis mapper
   */
  @CheckForNull
  public String likeQuery() {
    return query == null ? null : buildLikeValue(query.toLowerCase(Locale.ENGLISH), BEFORE_AND_AFTER);
  }

  public Builder toBuilder() {
    return new Builder()
      .setBranchUuid(branchUuid)
      .setDirect(direct)
      .setProductionScope(productionScope)
      .setPackageManagers(packageManagers)
      .setNewInPullRequest(newInPullRequest)
      .setQuery(query);
  }

  public static class Builder {
    private String branchUuid;
    private Boolean direct;
    private Boolean productionScope;
    private List<String> packageManagers;
    private Boolean newInPullRequest;
    private String query;

    public Builder setBranchUuid(String branchUuid) {
      this.branchUuid = branchUuid;
      return this;
    }

    public Builder setDirect(Boolean direct) {
      this.direct = direct;
      return this;
    }

    public Builder setProductionScope(Boolean productionScope) {
      this.productionScope = productionScope;
      return this;
    }

    public Builder setPackageManagers(List<String> packageManagers) {
      this.packageManagers = packageManagers;
      return this;
    }

    public Builder setNewInPullRequest(Boolean newInPullRequest) {
      this.newInPullRequest = newInPullRequest;
      return this;
    }

    public Builder setQuery(String query) {
      this.query = query;
      return this;
    }

    public ScaReleasesQuery build() {
      return new ScaReleasesQuery(branchUuid, direct, productionScope, packageManagers, newInPullRequest, query);
    }
  }
}
