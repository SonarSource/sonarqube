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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.6
 */
public class IssueChangeDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public IssueChangeDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<DefaultIssueComment> selectCommentsByIssues(SqlSession session, Collection<String> issueKeys) {
    return selectByIssuesAndType(session, issueKeys, IssueChangeDto.TYPE_COMMENT);
  }

  @CheckForNull
  public DefaultIssueComment selectCommentByKey(String commentKey) {
    SqlSession session = mybatis.openSession();
    try {
      IssueChangeMapper mapper = session.getMapper(IssueChangeMapper.class);
      IssueChangeDto dto = mapper.selectByKeyAndType(commentKey, IssueChangeDto.TYPE_COMMENT);
      return dto != null ? dto.toComment() : null;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  List<DefaultIssueComment> selectByIssuesAndType(SqlSession session, Collection<String> issueKeys, String changeType) {
    Preconditions.checkArgument(issueKeys.size() < 1000, "Number of issue keys is greater than or equal 1000");
    List<DefaultIssueComment> result = Lists.newArrayList();
    if (!issueKeys.isEmpty()) {
      IssueChangeMapper mapper = session.getMapper(IssueChangeMapper.class);
      List<IssueChangeDto> dtos = mapper.selectByIssuesAndType(issueKeys, changeType);
      for (IssueChangeDto dto : dtos) {
        result.add(dto.toComment());
      }
    }
    return result;
  }

  public boolean delete(String key) {
    SqlSession session = mybatis.openSession();
    try {
      IssueChangeMapper mapper = session.getMapper(IssueChangeMapper.class);
      int count = mapper.delete(key);
      session.commit();
      return count == 1;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean update(IssueChangeDto change) {
    SqlSession session = mybatis.openSession();
    try {
      IssueChangeMapper mapper = session.getMapper(IssueChangeMapper.class);
      int count = mapper.update(change);
      session.commit();
      return count == 1;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
