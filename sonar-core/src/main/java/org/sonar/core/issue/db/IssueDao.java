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

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static org.sonar.api.utils.DateUtils.dateToLong;

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
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectByKey(key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void selectNonClosedIssuesByModule(int componentId, ResultHandler handler) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.select("org.sonar.core.issue.db.IssueMapper.selectNonClosedIssuesByModule", componentId, handler);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  // TODO replace by aggregation in IssueIndex
  public List<RuleDto> findRulesByComponent(String componentKey, @Nullable Date createdAtOrAfter, DbSession session) {
    return mapper(session).findRulesByComponent(componentKey, dateToLong(createdAtOrAfter));
  }

  // TODO replace by aggregation in IssueIndex
  public List<String> findSeveritiesByComponent(String componentKey, @Nullable Date createdAtOrAfter, DbSession session) {
    return mapper(session).findSeveritiesByComponent(componentKey, dateToLong(createdAtOrAfter));
  }

  protected IssueMapper mapper(DbSession session) {
    return session.getMapper(IssueMapper.class);
  }

}
