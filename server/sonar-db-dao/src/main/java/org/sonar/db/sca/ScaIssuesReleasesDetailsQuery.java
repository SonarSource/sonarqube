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
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public record ScaIssuesReleasesDetailsQuery(
  String branchUuid,
  Sort sort,
  @Nullable String vulnerabilityIdSubstring,
  @Nullable String packageNameSubstring,
  @Nullable Boolean newInPullRequest,
  @Nullable List<ScaIssueType> types,
  @Nullable List<ScaSeverity> severities,
  @Nullable List<PackageManager> packageManagers) {

  public ScaIssuesReleasesDetailsQuery {
    Objects.requireNonNull(branchUuid);
    Objects.requireNonNull(sort);
  }

  /** For use in the mapper after <code>upper(vulnerabilityId) LIKE</code>,
   * and per the {@link org.sonar.db.DaoUtils#buildLikeValue(String, WildcardPosition)}}
   * docs, we have to say <code>ESCAPE '/'</code>. We are using uppercase because
   * most ids will be uppercase already.
   */
  @CheckForNull
  public String vulnerabilityIdUppercaseEscapedAsLikeValue() {
    return vulnerabilityIdSubstring == null ? null : buildLikeValue(vulnerabilityIdSubstring.toUpperCase(Locale.ROOT), BEFORE_AND_AFTER);
  }

  /** For use in the mapper after <code>lower(packageName) LIKE</code>,
   * and per the {@link org.sonar.db.DaoUtils#buildLikeValue(String, WildcardPosition)}}
   * docs, we have to say <code>ESCAPE '/'</code>. We are using lowercase because most
   * package names will be all or mostly lowercase already.
   */
  @CheckForNull
  public String packageNameLowercaseEscapedAsLikeValue() {
    return packageNameSubstring == null ? null : buildLikeValue(packageNameSubstring.toLowerCase(Locale.ROOT), BEFORE_AND_AFTER);
  }

  public Builder toBuilder() {
    return new Builder()
      .setBranchUuid(branchUuid)
      .setSort(sort)
      .setVulnerabilityIdSubstring(vulnerabilityIdSubstring)
      .setPackageNameSubstring(packageNameSubstring)
      .setNewInPullRequest(newInPullRequest)
      .setTypes(types)
      .setSeverities(severities)
      .setPackageManagers(packageManagers);
  }

  public enum Sort {
    IDENTITY_ASC("+identity"),
    IDENTITY_DESC("-identity"),
    SEVERITY_ASC("+severity"),
    SEVERITY_DESC("-severity"),
    CVSS_SCORE_ASC("+cvssScore"),
    CVSS_SCORE_DESC("-cvssScore");

    private final String queryParameterValue;

    Sort(String queryParameterValue) {
      this.queryParameterValue = queryParameterValue;
    }

    /**
     * Convert a query parameter value to the corresponding {@link Sort} enum value.
     * The passed-in string must not be null.
     */
    public static Optional<Sort> fromQueryParameterValue(String queryParameterValue) {
      for (Sort sort : values()) {
        if (sort.queryParameterValue.equals(queryParameterValue)) {
          return Optional.of(sort);
        }
      }
      return Optional.empty();
    }

    public String queryParameterValue() {
      return queryParameterValue;
    }
  }

  public static class Builder {
    private String branchUuid;
    private Sort sort;
    private String vulnerabilityIdSubstring;
    private String packageNameSubstring;
    private Boolean newInPullRequest;
    private List<ScaIssueType> types;
    private List<ScaSeverity> severities;
    private List<PackageManager> packageManagers;

    public Builder setBranchUuid(String branchUuid) {
      this.branchUuid = branchUuid;
      return this;
    }

    public Builder setSort(Sort sort) {
      this.sort = sort;
      return this;
    }

    public Builder setVulnerabilityIdSubstring(@Nullable String vulnerabilityIdSubstring) {
      this.vulnerabilityIdSubstring = vulnerabilityIdSubstring;
      return this;
    }

    public Builder setPackageNameSubstring(@Nullable String packageNameSubstring) {
      this.packageNameSubstring = packageNameSubstring;
      return this;
    }

    public Builder setNewInPullRequest(@Nullable Boolean newInPullRequest) {
      this.newInPullRequest = newInPullRequest;
      return this;
    }

    public Builder setTypes(@Nullable List<ScaIssueType> types) {
      this.types = types;
      return this;
    }

    public Builder setSeverities(@Nullable List<ScaSeverity> severities) {
      this.severities = severities;
      return this;
    }

    public Builder setPackageManagers(@Nullable List<PackageManager> packageManagers) {
      this.packageManagers = packageManagers;
      return this;
    }

    public ScaIssuesReleasesDetailsQuery build() {
      return new ScaIssuesReleasesDetailsQuery(branchUuid, sort, vulnerabilityIdSubstring, packageNameSubstring, newInPullRequest, types, severities, packageManagers);
    }
  }
}
