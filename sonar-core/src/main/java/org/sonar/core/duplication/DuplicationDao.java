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
package org.sonar.core.duplication;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.List;

@BatchSide
@ServerSide
public class DuplicationDao {

  private final MyBatis mybatis;

  public DuplicationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<DuplicationUnitDto> selectCandidates(int resourceSnapshotId, Integer lastSnapshotId, String language) {
    SqlSession session = mybatis.openSession(false);
    try {
      DuplicationMapper mapper = session.getMapper(DuplicationMapper.class);
      return mapper.selectCandidates(resourceSnapshotId, lastSnapshotId, language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Insert rows in the table DUPLICATIONS_INDEX.
   * Note that generated ids are not returned.
   */
  public void insert(Collection<DuplicationUnitDto> units) {
    DbSession session = mybatis.openSession(true);
    try {
      DuplicationMapper mapper = session.getMapper(DuplicationMapper.class);
      for (DuplicationUnitDto unit : units) {
        mapper.batchInsert(unit);
      }
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
