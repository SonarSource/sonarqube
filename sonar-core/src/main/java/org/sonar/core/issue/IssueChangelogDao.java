/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;

/**
 * @since 3.6
 */
public class IssueChangelogDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public IssueChangelogDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(IssueChangeLogDto issueChangeLogDto) {
    SqlSession session = mybatis.openSession();
    IssueChangelogMapper mapper = session.getMapper(IssueChangelogMapper.class);
    try {
      mapper.insert(issueChangeLogDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueChangeLogDto findById(long issueChangeLogId) {
    SqlSession session = mybatis.openSession();
    try {
      IssueChangelogMapper mapper = session.getMapper(IssueChangelogMapper.class);
      return mapper.findById(issueChangeLogId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<IssueChangeLogDto> selectByIssue(String issueUuid) {
    SqlSession session = mybatis.openSession();
    try {
      IssueChangelogMapper mapper = session.getMapper(IssueChangelogMapper.class);
      return mapper.selectByIssue(issueUuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
