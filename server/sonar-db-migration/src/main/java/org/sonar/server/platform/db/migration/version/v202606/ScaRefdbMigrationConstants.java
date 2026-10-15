/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

/**
 * Declares static values that are shared among the migrations related to SCA RefDB tables.
 */
final class ScaRefdbMigrationConstants {

  // Matches sonar-sca repo, see: https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/sonarqube-unified-app/src/main/java/com/sonar/sca/db/dtos/ScaIssueDto.java#L82.
  static final int VULNERABILITY_ID_MAX_LENGTH = 63;
  // Stores an enum currently of max length 5, see: https://github.com/SonarSource/sonar-sca/blob/2001a7e910e8c37bc90749d41f2c8eb0888683dd/sca-service/sca-cloud-app/data-service/src/main/java/com/sonar/sca/reference/db/dtos/LicensingVersionType.java
  // Migrate to a larger varchar size if enum length increases beyond 10 chars.
  static final int VERSION_TYPE_MAX_SIZE = 10;
  // Follows semver's max-size recommendation. See: https://semver.org/#does-semver-have-a-size-limit-on-the-version-string
  static final int VERSION_MAX_SIZE = 255;
  // Ecosystem names are typically pretty short (e.g., NPM, PyPI, crates.io, NuGet, etc.); migrate this to a larger size if necessary.
  static final int ECOSYSTEM_MAX_SIZE = 40;
  // Matches the max size on `sca_releases.package_name` and `sca_releases.package_purl`.
  static final int PACKAGE_NAME_MAX_SIZE = 400;
  static final int PACKAGE_PURL_MAX_SIZE = 400;
  // A package lookup key is composed of an ecosystem identifier, a slash, and a package name.
  static final int PACKAGE_LOOKUP_KEY_MAX_SIZE = ECOSYSTEM_MAX_SIZE + 1 + PACKAGE_NAME_MAX_SIZE;
  // The values in this column are data source abbreviations, such as 'GHSA' and 'OSV'. 10 characters should be okay, but migrate to a larger limit if necessary.
  static final int DATABASE_PREFIX_MAX_SIZE = 10;

  private ScaRefdbMigrationConstants() {
  }
}
