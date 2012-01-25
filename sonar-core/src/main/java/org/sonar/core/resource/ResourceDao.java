/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.resource;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class ResourceDao {
  private MyBatis mybatis;

  public ResourceDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<Long> getDescendantProjectIdsAndSelf(long projectId) {
    SqlSession session = mybatis.openSession();
    try {
      return getDescendantProjectIdsAndSelf(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<Long> getDescendantProjectIdsAndSelf(long projectId, SqlSession session) {
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    List<Long> ids = Lists.newArrayList();
    appendChildProjectIds(projectId, mapper, ids);
    return ids;
  }

  private void appendChildProjectIds(long projectId, ResourceMapper mapper, List<Long> ids) {
    ids.add(projectId);
    List<Long> subProjectIds = mapper.selectDescendantProjectIds(projectId);
    for (Long subProjectId : subProjectIds) {
      ids.add(subProjectId);
      appendChildProjectIds(subProjectId, mapper, ids);
    }
  }
}
