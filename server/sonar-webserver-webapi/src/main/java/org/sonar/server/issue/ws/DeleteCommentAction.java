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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_DELETE_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMMENT;

public class DeleteCommentAction implements IssuesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final OperationResponseWriter responseWriter;

  public DeleteCommentAction(UserSession userSession, DbClient dbClient, IssueFinder issueFinder, OperationResponseWriter responseWriter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_DELETE_COMMENT)
      .setDescription("Delete a comment.<br/>" +
        "Requires authentication and the following permission: 'Browse' on the project of the specified issue.")
      .setSince("3.6")
      .setChangelog(
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."),
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.3", "the response returns the issue with all its details"),
        new Change("6.3", "the 'key' parameter is renamed 'comment'"))
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "delete_comment-example.json"))
      .setPost(true);

    action.createParam(PARAM_COMMENT)
      .setDescription("Comment key")
      .setDeprecatedKey("key", "6.3")
      .setSince("6.3")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = Stream.of(request)
        .map(loadCommentData(dbSession))
        .peek(deleteComment(dbSession))
        .collect(MoreCollectors.toOneElement())
        .getIssueDto();
      responseWriter.write(issueDto.getKey(), new SearchResponseData(issueDto), request, response);
    }
  }

  private Function<Request, CommentData> loadCommentData(DbSession dbSession) {
    return request -> new CommentData(dbSession, request.mandatoryParam(PARAM_COMMENT));
  }

  private Consumer<CommentData> deleteComment(DbSession dbSession) {
    return commentData -> {
      dbClient.issueChangeDao().delete(dbSession, commentData.getIssueChangeDto().getKey());
      dbSession.commit();
    };
  }

  private class CommentData {
    private final IssueChangeDto issueChangeDto;
    private final IssueDto issueDto;

    CommentData(DbSession dbSession, String commentKey) {
      this.issueChangeDto = dbClient.issueChangeDao().selectCommentByKey(dbSession, commentKey)
        .orElseThrow(() -> new NotFoundException(format("Comment with key '%s' does not exist", commentKey)));
      // Load issue now to quickly fail if user hasn't permission to see it
      this.issueDto = issueFinder.getByKey(dbSession, issueChangeDto.getIssueKey());
      checkArgument(Objects.equals(issueChangeDto.getUserUuid(), userSession.getUuid()), "You can only delete your own comments");
    }

    IssueChangeDto getIssueChangeDto() {
      return issueChangeDto;
    }

    IssueDto getIssueDto() {
      return issueDto;
    }
  }

}
