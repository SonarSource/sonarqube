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
import java.util.Objects;

/**
 * This DTO represents the join of sca_releases and sca_dependencies, and is "read only"
 * (it cannot be inserted, it would only be a query result).
 * <p>
 *   The releaseUuid must match the uuid in the release DTO, it is duplicated to help out mybatis
 *   in caching and lookup (allow the mapper for this DTO have an idArg).
 * </p>
 * @param releaseUuid       uuid of the releaseDto
 * @param release        release
 * @param dependencies      dependency DTOs
 */
public record ScaReleaseDependenciesDto(
  String releaseUuid,
  ScaReleaseDto release,
  List<ScaDependencyDto> dependencies) {

  public ScaReleaseDependenciesDto {
    Objects.requireNonNull(release);
    Objects.requireNonNull(dependencies);
    if (!releaseUuid.equals(release.uuid())) {
      throw new IllegalArgumentException("releaseUuid must match release.uuid()");
    }
  }

  public ScaReleaseDependenciesDto(ScaReleaseDto release, List<ScaDependencyDto> dependencies) {
    this(release.uuid(), release, dependencies);
  }
}
