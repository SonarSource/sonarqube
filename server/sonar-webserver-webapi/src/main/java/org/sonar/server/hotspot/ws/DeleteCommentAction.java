/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Objects;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;

public class DeleteCommentAction implements HotspotsWsAction {
  private static final String PARAM_COMMENT = "comment";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;

  public DeleteCommentAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, UserSession userSession) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("delete_comment")
      .setHandler(this)
      .setPost(true)
      .setDescription("Delete comment from Security Hotpot.<br/>" +
        "Requires authentication and the following permission: 'Browse' on the project of the specified Security Hotspot.")
      .setSince("8.2")
      .setInternal(true);

    action.createParam(PARAM_COMMENT)
      .setDescription("Comment key.")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    hotspotWsSupport.checkLoggedIn();

    String commentKey = request.mandatoryParam(PARAM_COMMENT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueChangeDto hotspotComment = getHotspotComment(commentKey, dbSession);
      validate(dbSession, hotspotComment);
      deleteComment(dbSession, hotspotComment.getKey());
      response.noContent();
    }
  }

  private IssueChangeDto getHotspotComment(String commentKey, DbSession dbSession) {
    return dbClient.issueChangeDao().selectCommentByKey(dbSession, commentKey)
      .orElseThrow(() -> new NotFoundException(format("Comment with key '%s' does not exist", commentKey)));
  }

  private void validate(DbSession dbSession, IssueChangeDto issueChangeDto) {
    hotspotWsSupport.loadAndCheckBranch(dbSession, issueChangeDto.getIssueKey());
    checkArgument(Objects.equals(issueChangeDto.getUserUuid(), userSession.getUuid()), "You can only delete your own comments");
  }

  private void deleteComment(DbSession dbSession, String commentKey) {
    dbClient.issueChangeDao().deleteByKey(dbSession, commentKey);
  }
}
