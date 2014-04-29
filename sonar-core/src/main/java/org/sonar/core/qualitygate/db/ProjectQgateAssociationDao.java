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

package org.sonar.core.qualitygate.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;
import java.util.Map;

public class ProjectQgateAssociationDao {

  private final MyBatis mybatis;

  public ProjectQgateAssociationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<ProjectQgateAssociationDto> selectProjects(ProjectQgateAssociationQuery query, Long gateId, int offset, int limit) {
    SqlSession session = mybatis.openSession(false);
    try {
      Map<String, Object> params = ImmutableMap.of("query", query, "gateId", gateId.toString());
      return session.selectList("org.sonar.core.qualitygate.db.ProjectQgateAssociationMapper.selectProjects", params, new RowBounds(offset, limit));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  List<ProjectQgateAssociationDto> selectProjects(ProjectQgateAssociationQuery query, Long gateId) {
    return selectProjects(query, gateId, 0, Integer.MAX_VALUE);
  }

}
