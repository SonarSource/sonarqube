/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.SECURITY_HOTSPOT_RESOLUTIONS;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

public class ChangeStatusAction implements HotspotsWsAction {

  private static final String PARAM_HOTSPOT_KEY = "hotspot";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_STATUS = "status";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final TransitionService transitionService;
  private final System2 system2;
  private final IssueUpdater issueUpdater;

  public ChangeStatusAction(DbClient dbClient, UserSession userSession, TransitionService transitionService, System2 system2, IssueUpdater issueUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.transitionService = transitionService;
    this.system2 = system2;
    this.issueUpdater = issueUpdater;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("change_status")
      .setHandler(this)
      .setPost(true)
      .setDescription("Change the status of a Security Hotpot.")
      .setSince("8.1")
      .setInternal(true);

    action.createParam(PARAM_HOTSPOT_KEY)
      .setDescription("Key of the Security Hotspot")
      .setExampleValue(Uuids.UUID_EXAMPLE_03)
      .setRequired(true);
    action.createParam(PARAM_STATUS)
      .setDescription("New status of the Security Hotspot.")
      .setPossibleValues(STATUS_TO_REVIEW, STATUS_REVIEWED)
      .setRequired(true);
    action.createParam(PARAM_RESOLUTION)
      .setDescription("Resolution of the Security Hotspot when new status is " + STATUS_REVIEWED + ", otherwise must not be set.")
      .setPossibleValues(SECURITY_HOTSPOT_RESOLUTIONS);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String hotspotKey = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    String newStatus = request.mandatoryParam(PARAM_STATUS);
    String newResolution = resolutionParam(request, newStatus);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto hotspot = dbClient.issueDao().selectByKey(dbSession, hotspotKey)
        .filter(t -> t.getType() == RuleType.SECURITY_HOTSPOT.getDbConstant())
        .orElseThrow(() -> new NotFoundException(format("Hotspot '%s' does not exist", hotspotKey)));
      loadAndCheckProject(dbSession, hotspot);

      if (needStatusUpdate(hotspot, newStatus, newResolution)) {
        String transitionKey = toTransitionKey(newStatus, newResolution);
        doTransition(dbSession, hotspot, transitionKey);
      }
      response.noContent();
    }
  }

  private static String resolutionParam(Request request, String newStatus) {
    String resolution = request.param(PARAM_RESOLUTION);
    checkArgument(STATUS_REVIEWED.equals(newStatus) || resolution == null,
        "Parameter '%s' must not be specified when Parameter '%s' has value '%s'",
        PARAM_RESOLUTION, PARAM_STATUS, STATUS_TO_REVIEW);
    checkArgument(STATUS_TO_REVIEW.equals(newStatus) || resolution != null,
        "Parameter '%s' must be specified when Parameter '%s' has value '%s'",
        PARAM_RESOLUTION, PARAM_STATUS, STATUS_REVIEWED);
    return resolution;
  }

  private void loadAndCheckProject(DbSession dbSession, IssueDto hotspot) {
    String projectUuid = hotspot.getProjectUuid();
    checkArgument(projectUuid != null, "Hotspot '%s' has no project", hotspot.getKee());

    ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, projectUuid)
        .orElseThrow(() -> new NotFoundException(format("Project with uuid '%s' does not exist", projectUuid)));
    userSession.checkComponentPermission(UserRole.USER, project);
  }

  private static boolean needStatusUpdate(IssueDto hotspot, String newStatus, String newResolution) {
    return !(hotspot.getStatus().equals(newStatus) && Objects.equals(hotspot.getResolution(), newResolution));
  }

  private static String toTransitionKey(String newStatus, String newResolution) {
    if (STATUS_TO_REVIEW.equals(newStatus)) {
      return DefaultTransitions.RESET_AS_TO_REVIEW;
    }
    if (STATUS_REVIEWED.equals(newStatus) && RESOLUTION_FIXED.equals(newResolution)) {
      return DefaultTransitions.RESOLVE_AS_REVIEWED;
    }
    return DefaultTransitions.RESOLVE_AS_SAFE;
  }

  private void doTransition(DbSession session, IssueDto issueDto, String transitionKey) {
    DefaultIssue defaultIssue = issueDto.toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(system2.now()), userSession.getUuid());
    transitionService.checkTransitionPermission(transitionKey, defaultIssue);
    if (transitionService.doTransition(defaultIssue, context, transitionKey)) {
      issueUpdater.saveIssueAndPreloadSearchResponseData(session, defaultIssue, context, true);
    }
  }

}
