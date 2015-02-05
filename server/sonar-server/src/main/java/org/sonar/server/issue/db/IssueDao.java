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
package org.sonar.server.issue.db;

import org.apache.ibatis.session.ResultHandler;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;
import java.util.List;

public class IssueDao extends org.sonar.core.issue.db.IssueDao implements DaoComponent {

  public IssueDao(MyBatis mybatis) {
    super(mybatis);
  }

  @CheckForNull
  public IssueDto selectNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public IssueDto selectByKey(DbSession session, String key) {
    IssueDto issue = selectNullableByKey(session, key);
    if (issue == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return issue;
  }

  public List<IssueDto> findByActionPlan(DbSession session, String actionPlan) {
    return mapper(session).selectByActionPlan(actionPlan);
  }

  public List<IssueDto> selectByKeys(DbSession session, List<String> keys) {
    return mapper(session).selectByKeys(keys);
  }

  public void selectNonClosedIssuesByModuleUuid(DbSession session, String moduleUuid, ResultHandler handler) {
    session.select("org.sonar.core.issue.db.IssueMapper.selectNonClosedIssuesByModuleUuid", moduleUuid, handler);
  }

  public void selectNonClosedIssuesByProjectUuid(DbSession session, String projectUuid, ResultHandler handler) {
    session.select("org.sonar.core.issue.db.IssueMapper.selectNonClosedIssuesByProjectUuid", projectUuid, handler);
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
}
