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
 * Represents a single release of a package, such as an npm or maven package,
 * as found in a single dependency analysis run (so it's attached to a branch component,
 * and there's a separate copy of each release per branch it appears in).
 *
 * @param uuid                 Primary key
 * @param componentUuid        the component the release is associated with
 * @param packageUrl           package URL following the PURL specification
 * @param packageManager       package manager e.g. PYPI
 * @param packageName          package name e.g. "urllib3"
 * @param version              package version e.g. "1.25.6"
 * @param licenseExpression    an SPDX license expression (NOT a single license, can have parens/AND/OR)
 * @param known                is this package and version known to Sonar (if not it be internal, could be malicious, could be from a weird repo)
 * @param newInPullRequest     is it newly added in a PR (always false when not on a PR)
 * @param createdAt            timestamp it was created
 * @param updatedAt            timestamp it was last updated
 */
public record ScaReleaseDto(
  String uuid,
  String componentUuid,
  String packageUrl,
  PackageManager packageManager,
  String packageName,
  String version,
  String licenseExpression,
  boolean known,
  boolean newInPullRequest,
  long createdAt,
  long updatedAt) {

  // these need to match what's in the db
  public static final int PACKAGE_URL_MAX_LENGTH = 400;
  public static final int PACKAGE_MANAGER_MAX_LENGTH = 20;
  public static final int PACKAGE_NAME_MAX_LENGTH = 400;
  public static final int VERSION_MAX_LENGTH = 400;
  public static final int LICENSE_EXPRESSION_MAX_LENGTH = 400;

  public ScaReleaseDto {
    // We want these to raise errors and not silently put junk values in the db
    checkLength(packageUrl, PACKAGE_URL_MAX_LENGTH, "packageUrl");
    checkLength(packageName, PACKAGE_NAME_MAX_LENGTH, "packageName");
    checkLength(version, VERSION_MAX_LENGTH, "version");
    checkLength(licenseExpression, LICENSE_EXPRESSION_MAX_LENGTH, "licenseExpression");
  }

  private static void checkLength(String value, int maxLength, String name) {
    checkArgument(value.length() <= maxLength, "Maximum length of %s is %s: %s", name, maxLength, value);
  }

  public Builder toBuilder() {
    return new Builder()
      .setUuid(this.uuid)
      .setComponentUuid(this.componentUuid)
      .setPackageUrl(this.packageUrl)
      .setPackageManager(this.packageManager)
      .setPackageName(this.packageName)
      .setVersion(this.version)
      .setLicenseExpression(this.licenseExpression)
      .setKnown(this.known)
      .setNewInPullRequest(this.newInPullRequest)
      .setCreatedAt(this.createdAt)
      .setUpdatedAt(this.updatedAt);
  }

  /**
   * Returns an object whose .equals and .hashCode would match that of another ScaReleaseDto's
   * identity() if the two ScaReleaseDto would count as duplicates within the sca_releases table
   * (within a single analysis, so ignoring the componentUuid).
   * This is different from the DTOs themselves being equal because some fields do not count in
   * the identity of the row, and can be updated while preserving the identity. The method just
   * returns Object and not a type, because it exists just to call .equals and .hashCode on.
   *
   * @return an object to be used for hashing and comparing ScaReleaseDto instances for identity
   */
  public Identity identity() {
    return new IdentityImpl(this);
  }

  public interface Identity {
  }

  private record IdentityImpl(String packageUrl) implements Identity {
    IdentityImpl(ScaReleaseDto dto) {
      this(dto.packageUrl());
    }
  }

  public static class Builder {
    private String uuid;
    private String componentUuid;
    private String packageUrl;
    private PackageManager packageManager;
    private String packageName;
    private String version;
    private String licenseExpression;
    private boolean known;
    private boolean newInPullRequest;
    private long createdAt;
    private long updatedAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setComponentUuid(String componentUuid) {
      this.componentUuid = componentUuid;
      return this;
    }

    public Builder setPackageUrl(String packageUrl) {
      this.packageUrl = packageUrl;
      return this;
    }

    public Builder setPackageManager(PackageManager packageManager) {
      this.packageManager = packageManager;
      return this;
    }

    public Builder setPackageName(String packageName) {
      this.packageName = packageName;
      return this;
    }

    public Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder setLicenseExpression(String licenseExpression) {
      this.licenseExpression = licenseExpression;
      return this;
    }

    public Builder setKnown(boolean known) {
      this.known = known;
      return this;
    }

    public Builder setNewInPullRequest(boolean newInPullRequest) {
      this.newInPullRequest = newInPullRequest;
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

    public ScaReleaseDto build() {
      return new ScaReleaseDto(
        uuid, componentUuid, packageUrl, packageManager, packageName, version, licenseExpression, known, newInPullRequest, createdAt, updatedAt);
    }
  }
}
