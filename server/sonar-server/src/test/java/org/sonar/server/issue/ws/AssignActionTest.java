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
package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AssignActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  IssueService issueService = mock(IssueService.class);
  OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  AssignAction underTest = new AssignAction(userSession, issueService, responseWriter);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void assign_to_me() throws Exception {
    userSession.login("perceval");

    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("assignee", "_me")
      .execute();

    verify(issueService).assign("ABC", "perceval");
    verify(responseWriter).write(eq("ABC"), any(Request.class), any(Response.class));
  }

  @Test
  public void assign_to_me_with_deprecated_param() throws Exception {
    userSession.login("perceval");

    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("me", "true")
      .execute();

    verify(issueService).assign("ABC", "perceval");
    verify(responseWriter).write(eq("ABC"), any(Request.class), any(Response.class));
  }

  @Test
  public void assign_to_someone() throws Exception {
    userSession.login("perceval");

    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("assignee", "arthur")
      .execute();

    verify(issueService).assign("ABC", "arthur");
    verify(responseWriter).write(eq("ABC"), any(Request.class), any(Response.class));
  }

  @Test
  public void must_be_authenticated_to_assign_to_me() throws Exception {
    expectedException.expect(UnauthorizedException.class);

    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("assignee", "_me")
      .execute();
  }

  @Test
  public void unassign() throws Exception {
    userSession.login("perceval");

    tester.newRequest()
      .setParam("issue", "ABC")
      .execute();

    verify(issueService).assign("ABC", null);
    verify(responseWriter).write(eq("ABC"), any(Request.class), any(Response.class));
  }
}
