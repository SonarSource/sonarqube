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

package org.sonar.server.issue;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssueQueryResult;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.MockUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class IssueCommentServiceTest {

  private IssueUpdater updater;
  private IssueChangeDao changeDao;
  private IssueStorage storage;
  private DefaultIssueFinder finder;
  private IssueNotifications issueNotifications;
  private IssueCommentService issueCommentService;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpUser() {
    MockUserSession.set().setLogin("admin").setPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Before
  public void setUp() {
    updater = mock(IssueUpdater.class);
    changeDao = mock(IssueChangeDao.class);
    storage = mock(IssueStorage.class);

    finder = mock(DefaultIssueFinder.class);
    Issue issue = mock(Issue.class);
    IssueQueryResult result = new DefaultIssueQueryResult(newArrayList(issue));
    stub(finder.find(any(IssueQuery.class))).toReturn(result);

    issueNotifications = mock(IssueNotifications.class);

    issueCommentService = new IssueCommentService(updater, changeDao, storage, finder, issueNotifications);
  }

  @Test
  public void should_find_comment() throws Exception {
    issueCommentService.findComment("ABCD");
    verify(changeDao).selectCommentByKey("ABCD");
  }

  @Test
  public void should_add_comment() throws Exception {
    DefaultIssue issue = mock(DefaultIssue.class);
    when(issue.comments()).thenReturn(Lists.<IssueComment>newArrayList(new DefaultIssueComment()));

    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(Lists.<Issue>newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    issueCommentService.addComment("myIssue", "my comment", MockUserSession.get());

    verify(updater).addComment(eq(issue), eq("my comment"), any(IssueChangeContext.class));
    verify(storage).save(eq(issue));
    verify(issueNotifications).sendChanges(eq(issue), any(IssueChangeContext.class), eq(issueQueryResult), eq("my comment"));
  }

  @Test
  public void should_be_logged_when_adding_comment() throws Exception {
    throwable.expect(UnauthorizedException.class);

    MockUserSession.set().setLogin(null);

    issueCommentService.addComment("myIssue", "my comment", MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verify(storage, never()).save(any(DefaultIssue.class));
    verify(issueNotifications, never()).sendChanges(any(DefaultIssue.class), any(IssueChangeContext.class), any(IssueQueryResult.class), anyString());
  }

  @Test
  public void should_prevent_adding_empty_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", " ", MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verify(storage, never()).save(any(DefaultIssue.class));
    verify(issueNotifications, never()).sendChanges(any(DefaultIssue.class), any(IssueChangeContext.class), any(IssueQueryResult.class), anyString());
  }

  @Test
  public void should_prevent_adding_null_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", null, MockUserSession.get());

    verify(updater, never()).addComment(any(DefaultIssue.class), anyString(), any(IssueChangeContext.class));
    verify(storage, never()).save(any(DefaultIssue.class));
    verify(issueNotifications, never()).sendChanges(any(DefaultIssue.class), any(IssueChangeContext.class), any(IssueQueryResult.class), anyString());
  }

  @Test
  public void should_delete_comment() throws Exception {
    when(changeDao.selectCommentByKey("ABCD")).thenReturn(new DefaultIssueComment().setUserLogin("admin").setIssueKey("EFGH"));

    issueCommentService.deleteComment("ABCD", MockUserSession.get());

    verify(changeDao).delete("ABCD");
    verify(finder).findByKey(eq("EFGH"), eq(UserRole.USER));
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
    verify(finder).findByKey(eq("EFGH"), eq(UserRole.USER));
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
