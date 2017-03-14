/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Strings.emptyToNull;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;

public class AssignAction implements IssuesWsAction {

  private static final String DEPRECATED_PARAM_ME = "me";
  private static final String ASSIGN_TO_ME_VALUE = "_me";

  private final System2 system2;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final OperationResponseWriter responseWriter;

  public AssignAction(System2 system2, UserSession userSession, DbClient dbClient, IssueFinder issueFinder, IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater,
    OperationResponseWriter responseWriter) {
    this.system2 = system2;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_ASSIGN)
      .setDescription("Assign/Unassign an issue. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "assign-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_ASSIGNEE)
      .setDescription("Login of the assignee. When not set, it will unassign the issue. Use '%s' to assign to current user", ASSIGN_TO_ME_VALUE)
      .setExampleValue("admin");
    action.createParam(DEPRECATED_PARAM_ME)
      .setDescription("(deprecated) Assign the issue to the logged-in user. Replaced by the parameter assignee=_me")
      .setDeprecatedSince("5.2")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String assignee = getAssignee(request);
    String key = request.mandatoryParam(PARAM_ISSUE);
    assign(key, assignee);
    responseWriter.write(key, request, response);
  }

  private void assign(String issueKey, @Nullable String assignee) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      DefaultIssue issue = issueFinder.getByKey(dbSession, issueKey).toDefaultIssue();
      UserDto user = getUser(dbSession, assignee);
      IssueChangeContext context = IssueChangeContext.createUser(new Date(system2.now()), userSession.getLogin());
      if (issueFieldsSetter.assign(issue, user, context)) {
        issueUpdater.saveIssue(dbSession, issue, context, null);
      }
    }
  }

  @CheckForNull
  private String getAssignee(Request request) {
    String assignee = emptyToNull(request.param(PARAM_ASSIGNEE));
    if (ASSIGN_TO_ME_VALUE.equals(assignee) || BooleanUtils.isTrue(request.paramAsBoolean(DEPRECATED_PARAM_ME))) {
      return userSession.getLogin();
    }
    return assignee;
  }

  @CheckForNull
  private UserDto getUser(DbSession dbSession, @Nullable String assignee) {
    if (Strings.isNullOrEmpty(assignee)) {
      return null;
    }
    UserDto user = dbClient.userDao().selectByLogin(dbSession, assignee);
    return checkFound(user != null && user.isActive() ? user : null, "Unknown user: %s", assignee);
  }
}
