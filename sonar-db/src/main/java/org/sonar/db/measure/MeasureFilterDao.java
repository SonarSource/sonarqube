/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.measure;

import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class MeasureFilterDao implements Dao {
  private MyBatis mybatis;

  public MeasureFilterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public MeasureFilterDto selectSystemFilterByName(String name) {
    SqlSession session = mybatis.openSession(false);
    try {
      MeasureFilterMapper mapper = session.getMapper(MeasureFilterMapper.class);
      return mapper.findSystemFilterByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public MeasureFilterDto selectById(DbSession session, long id){
    return session.getMapper(MeasureFilterMapper.class).selectById(id);
  }

  public void insert(DbSession session, MeasureFilterDto filter) {
    session.getMapper(MeasureFilterMapper.class).insert(filter);
  }

  public void insert(MeasureFilterDto filter) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, filter);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
