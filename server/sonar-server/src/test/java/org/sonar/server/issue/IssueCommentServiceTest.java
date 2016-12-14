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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    issueCommentService = new IssueCommentService(dbClient, issueService, userSessionRule);
  }

  @Test
  public void should_delete_comment() {
    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("admin").setIssueKey("EFGH"));

    issueCommentService.deleteComment("ABCD");

    verify(changeDao).delete("ABCD");
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_delete_not_found_comment() {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(null);

    issueCommentService.deleteComment("ABCD");

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_prevent_delete_others_comment() {
    throwable.expect(ForbiddenException.class);

    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.deleteComment("ABCD");

    verify(changeDao, never()).delete(anyString());
  }

  @Test
  public void should_update_comment() {
    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setIssueKey("EFGH").setUserLogin("admin"));

    issueCommentService.editComment("ABCD", "updated comment");

    verify(changeDao).update(any(IssueChangeDto.class));
    verify(issueService).getByKey("EFGH");
  }

  @Test
  public void should_not_update_not_found_comment() {
    throwable.expect(NotFoundException.class);

    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(null);

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

    when(changeDao.selectDefaultCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("julien"));

    issueCommentService.editComment("ABCD", "updated comment");

    verify(changeDao, never()).update(any(IssueChangeDto.class));
  }
}
