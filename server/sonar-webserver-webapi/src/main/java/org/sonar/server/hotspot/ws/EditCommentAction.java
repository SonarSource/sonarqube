/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.hotspot.ws;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common.Comment;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class EditCommentAction implements HotspotsWsAction {
  private static final String PARAM_COMMENT = "comment";
  private static final String PARAM_TEXT = "text";
  private static final Integer MAXIMUM_COMMENT_LENGTH = 1000;

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final UserSession userSession;
  private final System2 system2;

  public EditCommentAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, UserSession userSession, System2 system2) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.userSession = userSession;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("edit_comment")
      .setDescription("Edit a comment.<br/>" +
        "Requires authentication and the following permission: 'Browse' on the project of the specified hotspot.")
      .setSince("8.2")
      .setHandler(this)
      .setPost(true)
      .setInternal(true)
      .setResponseExample(getClass().getResource("edit-comment-example.json"));

    action.createParam(PARAM_COMMENT)
      .setDescription("Comment key")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
    action.createParam(PARAM_TEXT)
      .setDescription("Comment text")
      .setMaximumLength(MAXIMUM_COMMENT_LENGTH)
      .setRequired(true)
      .setExampleValue("Safe because it doesn't apply to the context");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    hotspotWsSupport.checkLoggedIn();

    String commentKey = request.mandatoryParam(PARAM_COMMENT);
    String text = request.mandatoryParam(PARAM_TEXT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueChangeDto hotspotComment = getHotspotComment(commentKey, dbSession);
      validate(dbSession, hotspotComment);
      editComment(dbSession, hotspotComment, text);
      Comment commentData = prepareResponse(dbSession, hotspotComment);
      writeProtobuf(commentData, request, response);
    }
  }

  private IssueChangeDto getHotspotComment(String commentKey, DbSession dbSession) {
    return dbClient.issueChangeDao().selectCommentByKey(dbSession, commentKey)
      .orElseThrow(() -> new NotFoundException(format("Comment with key '%s' does not exist", commentKey)));
  }

  private void validate(DbSession dbSession, IssueChangeDto issueChangeDto) {
    hotspotWsSupport.loadAndCheckProject(dbSession, issueChangeDto.getIssueKey());
    checkArgument(Objects.equals(issueChangeDto.getUserUuid(), userSession.getUuid()), "You can only edit your own comments");
  }

  private void editComment(DbSession dbSession, IssueChangeDto hotspotComment, String text) {
    hotspotComment.setUpdatedAt(system2.now());
    hotspotComment.setChangeData(text);
    dbClient.issueChangeDao().update(dbSession, hotspotComment);
    dbSession.commit();
  }

  private Comment prepareResponse(DbSession dbSession, IssueChangeDto hotspotComment) {
    Comment.Builder commentBuilder = Comment.newBuilder();
    commentBuilder.clear()
      .setKey(hotspotComment.getKey())
      .setUpdatable(true)
      .setCreatedAt(DateUtils.formatDateTime(new Date(hotspotComment.getIssueChangeCreationDate())));
    getUserByUuid(dbSession, hotspotComment.getUserUuid()).ifPresent(user -> commentBuilder.setLogin(user.getLogin()));
    String markdown = hotspotComment.getChangeData();
    commentBuilder
      .setHtmlText(Markdown.convertToHtml(markdown))
      .setMarkdown(markdown);
    return commentBuilder.build();
  }

  private Optional<UserDto> getUserByUuid(DbSession dbSession, String userUuid) {
    return Optional.ofNullable(dbClient.userDao().selectByUuid(dbSession, userUuid));
  }
}
