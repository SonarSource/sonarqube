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
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.List;

/**
 * @since 3.6
 */
public class IssueStatsDao implements ServerComponent {

  private final MyBatis mybatis;

  public IssueStatsDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<Object> selectIssuesColumn(IssueQuery query, String column, @Nullable Integer userId) {
    SqlSession session = mybatis.openSession();
    try {
      IssueStatsMapper mapper = session.getMapper(IssueStatsMapper.class);
      return mapper.selectIssuesColumn(query, column, query.componentRoots(), userId, query.requiredRole());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
