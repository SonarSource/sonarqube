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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class AssignAction implements HotspotsWsAction {
  private static final String ACTION_ASSIGN = "assign";

  private static final String PARAM_HOTSPOT_KEY = "hotspot";
  private static final String PARAM_ASSIGNEE = "assignee";
  private static final String PARAM_COMMENT = "comment";

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;

  public AssignAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, IssueFieldsSetter issueFieldsSetter,
    IssueUpdater issueUpdater) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_ASSIGN)
      .setDescription("Assign a hotspot to an active user. Requires authentication and Browse permission on project")
      .setSince("8.2")
      .setHandler(this)
      .setInternal(true)
      .setPost(true)
      .setChangelog(
        new Change("8.9", "Parameter 'assignee' is no longer mandatory"));

    action.createParam(PARAM_HOTSPOT_KEY)
      .setDescription("Hotspot key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_ASSIGNEE)
      .setDescription("Login of the assignee with 'Browse' project permission")
      .setExampleValue("admin");

    action.createParam(PARAM_COMMENT)
      .setDescription("A comment provided with assign action")
      .setExampleValue("Hey Bob! Could you please have a look and confirm my assertion that we are safe here, please");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String assignee = request.param(PARAM_ASSIGNEE);
    String key = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    String comment = request.param(PARAM_COMMENT);

    assign(key, assignee, comment);

    response.noContent();
  }

  private void assign(String hotspotKey, String login, @Nullable String comment) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      IssueDto hotspotDto = hotspotWsSupport.loadHotspot(dbSession, hotspotKey);

      checkHotspotStatusAndResolution(hotspotDto);
      hotspotWsSupport.loadAndCheckProject(dbSession, hotspotDto, UserRole.USER);
      UserDto assignee = isNullOrEmpty(login) ? null : getAssignee(dbSession, login);

      IssueChangeContext context = hotspotWsSupport.newIssueChangeContextWithoutMeasureRefresh();

      DefaultIssue defaultIssue = hotspotDto.toDefaultIssue();

      if (comment != null) {
        issueFieldsSetter.addComment(defaultIssue, comment, context);
      }

      if (assignee != null) {
        checkAssigneeProjectPermission(dbSession, assignee, hotspotDto.getProjectUuid());
      }

      if (issueFieldsSetter.assign(defaultIssue, assignee, context)) {
        issueUpdater.saveIssueAndPreloadSearchResponseData(dbSession, defaultIssue, context);
      }
    }
  }

  private static void checkHotspotStatusAndResolution(IssueDto hotspotDto) {
    if (!STATUS_TO_REVIEW.equals(hotspotDto.getStatus()) && !RESOLUTION_ACKNOWLEDGED.equals(hotspotDto.getResolution())) {
      throw new IllegalArgumentException("Cannot change the assignee of this hotspot given its current status and resolution");
    }
  }

  private UserDto getAssignee(DbSession dbSession, String assignee) {
    return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, assignee), "Unknown user: %s", assignee);
  }

  private void checkAssigneeProjectPermission(DbSession dbSession, UserDto assignee, String issueProjectUuid) {
    ComponentDto componentDto = checkFoundWithOptional(dbClient.componentDao().selectByUuid(dbSession, issueProjectUuid),
      "Could not find project for issue");
    String mainProjectUuid = componentDto.getMainBranchProjectUuid() == null ? componentDto.uuid() : componentDto.getMainBranchProjectUuid();
    if (componentDto.isPrivate() && !hasProjectPermission(dbSession, assignee.getUuid(), mainProjectUuid)) {
      throw new IllegalArgumentException(String.format("Provided user with login '%s' does not have 'Browse' permission to project", assignee.getLogin()));
    }
  }

  private boolean hasProjectPermission(DbSession dbSession, String userUuid, String projectUuid) {
    return dbClient.authorizationDao().selectProjectPermissions(dbSession, projectUuid, userUuid).contains(UserRole.USER);
  }
}
