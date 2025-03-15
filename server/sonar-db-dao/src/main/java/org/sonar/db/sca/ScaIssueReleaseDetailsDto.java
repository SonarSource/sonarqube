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
import org.sonar.api.utils.DateUtils;

/**
 * <p>A "read-only" DTO used to query the join of sca_issues_releases, sca_issues, and sca_*_issues.
 * This is used to return all the details shown in a list of issues in the UX.
 * This DTO and its mapper are an optimization, to do more work in SQL and
 * avoid "joining in Java."
 * </p>
 * <p>
 *   The uuids in the DTOs must all correspond, or some kind of bug is happening.
 * </p>
 * <p>
 *   issueReleaseUuid is passed in separately because it allows mybatis to have an ID for the DTO,
 *   which it then uses for caching and lookup instead of hashing the whole object.
 * </p>
 */
public record ScaIssueReleaseDetailsDto(
  String issueReleaseUuid,
  ScaIssueReleaseDto issueReleaseDto,
  ScaIssueDto issueDto,
  ScaReleaseDto releaseDto,
  @Nullable ScaVulnerabilityIssueDto vulnerabilityIssueDto) {

  public ScaIssueReleaseDetailsDto {
    // the issueReleaseUuid is separate so mybatis can use it for instance
    // identity, but it must match the UUID in the issueReleaseDto
    // and is straight-up redundant.
    if (!issueReleaseUuid.equals(issueReleaseDto.uuid())) {
      throw new IllegalArgumentException("issueReleaseUuid must match issueReleaseDto.uuid()");
    }
    if (!issueDto.uuid().equals(issueReleaseDto.scaIssueUuid())) {
      throw new IllegalArgumentException("issueDto.uuid() must match issueReleaseDto.scaIssueUuid()");
    }
    if (!releaseDto.uuid().equals(issueReleaseDto.scaReleaseUuid())) {
      throw new IllegalArgumentException("releaseDto.uuid() must match issueReleaseDto.scaReleaseUuid()");
    }
    if (vulnerabilityIssueDto != null && !vulnerabilityIssueDto.uuid().equals(issueDto.uuid())) {
      throw new IllegalArgumentException("vulnerabilityIssueDto.uuid() must match issueDto.uuid()");
    }
  }

  // DateUtils says that this returns an RFC 822 timestamp
  // but it is really a ISO 8601 timestamp.
  public String createdAtIso8601() {
    return DateUtils.formatDateTime(issueReleaseDto.createdAt());
  }

  public ScaSeverity severity() {
    return issueReleaseDto.severity();
  }

  public String issueUuid() {
    return issueDto.uuid();
  }

  public String releaseUuid() {
    return releaseDto.uuid();
  }

  public ScaIssueType scaIssueType() {
    return issueDto.scaIssueType();
  }

  public boolean newInPullRequest() {
    return releaseDto.newInPullRequest();
  }

  public String version() {
    return releaseDto.version();
  }

  /**
   * Returns the versioned package URL of the release
   */
  public String releasePackageUrl() {
    return releaseDto.packageUrl();
  }

  /** Returns the unversioned package URL of the security vulnerability,
   * or ScaIssueDto::NULL_VALUE if the issue is not a vulnerability.
   */
  public String issuePackageUrl() {
    return issueDto.packageUrl();
  }

  /**
   * Returns the vulnerability ID of the issue, or ScaIssueDto::NULL_VALUE if the issue is not a vulnerability.
   */
  public String vulnerabilityId() {
    return issueDto.vulnerabilityId();
  }

  /** Returns the SPDX license ID of the issue, or ScaIssueDto::NULL_VALUE if the issue is not a license issue. */
  public String spdxLicenseId() {
    return issueDto.spdxLicenseId();
  }

  /** Returns the base severity of the vulnerability, or null if the issue is not a vulnerability. */
  public @Nullable ScaSeverity vulnerabilityBaseSeverity() {
    return vulnerabilityIssueDto == null ? null : vulnerabilityIssueDto.baseSeverity();
  }

  /** Returns the CWE IDs of the vulnerability, or null if the issue is not a vulnerability. */
  public @Nullable List<String> cweIds() {
    return vulnerabilityIssueDto == null ? null : vulnerabilityIssueDto.cweIds();
  }

  /** Returns the CVSS score of the vulnerability, or null if the issue is not a vulnerability or does not have a CVSS score. */
  public @Nullable BigDecimal cvssScore() {
    return vulnerabilityIssueDto == null ? null : vulnerabilityIssueDto.cvssScore();
  }

  public Builder toBuilder() {
    return new Builder()
      .setIssueReleaseDto(issueReleaseDto)
      .setIssueDto(issueDto)
      .setReleaseDto(releaseDto)
      .setVulnerabilityIssueDto(vulnerabilityIssueDto);
  }

  public static class Builder {
    private ScaIssueReleaseDto issueReleaseDto;
    private ScaIssueDto issueDto;
    private ScaReleaseDto releaseDto;
    @Nullable
    private ScaVulnerabilityIssueDto vulnerabilityIssueDto;

    public Builder setIssueReleaseDto(ScaIssueReleaseDto issueReleaseDto) {
      this.issueReleaseDto = issueReleaseDto;
      return this;
    }

    public Builder setIssueDto(ScaIssueDto issueDto) {
      this.issueDto = issueDto;
      return this;
    }

    public Builder setReleaseDto(ScaReleaseDto releaseDto) {
      this.releaseDto = releaseDto;
      return this;
    }

    public Builder setVulnerabilityIssueDto(@Nullable ScaVulnerabilityIssueDto vulnerabilityIssueDto) {
      this.vulnerabilityIssueDto = vulnerabilityIssueDto;
      return this;
    }

    public ScaIssueReleaseDetailsDto build() {
      return new ScaIssueReleaseDetailsDto(issueReleaseDto.uuid(), issueReleaseDto, issueDto, releaseDto, vulnerabilityIssueDto);
    }
  }
}
