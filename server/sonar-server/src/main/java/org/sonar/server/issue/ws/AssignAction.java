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

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Strings.emptyToNull;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;

public class AssignAction implements IssuesWsAction {

  private static final String DEPRECATED_PARAM_ME = "me";

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
    WebService.NewAction action = controller.createAction(ACTION_ASSIGN)
      .setDescription("Assign/Unassign an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(this)
      .setPost(true);
    // TODO add example of response

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_ASSIGNEE)
      // TODO document absent value for unassign, and "_me" for assigning to me
      .setDescription("Login of the assignee")
      .setExampleValue("admin");
    action.createParam(DEPRECATED_PARAM_ME)
      .setDescription("(deprecated) Assign the issue to the logged-in user. Replaced by the parameter assignee=_me")
      .setDeprecatedSince("5.2")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String assignee = emptyToNull(request.param(PARAM_ASSIGNEE));
    if ("_me".equals(assignee) || BooleanUtils.isTrue(request.paramAsBoolean(DEPRECATED_PARAM_ME))) {
      // Permission is currently checked by IssueService. We still
      // check that user is authenticated in order to get his login.
      userSession.checkLoggedIn();
      assignee = userSession.getLogin();
    }
    String key = request.mandatoryParam(PARAM_ISSUE);
    issueService.assign(key, assignee);

    responseWriter.write(key, request, response);
  }
}
