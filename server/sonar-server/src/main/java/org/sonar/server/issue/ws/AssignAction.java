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
package org.sonar.server.issue.ws;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Strings.emptyToNull;

public class AssignAction implements IssuesWsAction {

  public static final String ASSIGN_ACTION = "assign";

  private final UserSession userSession;
  private final IssueService issueService;
  private final OperationResponseWriter responseWriter;

  public AssignAction(UserSession userSession, IssueService issueService, OperationResponseWriter responseWriter) {
    this.userSession = userSession;
    this.issueService = issueService;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ASSIGN_ACTION)
      .setDescription("Assign/Unassign an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(this)
      .setPost(true);
    // TODO add example of response

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("assignee")
      // TODO document absent value for unassign, and "_me" for assigning to me
      .setDescription("Login of the assignee")
      .setExampleValue("admin");
    action.createParam("me")
      .setDescription("(deprecated) Assign the issue to the logged-in user. Replaced by the parameter assignee=_me")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String assignee = emptyToNull(request.param("assignee"));
    if ("_me".equals(assignee) || BooleanUtils.isTrue(request.paramAsBoolean("me"))) {
      // Permission is currently checked by IssueService. We still
      // check that user is authenticated in order to get his login.
      userSession.checkLoggedIn();
      assignee = userSession.getLogin();
    }
    String key = request.mandatoryParam("issue");
    issueService.assign(key, assignee);

    responseWriter.write(key, request, response);
  }
}
