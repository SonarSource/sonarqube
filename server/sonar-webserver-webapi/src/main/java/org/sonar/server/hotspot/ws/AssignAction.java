/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.pushapi.hotspots.HotspotChangeEventService;
import org.sonar.server.pushapi.hotspots.HotspotChangedEvent;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.db.component.BranchType.BRANCH;
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
  private final HotspotChangeEventService hotspotChangeEventService;

  public AssignAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, IssueFieldsSetter issueFieldsSetter,
    IssueUpdater issueUpdater, HotspotChangeEventService hotspotChangeEventService) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.hotspotChangeEventService = hotspotChangeEventService;
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
      hotspotWsSupport.loadAndCheckBranch(dbSession, hotspotDto, ProjectPermission.USER);
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
        issueUpdater.saveIssueAndPreloadSearchResponseData(dbSession, hotspotDto, defaultIssue, context);

        BranchDto branch = issueUpdater.getBranch(dbSession, defaultIssue);
        if (BRANCH.equals(branch.getBranchType())) {
          HotspotChangedEvent hotspotChangedEvent = buildEventData(defaultIssue, assignee, hotspotDto.getFilePath());
          hotspotChangeEventService.distributeHotspotChangedEvent(branch.getProjectUuid(), hotspotChangedEvent);
        }
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

  private void checkAssigneeProjectPermission(DbSession dbSession, UserDto assignee, String issueBranchUuid) {
    ProjectDto project = checkFoundWithOptional(dbClient.projectDao().selectByBranchUuid(dbSession, issueBranchUuid),
      "Could not find branch for issue");

    if (project.isPrivate() && !hasProjectPermission(dbSession, assignee.getUuid(), project.getUuid())) {
      throw new IllegalArgumentException(String.format("Provided user with login '%s' does not have 'Browse' permission to project", assignee.getLogin()));
    }
  }

  private boolean hasProjectPermission(DbSession dbSession, String userUuid, String projectUuid) {
    return dbClient.authorizationDao().selectEntityPermissions(dbSession, projectUuid, userUuid).contains(ProjectPermission.USER.getKey());
  }

  private static HotspotChangedEvent buildEventData(DefaultIssue defaultIssue, @Nullable UserDto assignee, String filePath) {
    return new HotspotChangedEvent.Builder()
      .setKey(defaultIssue.key())
      .setProjectKey(defaultIssue.projectKey())
      .setStatus(defaultIssue.status())
      .setResolution(defaultIssue.resolution())
      .setUpdateDate(defaultIssue.updateDate())
      .setAssignee(assignee == null ? null : assignee.getLogin())
      .setFilePath(filePath)
      .build();
  }
}
