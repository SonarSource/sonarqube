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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ScaReleasesDependenciesDao implements Dao {

  private static ScaReleasesMapper releasesMapper(DbSession session) {
    return session.getMapper(ScaReleasesMapper.class);
  }

  private static ScaDependenciesMapper dependenciesMapper(DbSession session) {
    return session.getMapper(ScaDependenciesMapper.class);
  }

  /**
   * Obtain ScaReleaseDependenciesDto for each of the release uuids.
   *
   * @param dbSession db session
   * @param uuids uuids for sca_releases
   * @return the list of ScaReleaseDependenciesDto
   */
  public List<ScaReleaseDependenciesDto> selectByReleaseUuids(DbSession dbSession, Collection<String> uuids) {
    List<ScaReleaseDto> releases = releasesMapper(dbSession).selectByUuids(uuids);
    return selectByReleaseDtos(dbSession, releases);
  }

  /**
   * Obtain ScaReleaseDependenciesDto wrapping each of the passed-in ScaReleaseDto and adding
   * the dependencies list.
   *
   * @param dbSession db session
   * @param releases ScaReleaseDto to be wrapped in ScaReleaseDependenciesDto after selecting dependencies
   * @return the list of ScaReleaseDependenciesDto
   */
  public List<ScaReleaseDependenciesDto> selectByReleaseDtos(DbSession dbSession, Collection<ScaReleaseDto> releases) {
    // This has a somewhat abnormal implementation (not in the mapper xml) due to
    // https://github.com/mybatis/mybatis-3/issues/101 ,
    // essentially a mapper cannot mix "creating immutable objects via constructor" (the &lt;constructor&gt; tag)
    // "filling in child objects from another query" (the &lt;collection&gt; tag), because mybatis would have to
    // be refactored to postpone creating the parent objects until it had loaded the child objects.
    // Some options considered:
    // 1. use mutable DTOs or temporary mutable DTOs that we then convert (lots of cruft)
    // 2. use a single join query instead of two queries (result set duplicates the parent's columns for each child)
    // 3. custom result handler doing something-or-other (not really worked out)
    // 4. just don't use a mapper and have a Dao that delegates to other mappers (this solution)
    List<ScaDependencyDto> dependencies = dependenciesMapper(dbSession).selectByReleaseUuids(releases.stream().map(ScaReleaseDto::uuid).toList());
    Map<String, List<ScaDependencyDto>> dependenciesGroupedByReleaseId = dependencies.stream().collect(Collectors.groupingBy(ScaDependencyDto::scaReleaseUuid));

    return releases.stream().map(releaseDto -> {
      // by returning empty list instead of omitting the release if there are no deps, we simulate a left join.
      // We may never actually save dependency-less sca_releases in real life though, which means we may
      // only be doing this so our tests don't always have to create dependencies in order to load releases
      // through here.
      var dependenciesDtos = Optional.ofNullable(dependenciesGroupedByReleaseId.get(releaseDto.uuid())).orElse(Collections.emptyList());
      return new ScaReleaseDependenciesDto(releaseDto, dependenciesDtos);
    }).toList();
  }
}
