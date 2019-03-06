/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.ce;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class CeActivityDao implements Dao {

  private final System2 system2;

  public CeActivityDao(System2 system2) {
    this.system2 = system2;
  }

  public Optional<CeActivityDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public void insert(DbSession dbSession, CeActivityDto dto) {
    dto.setCreatedAt(system2.now());
    dto.setUpdatedAt(system2.now());
    boolean isLast = dto.getStatus() != CeActivityDto.Status.CANCELED;
    dto.setIsLast(isLast);

    CeActivityMapper ceActivityMapper = mapper(dbSession);
    if (isLast) {
      ceActivityMapper.clearIsLast(dto.getIsLastKey(), dto.getUpdatedAt());
      ceActivityMapper.clearMainIsLast(dto.getMainIsLastKey(), dto.getUpdatedAt());
    }
    ceActivityMapper.insert(dto);
  }

  public List<CeActivityDto> selectOlderThan(DbSession dbSession, long beforeDate) {
    return mapper(dbSession).selectOlderThan(beforeDate);
  }

  public void deleteByUuids(DbSession dbSession, Set<String> uuids) {
    executeLargeUpdates(uuids, mapper(dbSession)::deleteByUuids);
  }

  /**
   * Ordered by id desc -> newest to oldest
   */
  public List<CeActivityDto> selectByQuery(DbSession dbSession, CeTaskQuery query, Pagination pagination) {
    if (query.isShortCircuitedByMainComponentUuids()) {
      return Collections.emptyList();
    }

    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public int countLastByStatusAndMainComponentUuid(DbSession dbSession, CeActivityDto.Status status, @Nullable String mainComponentUuid) {
    return mapper(dbSession).countLastByStatusAndMainComponentUuid(status, mainComponentUuid);
  }

  public Optional<CeActivityDto> selectLastByComponentUuidAndTaskType(DbSession dbSession, String componentUuid, String taskType) {
    return Optional.ofNullable(mapper(dbSession).selectLastByComponentUuidAndTaskType(componentUuid, taskType));
  }

  private static CeActivityMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(CeActivityMapper.class);
  }
}
