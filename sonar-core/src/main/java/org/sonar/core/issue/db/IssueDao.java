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

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.6
 */
public class IssueDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public IssueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public IssueDto selectByKey(String key) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.selectByKey(key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void selectNonClosedIssuesByModule(int componentId, ResultHandler handler) {
    SqlSession session = mybatis.openSession();
    try {
      session.select("org.sonar.core.issue.db.IssueMapper.selectNonClosedIssuesByModule", componentId, handler);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  List<IssueDto> selectIssues(IssueQuery query, @Nullable Integer userId, Integer maxResult) {
    SqlSession session = mybatis.openSession();
    try {
      return selectIssues(query, userId, maxResult, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  List<IssueDto> selectIssues(IssueQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return selectIssues(query, null, Integer.MAX_VALUE, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * The returned IssueDto list contains only the issue id and the sort column
   */
  public List<IssueDto> selectIssues(IssueQuery query, @Nullable Integer userId, SqlSession session){
    return selectIssues(query, userId, query.maxResults(), session);
  }

  private List<IssueDto> selectIssues(IssueQuery query, @Nullable Integer userId, Integer maxResults, SqlSession session){
    IssueMapper mapper = session.getMapper(IssueMapper.class);
    return mapper.selectIssues(query, query.componentRoots(), userId, query.requiredRole(), maxResults);
  }

  @VisibleForTesting
  List<IssueDto> selectByIds(Collection<Long> ids) {
    SqlSession session = mybatis.openSession();
    try {
      return selectByIds(ids, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueDto> selectByIds(Collection<Long> ids, SqlSession session) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    return session.getMapper(IssueMapper.class).selectByIds(ids);
  }
}
