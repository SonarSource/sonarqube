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
package org.sonar.server.dashboard.db;

import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.dashboard.DashboardMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class DashboardDao implements DaoComponent {

  @CheckForNull
  public DashboardDto getNullableByKey(DbSession session, Long key) {
    return mapper(session).selectById(key);
  }

  /**
   * Get dashboard if allowed : shared or owned by logged-in user
   * @param userId id of logged-in user, null if anonymous
   */
  @CheckForNull
  public DashboardDto getAllowedByKey(DbSession session, Long key, @Nullable Long userId) {
    return mapper(session).selectAllowedById(key, userId != null ? userId : -1L);
  }

  private DashboardMapper mapper(DbSession session) {
    return session.getMapper(DashboardMapper.class);
  }

}
