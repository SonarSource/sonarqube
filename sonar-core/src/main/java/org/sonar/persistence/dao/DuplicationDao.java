/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.dao;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.persistence.MyBatis;
import org.sonar.persistence.model.DuplicationMapper;
import org.sonar.persistence.model.DuplicationUnit;

public class DuplicationDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public DuplicationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<DuplicationUnit> selectCandidates(int resourceSnapshotId, Integer lastSnapshotId) {
    SqlSession sqlSession = mybatis.openSession();
    try {
      DuplicationMapper mapper = sqlSession.getMapper(DuplicationMapper.class);
      return mapper.selectCandidates(resourceSnapshotId, lastSnapshotId);
    } finally {
      sqlSession.close();
    }
  }

  public void insert(Collection<DuplicationUnit> units) {
    SqlSession session = mybatis.openSession();
    try {
      DuplicationMapper mapper = session.getMapper(DuplicationMapper.class);
      for (DuplicationUnit unit : units) {
        mapper.insert(unit);
      }
      session.commit();
    } finally {
      session.close();
    }
  }

}
