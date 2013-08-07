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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.core.issue.DefaultIssueQueryResult;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.permission.Permission;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.MockUserSession;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class IssueCommentServiceTest {

  private IssueUpdater updater;
  private IssueChangeDao changeDao;
  private IssueStorage storage;
  private DefaultIssueFinder finder;
  private IssueNotifications issueNotifications;
  private IssueCommentService issueCommentService;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @BeforeClass
  public static void setUpUser() {
    MockUserSession.set().setLogin("admin").setPermissions(Permission.SYSTEM_ADMIN);
  }

  @Before
  public void setUp() {
    updater = mock(IssueUpdater.class);
    changeDao = mock(IssueChangeDao.class);
    storage = mock(IssueStorage.class);

    finder = mock(DefaultIssueFinder.class);
    Issue issue = mock(Issue.class);
    IssueQueryResult result = new DefaultIssueQueryResult(Lists.newArrayList(issue));
    stub(finder.find(any(IssueQuery.class))).toReturn(result);

    issueNotifications = mock(IssueNotifications.class);

    issueCommentService = new IssueCommentService(updater, changeDao, storage, finder, issueNotifications);
  }

  @Test
  public void should_prevent_adding_empty_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", " ", MockUserSession.get());
  }

  @Test
  public void should_prevent_adding_null_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.addComment("myIssue", null, MockUserSession.get());
  }

  @Test
  public void should_prevent_updating_empty_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("myComment", "", MockUserSession.get());
  }

  @Test
  public void should_prevent_updating_null_comment() throws Exception {
    throwable.expect(BadRequestException.class);

    issueCommentService.editComment("myComment", null, MockUserSession.get());
  }
}
