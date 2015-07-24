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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static java.util.Arrays.asList;

@ServerSide
public class IssueChangeDao implements Dao {

  private final MyBatis mybatis;

  public IssueChangeDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<DefaultIssueComment> selectCommentsByIssues(DbSession session, Collection<String> issueKeys) {
    List<DefaultIssueComment> comments = Lists.newArrayList();
    for (IssueChangeDto dto : selectByTypeAndIssueKeys(session, issueKeys, IssueChangeDto.TYPE_COMMENT)) {
      comments.add(dto.toComment());
    }
    return comments;
  }

  public List<FieldDiffs> selectChangelogByIssue(String issueKey) {
    DbSession session = mybatis.openSession(false);
    try {
      List<FieldDiffs> result = Lists.newArrayList();
      for (IssueChangeDto dto : selectByTypeAndIssueKeys(session, asList(issueKey), IssueChangeDto.TYPE_FIELD_CHANGE)) {
        result.add(dto.toFieldDiffs());
      }
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueChangeDto> selectChangelogOfNonClosedIssuesByComponent(String componentUuid) {
    DbSession session = mybatis.openSession(false);
    try {
      IssueChangeMapper mapper = mapper(session);
      return mapper.selectChangelogOfNonClosedIssuesByComponent(componentUuid, IssueChangeDto.TYPE_FIELD_CHANGE);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public DefaultIssueComment selectCommentByKey(String commentKey) {
    DbSession session = mybatis.openSession(false);
    try {
      IssueChangeMapper mapper = mapper(session);
      IssueChangeDto dto = mapper.selectByKeyAndType(commentKey, IssueChangeDto.TYPE_COMMENT);
      return dto != null ? dto.toComment() : null;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<IssueChangeDto> selectByTypeAndIssueKeys(DbSession session, Collection<String> issueKeys, String changeType) {
    return DatabaseUtils.executeLargeInputs(issueKeys, new SelectByIssueKeys(mapper(session), changeType));
  }

  private static class SelectByIssueKeys implements Function<List<String>, List<IssueChangeDto>> {

    private final IssueChangeMapper mapper;
    private final String changeType;

    private SelectByIssueKeys(IssueChangeMapper mapper, String changeType) {
      this.mapper = mapper;
      this.changeType = changeType;
    }

    @Override
    public List<IssueChangeDto> apply(@Nonnull List<String> issueKeys) {
      return mapper.selectByIssuesAndType(issueKeys, changeType);
    }

  }

  public void insert(DbSession session, IssueChangeDto change) {
    mapper(session).insert(change);
  }

  public boolean delete(String key) {
    DbSession session = mybatis.openSession(false);
    try {
      IssueChangeMapper mapper = mapper(session);
      int count = mapper.delete(key);
      session.commit();
      return count == 1;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean update(IssueChangeDto change) {
    DbSession session = mybatis.openSession(false);
    try {
      IssueChangeMapper mapper = mapper(session);
      int count = mapper.update(change);
      session.commit();
      return count == 1;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private IssueChangeMapper mapper(DbSession session) {
    return session.getMapper(IssueChangeMapper.class);
  }
}
