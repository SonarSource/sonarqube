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
import com.google.common.collect.Lists;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * @since 3.6
 */
public class IssueDao implements BatchComponent, ServerComponent {

  private final static Integer MAX_RESULT = 10000;

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

  public List<IssueDto> selectNonClosedIssuesByRootComponent(int componentId) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.selectNonClosedIssues(componentId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueDto> select(IssueQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.select(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  List<IssueDto> selectIssueAndComponentIds(IssueQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return selectIssueAndComponentIds(query, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * The returned IssueDto list contains only the issue id and the resource id
   */
  public List<IssueDto> selectIssueAndComponentIds(IssueQuery query, SqlSession session) {
    IssueMapper mapper = session.getMapper(IssueMapper.class);
    return mapper.selectIssueAndComponentIds(query);
  }

  @VisibleForTesting
  List<IssueDto> selectIssueAndProjectIds(IssueQuery query, Collection<Integer> authorizedRootProjectIds, Integer maxResults) {
    SqlSession session = mybatis.openSession();
    try {
      return selectIssueAndProjectIds(query, authorizedRootProjectIds, maxResults, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * The returned IssueDto list contains only the issue id and the project id
   */
  public List<IssueDto> selectIssueAndProjectIds(final IssueQuery query, final Collection<Integer> authorizedRootProjectIds, SqlSession session) {
    return selectIssueAndProjectIds(query, authorizedRootProjectIds, query.maxResults(), session);
  }

  private List<IssueDto> selectIssueAndProjectIds(final IssueQuery query, final Collection<Integer> authorizedRootProjectIds, final Integer maxResults, SqlSession session) {
    final List<IssueDto> issues = newArrayList();
    ResultHandler resultHandler = new ResultHandler(){
      @Override
      public void handleResult(ResultContext context) {
        IssueDto issueDto = (IssueDto) context.getResultObject();
        if (authorizedRootProjectIds.contains(issueDto.getProjectId())) {
          issues.add(issueDto);
        }
        if (issues.size() >= maxResults) {
          context.stop();
        }
      }
    };
    session.select("selectIssueAndProjectIds", query, resultHandler);
    return issues;
  }

  @VisibleForTesting
  Collection<IssueDto> selectByIds(Collection<Long> ids, IssueQuery.Sort sort, Boolean asc) {
    SqlSession session = mybatis.openSession();
    try {
      return selectByIds(ids, sort, asc, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<IssueDto> selectByIds(Collection<Long> ids, IssueQuery.Sort sort, Boolean asc, SqlSession session) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    Object idsPartition = Lists.partition(newArrayList(ids), 1000);
    Map<String, Object> params = newHashMap();
    params.put("ids", idsPartition);
    params.put("sort", sort);
    params.put("asc", asc);
    return session.selectList("org.sonar.core.issue.db.IssueMapper.selectByIds", params);
  }
}
