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

import java.math.BigDecimal;
import java.util.List;
import javax.annotation.Nullable;

/**
 * <p>A "read-only" DTO used to query the join of sca_issues_releases, sca_issues, and sca_*_issues.
 * This is used to return all the details shown in a list of issues in the UX.
 * This DTO and its mapper are an optimization, to do more work in SQL and
 * avoid "joining in Java."
 * </p>
 * <p>
 *   The packageUrl parameter in particular is tricky; it's the identity packageUrl from ScaIssueDto,
 *   and can be set to {@link ScaIssueDto#NULL_VALUE} if the issue is not a vulnerability. What you
 *   likely want in many cases instead would be the package URLs from the individual releases,
 *   those are the affected release URLs.
 * </p>
 * <p>
 *   Similarily, vulnerabiiltyId and spdxLicenseId can have {@link ScaIssueDto#NULL_VALUE} if they
 *   are inapplicable to the issue type at hand.
 * </p>
 */
public record ScaIssueReleaseDetailsDto(String scaIssueReleaseUuid,
  ScaSeverity severity,
  String scaIssueUuid,
  String scaReleaseUuid,
  ScaIssueType scaIssueType,
  boolean newInPullRequest,
  String packageUrl,
  String vulnerabilityId,
  String spdxLicenseId,
  @Nullable ScaSeverity vulnerabilityBaseSeverity,
  @Nullable List<String> cweIds,
  @Nullable BigDecimal cvssScore) implements ScaIssueIdentity {

  public Builder toBuilder() {
    return new Builder()
      .setScaIssueReleaseUuid(scaIssueReleaseUuid)
      .setSeverity(severity)
      .setScaIssueUuid(scaIssueUuid)
      .setScaReleaseUuid(scaReleaseUuid)
      .setScaIssueType(scaIssueType)
      .setNewInPullRequest(newInPullRequest)
      .setPackageUrl(packageUrl)
      .setVulnerabilityId(vulnerabilityId)
      .setSpdxLicenseId(spdxLicenseId)
      .setVulnerabilityBaseSeverity(vulnerabilityBaseSeverity)
      .setCweIds(cweIds)
      .setCvssScore(cvssScore);
  }

  public static class Builder {
    private String scaIssueReleaseUuid;
    private ScaSeverity severity;
    private String scaIssueUuid;
    private String scaReleaseUuid;
    private ScaIssueType scaIssueType;
    private boolean newInPullRequest;
    private String packageUrl;
    private String vulnerabilityId;
    private String spdxLicenseId;
    private ScaSeverity vulnerabilityBaseSeverity;
    private List<String> cweIds;
    private BigDecimal cvssScore;

    public Builder setScaIssueReleaseUuid(String scaIssueReleaseUuid) {
      this.scaIssueReleaseUuid = scaIssueReleaseUuid;
      return this;
    }

    public Builder setSeverity(ScaSeverity severity) {
      this.severity = severity;
      return this;
    }

    public Builder setScaIssueUuid(String scaIssueUuid) {
      this.scaIssueUuid = scaIssueUuid;
      return this;
    }

    public Builder setScaReleaseUuid(String scaReleaseUuid) {
      this.scaReleaseUuid = scaReleaseUuid;
      return this;
    }

    public Builder setScaIssueType(ScaIssueType scaIssueType) {
      this.scaIssueType = scaIssueType;
      return this;
    }

    public Builder setNewInPullRequest(boolean newInPullRequest) {
      this.newInPullRequest = newInPullRequest;
      return this;
    }

    public Builder setPackageUrl(String packageUrl) {
      this.packageUrl = packageUrl;
      return this;
    }

    public Builder setVulnerabilityId(String vulnerabilityId) {
      this.vulnerabilityId = vulnerabilityId;
      return this;
    }

    public Builder setSpdxLicenseId(String spdxLicenseId) {
      this.spdxLicenseId = spdxLicenseId;
      return this;
    }

    public Builder setVulnerabilityBaseSeverity(@Nullable ScaSeverity vulnerabilityBaseSeverity) {
      this.vulnerabilityBaseSeverity = vulnerabilityBaseSeverity;
      return this;
    }

    public Builder setCweIds(@Nullable List<String> cweIds) {
      this.cweIds = cweIds;
      return this;
    }

    public Builder setCvssScore(@Nullable BigDecimal cvssScore) {
      this.cvssScore = cvssScore;
      return this;
    }

    public ScaIssueReleaseDetailsDto build() {
      return new ScaIssueReleaseDetailsDto(scaIssueReleaseUuid, severity, scaIssueUuid, scaReleaseUuid, scaIssueType,
        newInPullRequest, packageUrl, vulnerabilityId, spdxLicenseId, vulnerabilityBaseSeverity, cweIds, cvssScore);
    }
  }
}
