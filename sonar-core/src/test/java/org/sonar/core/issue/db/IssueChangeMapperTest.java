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

import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

public class IssueChangeMapperTest extends AbstractDaoTestCase {
  SqlSession session;
  IssueChangeMapper mapper;

  @Before
  public void setUp() {
    session = getMyBatis().openSession();
    mapper = session.getMapper(IssueChangeMapper.class);
  }

  @After
  public void tearDown() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void insert_diff() throws Exception {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setKey(null /* no key on field changes */);
    dto.setUserLogin("emmerik");
    dto.setIssueKey("ABCDE");
    dto.setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE);
    dto.setChangeData("severity=INFO|BLOCKER");
    dto.setCreatedAt(1_500_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);
    dto.setIssueChangeCreationDate(1_500_000_000_000L);
    mapper.insert(dto);
    session.commit();

    checkTables("insert_diff", new String[] {"id"}, "issue_changes");
  }

  @Test
  public void insert_comment() throws Exception {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setKey("COMMENT-1234");
    dto.setUserLogin("emmerik");
    dto.setIssueKey("ABCDE");
    dto.setChangeType(IssueChangeDto.TYPE_COMMENT);
    dto.setChangeData("the comment");
    dto.setCreatedAt(1_500_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);
    mapper.insert(dto);
    session.commit();

    checkTables("insert_comment", new String[] {"id"}, "issue_changes");
  }
}
