/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.user.ai;

import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class UserAiToolUsageDao implements Dao {

  public void insert(DbSession dbSession, UserAiToolUsageDto userAiToolUsageDto) {
    mapper(dbSession).insert(userAiToolUsageDto);
  }

  public List<UserAiToolUsageDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public void update(DbSession dbSession, UserAiToolUsageDto userAiToolUsageDto) {
    mapper(dbSession).update(userAiToolUsageDto);
  }

  public void delete(DbSession dbSession, UserAiToolUsageDto userAiToolUsageDto) {
    mapper(dbSession).delete(userAiToolUsageDto);
  }

  private static UserAiToolUsageMapper mapper(DbSession session) {
    return session.getMapper(UserAiToolUsageMapper.class);
  }

}
