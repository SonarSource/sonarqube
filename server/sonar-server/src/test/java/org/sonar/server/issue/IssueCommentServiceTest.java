/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueCommentServiceTest {

  @Mock
  private DbClient dbClient;

  @Mock
  private DbSession session;

  @Mock
  private IssueService issueService;

  @Mock
  private IssueUpdater updater;

  @Mock
  private IssueChangeDao changeDao;

  @Mock
  private IssueCommentService issueCommentService;

  @Rule
  public ExpectedException throwable = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.issueChangeDao()).thenReturn(changeDao);

    issueCommentService = new IssueCommentService(dbClient, issueService, updater, userSessionRule);
  }

  @Test
  public void find_comments() {
    issueCommentService.findComments("ABCD");
    verify(changeDao).selectCommentsByIssues(session, newArrayList("ABCD"));
  }

  @Test
  public void should_find_comment() {
    issueCommentService.findComment("ABCD");
    verify(changeDao).selectCommentByKey("ABCD");
  }

  @Test
  public void should_add_comment() {
    IssueDto issueDto = IssueTesting.newDto(RuleTesting.newXooX1().setId(500), ComponentTesting.newFileDto(ComponentTesting.newProjectDto(), null), ComponentTesting.newProjectDto());
    when(issueService.getByKeyForUpdate(session, "ABCD")).thenReturn(issueDto);
    when(issueCommentService.findComments(session, "ABCD")).thenReturn(newArrayList(new DefaultIssueComment()));

    issueCommentService.addComment("ABCD", "my comment");

    verify(updater).addComment(eq(issueDto.toDefaultIssue()), eq("my comment"), any(IssueChangeContext.class));
    verify(issueService).saveIssue(eq(session), eq(issueDto.toDefaultIssue()), any(IssueChangeContext.class), eq("my comment"));
  }

  @Test
  public void should_be_logged_when_adding_comment() {
    throwable.expect(UnauthorizedException.class);
    userSessionRule.anonymous();

    issueCommentService.addComment("myIssue", "my comment");

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void should_prevent_adding_empty_comment() {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", " ");

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void should_prevent_adding_null_comment() {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", null);

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void fail_if_comment_not_inserted_in_db() {
    IssueDto issueDto = IssueTesting.newDto(RuleTesting.newXooX1().setId(500), ComponentTesting.newFileDto(ComponentTesting.newProjectDto(), null), ComponentTesting.newProjectDto());
    when(issueService.getByKeyForUpdate(session, "ABCD")).thenReturn(issueDto);
    // Comment has not be inserted in db
    when(issueCommentService.findComments(session, "ABCD")).thenReturn(Collections.<DefaultIssueComment>emptyList());

    try {
      issueCommentService.addComment("ABCD", "my comment");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Fail to add a comment on issue ABCD");
    }
  }

  @Test
  public void should_delete_comment() {
    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("admin").setIssueKey("EFGH"));

    issueCommentService.deleteComment("ABCD");

    verify(changeDao).delete("ABCD");
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_delete_not_found_comment() {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(null);

    issueCommentService.deleteComment("ABCD");

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_prevent_delete_others_comment() {
    throwable.expect(ForbiddenException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.deleteComment("ABCD");

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_update_comment() {
    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setIssueKey("EFGH").setUserLogin("admin"));

    issueCommentService.editComment("ABCD", "updated comment");

    verify(changeDao).update(any(IssueChangeDto.class));
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_update_not_found_comment() {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(null);

    issueCommentService.editComment("ABCD", "updated comment");

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_empty_comment() {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("ABCD", "");

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_null_comment() {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("ABCD", null);

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_others_comment() {
    throwable.expect(ForbiddenException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.editComment("ABCD", "updated comment");

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }
}
