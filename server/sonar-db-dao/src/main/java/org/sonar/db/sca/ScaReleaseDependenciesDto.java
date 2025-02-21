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

/**
 * This DTO represents the join of sca_releases and sca_dependencies, and is "read only"
 * (it cannot be inserted, it would only be a query result).
 *
 * @param uuid              Primary key
 * @param componentUuid     the component the release is associated with
 * @param packageUrl        package URL following the PURL specification
 * @param packageManager    package manager e.g. PYPI
 * @param packageName       package name e.g. "urllib3"
 * @param version           package version e.g. "1.25.6"
 * @param licenseExpression an SPDX license expression (NOT a single license, can have parens/AND/OR)
 * @param known             is this package and version known to Sonar (if not it be internal, could be malicious, could be from a weird repo)
 * @param createdAt         timestamp it was created
 * @param updatedAt         timestamp it was last updated
 */
public record ScaReleaseDependenciesDto(
  String uuid,
  String componentUuid,
  String packageUrl,
  PackageManager packageManager,
  String packageName,
  String version,
  String licenseExpression,
  boolean known,
  long createdAt,
  long updatedAt,
  List<ScaDependencyDto> dependencies) {

  public ScaReleaseDependenciesDto(ScaReleaseDto release, List<ScaDependencyDto> dependencies) {
    this(
      release.uuid(),
      release.componentUuid(),
      release.packageUrl(),
      release.packageManager(),
      release.packageName(),
      release.version(),
      release.licenseExpression(),
      release.known(),
      release.createdAt(),
      release.updatedAt(),
      dependencies
    );
  }
}
