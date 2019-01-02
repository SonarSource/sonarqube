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
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class EventComponentChangeDao implements Dao {
  private final System2 system2;

  public EventComponentChangeDao(System2 system2) {
    this.system2 = system2;
  }

  public List<EventComponentChangeDto> selectByEventUuid(DbSession dbSession, String eventUuid) {
    return getMapper(dbSession).selectByEventUuid(eventUuid);
  }

  public List<EventComponentChangeDto> selectByAnalysisUuids(DbSession dbSession, List<String> analyses) {
    return executeLargeInputs(analyses, getMapper(dbSession)::selectByAnalysisUuids);
  }

  public void insert(DbSession dbSession, EventComponentChangeDto dto, EventPurgeData eventPurgeData) {
    getMapper(dbSession)
      .insert(dto, eventPurgeData, system2.now());
  }

  private static EventComponentChangeMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(EventComponentChangeMapper.class);
  }

}
