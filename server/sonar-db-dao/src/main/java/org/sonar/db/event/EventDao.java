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
package org.sonar.db.event;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class EventDao implements Dao {

  public Optional<EventDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<EventDto> selectByComponentUuid(DbSession session, String componentUuid) {
    return session.getMapper(EventMapper.class).selectByComponentUuid(componentUuid);
  }

  public List<EventDto> selectByAnalysisUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByAnalysisUuid(uuid);
  }

  public List<EventDto> selectByAnalysisUuids(DbSession dbSession, List<String> analyses) {
    return executeLargeInputs(analyses, mapper(dbSession)::selectByAnalysisUuids);
  }

  public List<EventDto> selectVersionsByMostRecentFirst(DbSession session, String componentUuid) {
    return mapper(session).selectVersions(componentUuid);
  }

  public EventDto insert(DbSession session, EventDto dto) {
    mapper(session).insert(dto);

    return dto;
  }

  public void update(DbSession dbSession, String uuid, @Nullable String name, @Nullable String description) {
    mapper(dbSession).update(uuid, name, description);
  }

  public void delete(DbSession session, Long id) {
    mapper(session).deleteById(id);
  }

  public void delete(DbSession session, String uuid) {
    mapper(session).deleteByUuid(uuid);
  }

  private static EventMapper mapper(DbSession session) {
    return session.getMapper(EventMapper.class);
  }
}
