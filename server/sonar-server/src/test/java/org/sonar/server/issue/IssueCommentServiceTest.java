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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.user.MockUserSession;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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

  @Before
  public void setUpUser() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(session);

    issueCommentService = new IssueCommentService(dbClient, issueService, updater, changeDao);
  }

  @Test
  public void find_comments() throws Exception {
    issueCommentService.findComments("ABCD");
    verify(changeDao).selectCommentsByIssues(session, newArrayList("ABCD"));
  }

  @Test
  public void should_find_comment() throws Exception {
    issueCommentService.findComment("ABCD");
    verify(changeDao).selectCommentByKey("ABCD");
  }

  @Test
  public void should_add_comment() throws Exception {
    IssueDto issueDto = IssueTesting.newDto(RuleTesting.newXooX1().setId(500), ComponentTesting.newFileDto(ComponentTesting.newProjectDto()), ComponentTesting.newProjectDto());
    when(issueService.getByKeyForUpdate(session, "ABCD")).thenReturn(issueDto);
    when(issueCommentService.findComments(session, "ABCD")).thenReturn(newArrayList(new DefaultIssueComment()));

    issueCommentService.addComment("ABCD", "my comment", MockUserSession.get());

    verify(updater).addComment(eq(issueDto.toDefaultIssue()), eq("my comment"), any(IssueChangeContext.class));
    verify(issueService).saveIssue(eq(session), eq(issueDto.toDefaultIssue()), any(IssueChangeContext.class), eq("my comment"));
  }

  @Test
  public void should_be_logged_when_adding_comment() throws Exception {
    throwable.expect(UnauthorizedException.class);

    MockUserSession.set().setLogin(null);

    issueCommentService.addComment("myIssue", "my comment", MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void should_prevent_adding_empty_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", " ", MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void should_prevent_adding_null_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", null, MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verifyZeroInteractions(issueService);
  }

  @Test
  public void fail_if_comment_not_inserted_in_db() throws Exception {
    IssueDto issueDto = IssueTesting.newDto(RuleTesting.newXooX1().setId(500), ComponentTesting.newFileDto(ComponentTesting.newProjectDto()), ComponentTesting.newProjectDto());
    when(issueService.getByKeyForUpdate(session, "ABCD")).thenReturn(issueDto);
    // Comment has not be inserted in db
    when(issueCommentService.findComments(session, "ABCD")).thenReturn(Collections.<DefaultIssueComment>emptyList());

    try {
      issueCommentService.addComment("ABCD", "my comment", MockUserSession.get());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Fail to add a comment on issue ABCD");
    }
  }

  @Test
  public void should_delete_comment() throws Exception {
    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("admin").setIssueKey("EFGH"));

    issueCommentService.deleteComment("ABCD", MockUserSession.get());

    verify(changeDao).delete("ABCD");
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_delete_not_found_comment() throws Exception {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(null);

    issueCommentService.deleteComment("ABCD", MockUserSession.get());

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_prevent_delete_others_comment() throws Exception {
    throwable.expect(ForbiddenException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.deleteComment("ABCD", MockUserSession.get());

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_update_comment() throws Exception {
    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setIssueKey("EFGH").setUserLogin("admin"));

    issueCommentService.editComment("ABCD", "updated comment", MockUserSession.get());

    verify(changeDao).update(any(IssueChangeDto.class));
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_update_not_found_comment() throws Exception {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(null);

    issueCommentService.editComment("ABCD", "updated comment", MockUserSession.get());

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_empty_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("ABCD", "", MockUserSession.get());

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_null_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("ABCD", null, MockUserSession.get());

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }

  @Test
  public void should_prevent_updating_others_comment() throws Exception {
    throwable.expect(ForbiddenException.class);

    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.editComment("ABCD", "updated comment", MockUserSession.get());

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }
}
