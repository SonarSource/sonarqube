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
import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

public class ScaReleasesDao implements Dao {

  private static ScaReleasesMapper mapper(DbSession session) {
    return session.getMapper(ScaReleasesMapper.class);
  }

  public void insert(DbSession session, ScaReleaseDto scaReleaseDto) {
    mapper(session).insert(scaReleaseDto);
  }

  public void deleteByUuid(DbSession session, String uuid) {
    mapper(session).deleteByUuid(uuid);
  }

  public Optional<ScaReleaseDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<ScaReleaseDto> selectByUuids(DbSession dbSession, Collection<String> uuids) {
    return mapper(dbSession).selectByUuids(uuids);
  }

  /**
   * Retrieves all releases with a specific branch UUID, no other filtering is done by this method.
   */
  public List<ScaReleaseDto> selectByBranchUuid(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectByBranchUuid(branchUuid);
  }

  public List<ScaReleaseDto> selectByQuery(DbSession session, ScaReleasesQuery scaReleasesQuery, Pagination pagination) {
    return mapper(session).selectByQuery(scaReleasesQuery, pagination);
  }

  public int countByQuery(DbSession session, ScaReleasesQuery scaReleasesQuery) {
    return mapper(session).countByQuery(scaReleasesQuery);
  }

  public void update(DbSession session, ScaReleaseDto scaReleaseDto) {
    mapper(session).update(scaReleaseDto);
  }
}
