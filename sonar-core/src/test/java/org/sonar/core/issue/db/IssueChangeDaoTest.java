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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueChangeDaoTest extends AbstractDaoTestCase {

  IssueChangeDao dao;

  @Before
  public void setUp() {
    dao = new IssueChangeDao(getMyBatis());
  }

  @Test
  public void selectCommentsByIssues() {
    setupData("shared");

    SqlSession session = getMyBatis().openSession();
    List<DefaultIssueComment> comments = dao.selectCommentsByIssues(session, Arrays.asList("1000"));
    session.close();
    assertThat(comments).hasSize(2);

    // chronological order
    DefaultIssueComment first = comments.get(0);
    assertThat(first.text()).isEqualTo("old comment");


    DefaultIssueComment second = comments.get(1);
    assertThat(second.userLogin()).isEqualTo("arthur");
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.text()).isEqualTo("recent comment");
  }

  @Test
  public void selectCommentsByIssues_empty_input() {
    // no need to connect to db
    SqlSession session = mock(SqlSession.class);
    List<DefaultIssueComment> comments = dao.selectCommentsByIssues(session, Collections.<String>emptyList());

    assertThat(comments).isEmpty();
  }

  @Test
  public void delete() {
    setupData("delete");

    assertThat(dao.delete("COMMENT-2")).isTrue();

    checkTable("delete", "issue_changes");
  }

  @Test
  public void delete_unknown_key() {
    setupData("delete");

    assertThat(dao.delete("UNKNOWN")).isFalse();
  }

  @Test
  public void update() {
    setupData("update");

    IssueChangeDto change = new IssueChangeDto();
    change.setKey("COMMENT-2");

    // Only the following fields can be updated:
    change.setChangeData("new comment");
    change.setUpdatedAt(DateUtils.parseDate("2013-06-30"));

    assertThat(dao.update(change)).isTrue();

    checkTable("update", "issue_changes");
  }

  @Test
  public void update_unknown_key() {
    setupData("update");

    IssueChangeDto change = new IssueChangeDto();
    change.setKey("UNKNOWN");

    // Only the following fields can be updated:
    change.setChangeData("new comment");
    change.setUpdatedAt(DateUtils.parseDate("2013-06-30"));

    assertThat(dao.update(change)).isFalse();
  }
}
