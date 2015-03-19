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

import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueChangeDaoTest extends AbstractDaoTestCase {

  DbSession session;

  IssueChangeDao dao;

  @Before
  public void createDao() {
    session = getMyBatis().openSession(false);
    dao = new IssueChangeDao(getMyBatis());
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select_comments_by_issues() {
    setupData("shared");

    DbSession session = getMyBatis().openSession(false);
    List<DefaultIssueComment> comments = dao.selectCommentsByIssues(session, Arrays.asList("1000"));
    MyBatis.closeQuietly(session);
    assertThat(comments).hasSize(2);

    // chronological order
    DefaultIssueComment first = comments.get(0);
    assertThat(first.markdownText()).isEqualTo("old comment");

    DefaultIssueComment second = comments.get(1);
    assertThat(second.userLogin()).isEqualTo("arthur");
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.markdownText()).isEqualTo("recent comment");
  }

  @Test
  public void select_comments_by_issues_on_huge_number_of_issues() {
    setupData("shared");

    DbSession session = getMyBatis().openSession(false);
    List<String> hugeNbOfIssues = newArrayList();
    for (int i = 0; i < 4500; i++) {
      hugeNbOfIssues.add("ABCD" + i);
    }
    List<DefaultIssueComment> comments = dao.selectCommentsByIssues(session, hugeNbOfIssues);
    MyBatis.closeQuietly(session);

    // The goal of this test is only to check that the query do no fail, not to check the number of results
    assertThat(comments).isEmpty();
  }

  @Test
  public void select_comment_by_key() {
    setupData("shared");

    DefaultIssueComment comment = dao.selectCommentByKey("FGHIJ");
    assertThat(comment).isNotNull();
    assertThat(comment.key()).isEqualTo("FGHIJ");
    assertThat(comment.key()).isEqualTo("FGHIJ");
    assertThat(comment.userLogin()).isEqualTo("arthur");

    assertThat(dao.selectCommentByKey("UNKNOWN")).isNull();
  }

  @Test
  public void select_issue_changelog_from_issue_key() {
    setupData("shared");

    List<FieldDiffs> changelog = dao.selectChangelogByIssue("1000");
    assertThat(changelog).hasSize(1);
    assertThat(changelog.get(0).diffs()).hasSize(1);
    assertThat(changelog.get(0).diffs().get("severity").newValue()).isEqualTo("BLOCKER");
    assertThat(changelog.get(0).diffs().get("severity").oldValue()).isEqualTo("MAJOR");
  }

  @Test
  public void select_issue_changelog_by_module() {
    setupData("select_issue_changelog_by_module");

    // 400 is a non-root module, we should find 2 + 1 changelog from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectChangelogOnNonClosedIssuesByModuleAndType(400, handler);
    assertThat(handler.getResultList()).hasSize(4);

    IssueChangeDto issueChangeDto = (IssueChangeDto) handler.getResultList().get(0);
    assertThat(issueChangeDto.getId()).isNotNull();
    assertThat(issueChangeDto.getKey()).isNotNull();
    assertThat(issueChangeDto.getIssueKey()).isNotNull();
    assertThat(issueChangeDto.getUserLogin()).isNotNull();
    assertThat(issueChangeDto.getChangeType()).isNotNull();
    assertThat(issueChangeDto.getChangeData()).isNotNull();
    assertThat(issueChangeDto.getCreatedAt()).isNotNull();
    assertThat(issueChangeDto.getUpdatedAt()).isNotNull();

    for (Object changeDtoObject : handler.getResultList()) {
      IssueChangeDto changeDto = (IssueChangeDto) changeDtoObject;
      assertThat(changeDto.getChangeType()).isEqualTo(IssueChangeDto.TYPE_FIELD_CHANGE);
    }

    // 399 is the root module, we should only find 1 changelog on itself
    handler = new DefaultResultHandler();
    dao.selectChangelogOnNonClosedIssuesByModuleAndType(399, handler);
    assertThat(handler.getResultList()).hasSize(1);
  }

  @Test
  public void select_issue_changelog_by_module_should_be_sorted_by_creation_date() {
    setupData("select_issue_changelog_by_module_are_sorted_by_creation_date");

    DefaultResultHandler handler = new DefaultResultHandler();
    dao.selectChangelogOnNonClosedIssuesByModuleAndType(399, handler);
    assertThat(handler.getResultList()).hasSize(3);
    assertThat(((IssueChangeDto) handler.getResultList().get(0)).getId()).isEqualTo(1001);
    assertThat(((IssueChangeDto) handler.getResultList().get(1)).getId()).isEqualTo(1002);
    assertThat(((IssueChangeDto) handler.getResultList().get(2)).getId()).isEqualTo(1000);
  }

  @Test
  public void select_comments_by_issues_empty_input() {
    // no need to connect to db
    DbSession session = mock(DbSession.class);
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
  public void insert() {
    setupData("empty");

    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserLogin("emmerik")
      .setChangeData("Some text")
      .setChangeType("comment")
      .setIssueKey("ABCDE")
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_501_000_000_000L)
      .setIssueChangeCreationDate(1_502_000_000_000L);

    dao.insert(session, changeDto);
    session.commit();

    checkTable("insert", "issue_changes");
  }

  @Test
  public void update() {
    setupData("update");

    IssueChangeDto change = new IssueChangeDto();
    change.setKey("COMMENT-2");

    // Only the following fields can be updated:
    change.setChangeData("new comment");
    change.setUpdatedAt(1_500_000_000_000L);

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
    change.setUpdatedAt(DateUtils.parseDate("2013-06-30").getTime());

    assertThat(dao.update(change)).isFalse();
  }
}
