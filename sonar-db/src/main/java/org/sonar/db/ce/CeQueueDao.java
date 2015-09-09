/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.ce;

import com.google.common.base.Optional;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;

public class CeQueueDao implements Dao {

  private final System2 system2;

  public CeQueueDao(System2 system2) {
    this.system2 = system2;
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectAllInAscOrder(DbSession session) {
    return mapper(session).selectAllInAscOrder();
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectByComponentUuid(DbSession session, String componentUuid) {
    return mapper(session).selectByComponentUuid(componentUuid);
  }

  public Optional<CeQueueDto> selectByUuid(DbSession session, String uuid) {
    return Optional.fromNullable(mapper(session).selectByUuid(uuid));
  }

  public CeQueueDto insert(DbSession session, CeQueueDto dto) {
    dto.setCreatedAt(system2.now());
    dto.setUpdatedAt(system2.now());
    mapper(session).insert(dto);
    return dto;
  }

  public void deleteByUuid(DbSession session, String uuid) {
    mapper(session).deleteByUuid(uuid);
  }

  /**
   * Update all rows with: STATUS='PENDING', STARTED_AT=NULL, UPDATED_AT={now}
   */
  public void resetAllToPendingStatus(DbSession session) {
    mapper(session).resetAllToPendingStatus(system2.now());
  }

  public int countByStatus(DbSession dbSession, CeQueueDto.Status status) {
    return mapper(dbSession).countByStatus(status);
  }

  public int countAll(DbSession dbSession) {
    return mapper(dbSession).countAll();
  }

  public Optional<CeQueueDto> peek(DbSession session) {
    List<String> taskUuids = mapper(session).selectEligibleForPeek();
    if (taskUuids.isEmpty()) {
      return Optional.absent();
    }

    String taskUuid = taskUuids.get(0);
    return tryToPeek(session, taskUuid);
  }

  private Optional<CeQueueDto> tryToPeek(DbSession session, String taskUuid) {
    int touchedRows = mapper(session).updateIfStatus(taskUuid, IN_PROGRESS, system2.now(), system2.now(), PENDING);
    if (touchedRows != 1) {
      session.rollback();
      return Optional.absent();
    }

    CeQueueDto result = mapper(session).selectByUuid(taskUuid);
    session.commit();
    return Optional.of(result);
  }

  private CeQueueMapper mapper(DbSession session) {
    return session.getMapper(CeQueueMapper.class);
  }
}
