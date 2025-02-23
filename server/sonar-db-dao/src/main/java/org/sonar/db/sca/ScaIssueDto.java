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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This table has GLOBAL rows spanning all analysis runs. For a given notional
 * problem there will be ONE row. A notional problem could be a particular
 * vulnerability "CVE-12345" or a particular license rule like "GPL-3.0 is prohibited".
 * The purpose of this table is to assign a uuid to that notional problem.
 * Because the uuid must be globally unique for the same problem, there is a
 * unique constraint across all the columns.
 * <p>
 *   NULL columns cannot participate in unique constraints on all database backends,
 * so irrelevant columns for a particular issue type are set to empty string instead of NULL.
 * </p>
 * <p>
 *   The columns in this table should be those that establish the identity of the issue
 *   and no more. See {@link ScaIssueType} which has a method returning the proper
 *   ScaIssueDto for each issue type. Those same columns without uuid and timestamps
 *   are also in the {@link DefaultScaIssueIdentity} type.
 * </p>
 * <p>
 *   The packageUrl may or may not include a version number, depending on whether
 *   the issue type is per-package or per-release.
 * </p>
 */
public record ScaIssueDto(
  String uuid,
  ScaIssueType scaIssueType,
  String packageUrl,
  String vulnerabilityId,
  String spdxLicenseId,
  long createdAt,
  long updatedAt) implements ScaIssueIdentity {

  /**
   * Value that represents "does not apply" in one of the identity columns.
   * <p>
   *   You know you are going to ask, so the reason we can't use empty string
   *   is that Oracle thinks empty strings are NULL. And the reason we can't
   *   use NULL is that not all databases have a way to consider NULL as a
   *   value in a unique constraint. So anyway, just go with it.
   * </p>
   * <p>
   *   This string should be invalid as an actual value for all of the
   *   columns, so it's not a package url, not a vulnerability ID,
   *   and not a SPDX license ID.
   * </p>
   */
  public static final String NULL_VALUE = "-";

  // these need to match what's in the db
  public static final int SCA_ISSUE_TYPE_MAX_LENGTH = 40;
  public static final int PACKAGE_URL_MAX_LENGTH = 400;
  public static final int VULNERABILITY_ID_MAX_LENGTH = 63;
  public static final int SPDX_LICENSE_ID_MAX_LENGTH = 127;

  public ScaIssueDto {
    // We want these to raise errors and not silently put junk values in the db
    checkIdentityColumn(packageUrl, PACKAGE_URL_MAX_LENGTH, "packageUrl");
    checkIdentityColumn(vulnerabilityId, VULNERABILITY_ID_MAX_LENGTH, "vulnerabilityId");
    checkIdentityColumn(spdxLicenseId, SPDX_LICENSE_ID_MAX_LENGTH, "spdxLicenseId");
  }

  public ScaIssueDto(String uuid, ScaIssueIdentity identity, long createdAt, long updatedAt) {
    this(uuid, identity.scaIssueType(), identity.packageUrl(), identity.vulnerabilityId(), identity.spdxLicenseId(), createdAt, updatedAt);
  }

  private static void checkIdentityColumn(String value, int maxLength, String name) {
    checkArgument(value != null, "Column %s cannot be null", name);
    checkArgument(!value.isBlank(), "Column %s cannot be blank, use ScaIssueDto.NULL_VALUE", name);
    checkArgument(value.length() <= maxLength, "Maximum length of %s is %s: %s", name, maxLength, value);
  }

  public Builder toBuilder() {
    return new Builder()
      .setUuid(uuid)
      .setScaIssueType(scaIssueType)
      .setPackageUrl(packageUrl)
      .setVulnerabilityId(vulnerabilityId)
      .setSpdxLicenseId(spdxLicenseId)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt);
  }

  public static class Builder {
    private String uuid;
    private ScaIssueType scaIssueType;
    private String packageUrl;
    private String vulnerabilityId;
    private String spdxLicenseId;
    private long createdAt;
    private long updatedAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setScaIssueType(ScaIssueType scaIssueType) {
      this.scaIssueType = scaIssueType;
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

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public ScaIssueDto build() {
      return new ScaIssueDto(uuid, scaIssueType, packageUrl, vulnerabilityId, spdxLicenseId, createdAt, updatedAt);
    }
  }
}
