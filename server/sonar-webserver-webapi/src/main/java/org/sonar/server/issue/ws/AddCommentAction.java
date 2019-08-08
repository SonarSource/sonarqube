/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.io.Resources;
import java.util.Date;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TEXT;

public class AddCommentAction implements IssuesWsAction {

  private final System2 system2;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueUpdater issueUpdater;
  private final IssueFieldsSetter issueFieldsSetter;
  private final OperationResponseWriter responseWriter;

  public AddCommentAction(System2 system2, UserSession userSession, DbClient dbClient, IssueFinder issueFinder, IssueUpdater issueUpdater, IssueFieldsSetter issueFieldsSetter,
    OperationResponseWriter responseWriter) {
    this.system2 = system2;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueUpdater = issueUpdater;
    this.issueFieldsSetter = issueFieldsSetter;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(IssuesWsParameters.ACTION_ADD_COMMENT)
      .setDescription("Add a comment.<br/>" +
        "Requires authentication and the following permission: 'Browse' on the project of the specified issue.")
      .setSince("3.6")
      .setChangelog(
        new Change("6.3", "the response returns the issue with all its details"),
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."))
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "add_comment-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
    action.createParam(PARAM_TEXT)
      .setDescription("Comment text")
      .setRequired(true)
      .setExampleValue("Won't fix because it doesn't apply to the context");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    AddCommentRequest wsRequest = toWsRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = issueFinder.getByKey(dbSession, wsRequest.getIssue());
      IssueChangeContext context = IssueChangeContext.createUser(new Date(system2.now()), userSession.getUuid());
      DefaultIssue defaultIssue = issueDto.toDefaultIssue();
      issueFieldsSetter.addComment(defaultIssue, wsRequest.getText(), context);
      SearchResponseData preloadedSearchResponseData = issueUpdater.saveIssueAndPreloadSearchResponseData(dbSession, defaultIssue, context, false);
      responseWriter.write(defaultIssue.key(), preloadedSearchResponseData, request, response);
    }
  }

  private static AddCommentRequest toWsRequest(Request request) {
    AddCommentRequest wsRequest = new AddCommentRequest(request.mandatoryParam(PARAM_ISSUE), request.mandatoryParam(PARAM_TEXT));
    checkArgument(!isNullOrEmpty(wsRequest.getText()), "Cannot add empty comment to an issue");
    return wsRequest;
  }

  private static class AddCommentRequest {

    private final String issue;
    private final String text;

    private AddCommentRequest(String issue, String text) {
      this.issue = requireNonNull(issue, "Issue key cannot be null");
      this.text = requireNonNull(text, "Text cannot be null");
    }

    public String getIssue() {
      return issue;
    }

    public String getText() {
      return text;
    }
  }
}
