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

package org.sonar.db.issue;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class IssueFilterDao implements Dao {

  private final MyBatis mybatis;

  public IssueFilterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public IssueFilterDto selectById(long id) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.getMapper(IssueFilterMapper.class);
      return mapper(session).selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @deprecated since 5.2 use {@link #selectByUser(DbSession, String)}
   */
  @Deprecated
  public List<IssueFilterDto> selectByUser(String user) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByUser(session, user);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterDto> selectByUser(DbSession session, String user) {
    return mapper(session).selectByUser(user);
  }

  public List<IssueFilterDto> selectFavoriteFiltersByUser(String user) {
    SqlSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectFavoriteFiltersByUser(user);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueFilterDto selectProvidedFilterByName(String name) {
    SqlSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectProvidedFilterByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * @deprecated since 5.2 use {@link #selectSharedFilters(DbSession)}
   */
  @Deprecated
  public List<IssueFilterDto> selectSharedFilters() {
    DbSession session = mybatis.openSession(false);
    try {
      return selectSharedFilters(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterDto> selectSharedFilters(DbSession session) {
    return mapper(session).selectSharedFilters();
  }

  /**
   * @deprecated since 5.2 use {@link #insert(DbSession, IssueFilterDto)}
   */
  @Deprecated
  public void insert(IssueFilterDto filter) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, filter);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueFilterDto insert(DbSession session, IssueFilterDto filter) {
    mapper(session).insert(filter);
    session.commit();

    return filter;
  }

  public void update(IssueFilterDto filter) {
    DbSession session = mybatis.openSession(false);
    try {
      mapper(session).update(filter);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(long id) {
    DbSession session = mybatis.openSession(false);
    try {
      mapper(session).delete(id);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private IssueFilterMapper mapper(SqlSession session) {
    return session.getMapper(IssueFilterMapper.class);
  }
}
