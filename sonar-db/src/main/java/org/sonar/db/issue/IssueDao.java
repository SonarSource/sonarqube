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
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class IssueDao implements Dao {

  private final MyBatis mybatis;

  public IssueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void selectNonClosedIssuesByModule(long componentId, ResultHandler handler) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.select("org.sonar.db.issue.IssueMapper.selectNonClosedIssuesByModule", componentId, handler);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public IssueDto selectNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public IssueDto selectByKey(DbSession session, String key) {
    IssueDto issue = selectNullableByKey(session, key);
    if (issue == null) {
      throw new IllegalArgumentException(String.format("Issue key '%s' does not exist", key));
    }
    return issue;
  }

  public List<IssueDto> findByActionPlan(DbSession session, String actionPlan) {
    return mapper(session).selectByActionPlan(actionPlan);
  }

  public List<IssueDto> selectByKeys(DbSession session, List<String> keys) {
    return mapper(session).selectByKeys(keys);
  }

  public Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(DbSession session, String projectUuid) {
    return mapper(session).selectComponentUuidsOfOpenIssuesForProjectUuid(projectUuid);
  }

  public void insert(DbSession session, IssueDto dto) {
    mapper(session).insert(dto);
  }

  public void insert(DbSession session, IssueDto dto, IssueDto... others) {
    IssueMapper mapper = mapper(session);
    mapper.insert(dto);
    for (IssueDto other : others) {
      mapper.insert(other);
    }
  }

  public void update(DbSession session, IssueDto dto) {
    mapper(session).update(dto);
  }

  private IssueMapper mapper(DbSession session) {
    return session.getMapper(IssueMapper.class);
  }

}
