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

package org.sonar.core.issue.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 3.7
 */
@BatchSide
@ServerSide
public class IssueFilterDao implements DaoComponent {

  private final MyBatis mybatis;

  public IssueFilterDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public IssueFilterDto selectById(long id) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.getMapper(IssueFilterMapper.class);
      return getMapper(session).selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterDto> selectByUser(String user) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getMapper(session).selectByUser(user);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterDto> selectFavoriteFiltersByUser(String user) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getMapper(session).selectFavoriteFiltersByUser(user);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueFilterDto selectProvidedFilterByName(String name) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getMapper(session).selectProvidedFilterByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueFilterDto> selectSharedFilters() {
    SqlSession session = mybatis.openSession(false);
    try {
      return getMapper(session).selectSharedFilters();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(IssueFilterDto filter) {
    SqlSession session = mybatis.openSession(false);
    try {
      getMapper(session).insert(filter);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(IssueFilterDto filter) {
    SqlSession session = mybatis.openSession(false);
    try {
      getMapper(session).update(filter);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(long id) {
    SqlSession session = mybatis.openSession(false);
    try {
      getMapper(session).delete(id);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private IssueFilterMapper getMapper(SqlSession session) {
    return session.getMapper(IssueFilterMapper.class);
  }
}
