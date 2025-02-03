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
 * Represents a Software Composition Analysis (SCA) dependency, associated with a component.
 * The component will be a package component nested inside a project branch component.
 *
 * @param uuid                         primary key
 * @param componentUuid                the component the dependency is associated with
 * @param packageUrl                   package URL following the PURL specification
 * @param packageManager               package manager e.g. PYPI
 * @param packageName                  package name e.g. "urllib3"
 * @param version                      package version e.g. "1.25.6"
 * @param direct                       is this a direct dependency of the project
 * @param scope                        the scope of the dependency e.g. "development"
 * @param dependencyFilePath           path to the file where the dependency was found, preferring the "manifest" to the "lockfile"
 * @param licenseExpression            an SPDX license expression (NOT a single license, can have parens/AND/OR)
 * @param known                        is this package and version known to Sonar (if not it be internal, could be malicious, could be from a weird repo)
 * @param createdAt                    timestamp of creation
 * @param updatedAt                    timestamp of most recent update
 */
public record ScaDependencyDto(
  String uuid,
  String componentUuid,
  String packageUrl,
  PackageManager packageManager,
  String packageName,
  String version,
  boolean direct,
  String scope,
  String dependencyFilePath,
  String licenseExpression,
  boolean known,
  long createdAt,
  long updatedAt) {

  // These need to be in sync with the database but because the db migration module and this module don't
  // depend on each other, we can't make one just refer to the other.
  public static final int PACKAGE_URL_MAX_LENGTH = 400;
  public static final int PACKAGE_MANAGER_MAX_LENGTH = 20;
  public static final int PACKAGE_NAME_MAX_LENGTH = 400;
  public static final int VERSION_MAX_LENGTH = 400;
  public static final int SCOPE_MAX_LENGTH = 100;
  public static final int DEPENDENCY_FILE_PATH_MAX_LENGTH = 1000;
  public static final int LICENSE_EXPRESSION_MAX_LENGTH = 400;

  public ScaDependencyDto {
    // We want these to raise errors and not silently put junk values in the db
    checkLength(packageUrl, PACKAGE_URL_MAX_LENGTH, "packageUrl");
    checkLength(packageName, PACKAGE_NAME_MAX_LENGTH, "packageName");
    checkLength(version, VERSION_MAX_LENGTH, "version");
    checkLength(scope, SCOPE_MAX_LENGTH, "scope");
    checkLength(dependencyFilePath, DEPENDENCY_FILE_PATH_MAX_LENGTH, "dependencyFilePath");
    checkLength(licenseExpression, LICENSE_EXPRESSION_MAX_LENGTH, "licenseExpression");
  }

  private static void checkLength(String value, int maxLength, String name) {
    checkArgument(value.length() <= maxLength, "Maximum length of %s is %s: %s", name, maxLength, value);
  }

  public static class Builder {
    private String uuid;
    private String componentUuid;
    private String packageUrl;
    private PackageManager packageManager;
    private String packageName;
    private String version;
    private boolean direct;
    private String scope;
    private String dependencyFilePath;
    private String licenseExpression;
    private boolean known;
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

    public Builder setDirect(boolean direct) {
      this.direct = direct;
      return this;
    }

    public Builder setScope(String scope) {
      this.scope = scope;
      return this;
    }

    public Builder setDependencyFilePath(String dependencyFilePath) {
      this.dependencyFilePath = dependencyFilePath;
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

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public ScaDependencyDto build() {
      return new ScaDependencyDto(
        uuid, componentUuid, packageUrl, packageManager, packageName, version, direct, scope, dependencyFilePath, licenseExpression, known, createdAt, updatedAt
      );
    }
  }

  public Builder toBuilder() {
    return new Builder()
      .setUuid(this.uuid)
      .setComponentUuid(this.componentUuid)
      .setPackageUrl(this.packageUrl)
      .setPackageManager(this.packageManager)
      .setPackageName(this.packageName)
      .setVersion(this.version)
      .setDirect(this.direct)
      .setScope(this.scope)
      .setDependencyFilePath(this.dependencyFilePath)
      .setLicenseExpression(this.licenseExpression)
      .setKnown(this.known)
      .setCreatedAt(this.createdAt)
      .setUpdatedAt(this.updatedAt);
  }
}
