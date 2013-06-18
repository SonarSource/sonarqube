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
import org.sonar.core.persistence.MyBatis;

/**
 * @since 3.7
 */
public class IssueFilterFavouriteDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public IssueFilterFavouriteDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public IssueFilterFavouriteDto selectById(Long id) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueFilterFavouriteDto selectByUserAndIssueFilterId(String user, Long issueFilterId) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectByIssueFilterId(user, issueFilterId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(IssueFilterFavouriteDto filter) {
    SqlSession session = mybatis.openSession();
    try {
      getMapper(session).insert(filter);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(Long issueFilterFavouriteId) {
    SqlSession session = mybatis.openSession();
    try {
      getMapper(session).delete(issueFilterFavouriteId);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteByIssueFilterId(Long issueFilterId) {
    SqlSession session = mybatis.openSession();
    try {
      getMapper(session).deleteByIssueFilterId(issueFilterId);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private IssueFilterFavouriteMapper getMapper(SqlSession session) {
    return session.getMapper(IssueFilterFavouriteMapper.class);
  }
}
