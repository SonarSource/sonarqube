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
import javax.annotation.Nullable;

/**
 * This DTO represents the join of sca_dependencies and sca_releases, and is "read only"
 * (it cannot be inserted, it would only be a query result).
 *
 * @param dependencyUuid uuid of the sca_dependencies row
 * @param releaseUuid uuid of the sca_releases row
 * @param componentUuid uuid of the component both rows were associated with
 * @param direct is it a direct dep
 * @param scope scope/type of the dep like "compile"
 * @param userDependencyFilePath which manifest file (e.g. package.json)
 * @param lockfileDependencyFilePath which lockfile (e.g. package-lock.json)
 * @param chains chains that brought the dependency in, e.g. [["pkg:npm/foo@1.0.0", ...], ...]
 * @param packageUrl PURL specification URL
 * @param packageManager package manager
 * @param packageName name of package
 * @param version version
 * @param licenseExpression SPDX license expression
 * @param known was the package known to Sonar
 */
public record ScaDependencyReleaseDto(String dependencyUuid,
  String releaseUuid,
  String componentUuid,
  boolean direct,
  String scope,
  @Nullable String userDependencyFilePath,
  @Nullable String lockfileDependencyFilePath,
  @Nullable List<List<String>> chains,
  String packageUrl,
  PackageManager packageManager,
  String packageName,
  String version,
  String licenseExpression,
  boolean known) {

  public ScaDependencyReleaseDto(ScaDependencyDto dependency, ScaReleaseDto release) {
    this(
      dependency.uuid(),
      release.uuid(),
      release.componentUuid(),
      dependency.direct(),
      dependency.scope(),
      dependency.userDependencyFilePath(),
      dependency.lockfileDependencyFilePath(),
      dependency.chains(),
      release.packageUrl(),
      release.packageManager(),
      release.packageName(),
      release.version(),
      release.licenseExpression(),
      release.known());
    if (!dependency.scaReleaseUuid().equals(release.uuid())) {
      throw new IllegalArgumentException("Dependency and release UUIDs should match");
    }
  }

  public String primaryDependencyFilePath() {
    return userDependencyFilePath != null ? userDependencyFilePath : lockfileDependencyFilePath;
  }
}
