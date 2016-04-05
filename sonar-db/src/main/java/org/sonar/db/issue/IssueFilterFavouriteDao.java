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
package org.sonar.db.issue;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class IssueFilterFavouriteDao implements Dao {

  private final MyBatis mybatis;

  public IssueFilterFavouriteDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public IssueFilterFavouriteDto selectById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  public IssueFilterFavouriteDto selectById(long id) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectById(session, id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterFavouriteDto> selectByFilterId(long filterId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectByFilterId(filterId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @deprecated since 5.2 use {@link #insert(DbSession, IssueFilterFavouriteDto)}
   */
  @Deprecated
  public void insert(IssueFilterFavouriteDto filter) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, filter);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(DbSession session, IssueFilterFavouriteDto filter) {
    mapper(session).insert(filter);
    session.commit();
  }

  public void delete(long id) {
    SqlSession session = mybatis.openSession(false);
    try {
      mapper(session).delete(id);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteByFilterId(long filterId) {
    SqlSession session = mybatis.openSession(false);
    try {
      mapper(session).deleteByFilterId(filterId);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static IssueFilterFavouriteMapper mapper(SqlSession session) {
    return session.getMapper(IssueFilterFavouriteMapper.class);
  }

  public List<IssueFilterFavouriteDto> selectByUser(DbSession dbSession, String login) {
    return mapper(dbSession).selectByUser(login);
  }
}
