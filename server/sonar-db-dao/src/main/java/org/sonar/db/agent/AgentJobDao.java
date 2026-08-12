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
package org.sonar.db.agent;

import java.util.Collections;
import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

public class AgentJobDao implements Dao {

  /**
   * Ordered by created_at desc -> newest to oldest
   */
  public List<AgentJobDto> selectByQuery(DbSession dbSession, AgentJobQuery query, Pagination pagination) {
    if (query.isShortCircuitedByJobIds()) {
      return Collections.emptyList();
    }
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public int countByQuery(DbSession dbSession, AgentJobQuery query) {
    if (query.isShortCircuitedByJobIds()) {
      return 0;
    }
    return mapper(dbSession).countByQuery(query);
  }

  private static AgentJobMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(AgentJobMapper.class);
  }
}
