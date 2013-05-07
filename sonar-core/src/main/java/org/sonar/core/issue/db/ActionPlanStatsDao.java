/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.issue.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;

/**
 * @since 3.6
 */
public class ActionPlanStatsDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public ActionPlanStatsDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  private Collection<ActionPlanStatsDto> findByProjectId(Long projectId, String status, String sort, boolean asc) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ActionPlanStatsMapper.class).findByProjectId(projectId, status, sort, asc);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<ActionPlanStatsDto> findOpenByProjectId(Long projectId) {
      return findByProjectId(projectId, ActionPlan.STATUS_OPEN, "DEADLINE", true);
  }

  public Collection<ActionPlanStatsDto> findClosedByProjectId(Long projectId) {
    return findByProjectId(projectId, ActionPlan.STATUS_CLOSED, "DEADLINE", false);
  }

}
