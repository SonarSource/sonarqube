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

import com.google.common.base.Preconditions;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;

/**
 * @since 3.6
 */
public class IssueDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public IssueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(IssueDto issueDto) {
    SqlSession session = mybatis.openSession();
    IssueMapper mapper = session.getMapper(IssueMapper.class);
    try {
      mapper.insert(issueDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueDao update(Collection<IssueDto> issues) {
    Preconditions.checkNotNull(issues);

    SqlSession session = mybatis.openBatchSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      for (IssueDto issue : issues) {
        mapper.update(issue);
      }
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueDto findById(long issueId) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.findById(issueId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public IssueDto findByUuid(String uuid) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.findByUuid(uuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<IssueDto> select(IssueQuery issueQuery) {
    SqlSession session = mybatis.openSession();
    try {
      IssueMapper mapper = session.getMapper(IssueMapper.class);
      return mapper.select(issueQuery);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }


}
