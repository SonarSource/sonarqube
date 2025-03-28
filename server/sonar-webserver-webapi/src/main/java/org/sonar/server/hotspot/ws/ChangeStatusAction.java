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
import javax.annotation.CheckForNull;
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
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.workflow.SecurityHotspotWorkflowTransition;
import org.sonar.server.issue.ws.IssueUpdater;
import org.sonar.server.pushapi.hotspots.HotspotChangeEventService;
import org.sonar.server.pushapi.hotspots.HotspotChangedEvent;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.SECURITY_HOTSPOT_RESOLUTIONS;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.db.component.BranchType.BRANCH;

public class ChangeStatusAction implements HotspotsWsAction {

  private static final String PARAM_HOTSPOT_KEY = "hotspot";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_COMMENT = "comment";

  private final DbClient dbClient;
  private final HotspotWsSupport hotspotWsSupport;
  private final TransitionService transitionService;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final HotspotChangeEventService hotspotChangeEventService;

  public ChangeStatusAction(DbClient dbClient, HotspotWsSupport hotspotWsSupport, TransitionService transitionService,
    IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater, HotspotChangeEventService hotspotChangeEventService) {
    this.dbClient = dbClient;
    this.hotspotWsSupport = hotspotWsSupport;
    this.transitionService = transitionService;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.hotspotChangeEventService = hotspotChangeEventService;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("change_status")
      .setHandler(this)
      .setPost(true)
      .setDescription("Change the status of a Security Hotpot.<br/>" +
        "Requires the 'Administer Security Hotspot' permission.")
      .setSince("8.1")
      .setChangelog(
        new Change("2025.1", String.format("The following '%s' values are not deprecated anymore: %s",
          PARAM_RESOLUTION, String.join(", ", SECURITY_HOTSPOT_RESOLUTIONS))),
        new Change("2025.1", String.format("The following '%s' values are not deprecated anymore: %s",
          PARAM_STATUS, String.join(", ", STATUS_TO_REVIEW, STATUS_REVIEWED))),
        new Change("10.1", "Endpoint visibility change from internal to public"));

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
    action.createParam(PARAM_COMMENT)
      .setDescription("Comment text.")
      .setExampleValue("This is safe because user input is validated by the calling code");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    hotspotWsSupport.checkLoggedIn();

    String hotspotKey = request.mandatoryParam(PARAM_HOTSPOT_KEY);
    String newStatus = request.mandatoryParam(PARAM_STATUS);
    String newResolution = resolutionParam(request, newStatus);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto hotspot = hotspotWsSupport.loadHotspot(dbSession, hotspotKey);
      hotspotWsSupport.loadAndCheckBranch(dbSession, hotspot, ProjectPermission.SECURITYHOTSPOT_ADMIN);

      if (needStatusUpdate(hotspot, newStatus, newResolution)) {
        SecurityHotspotWorkflowTransition transition = toTransition(newStatus, newResolution);
        doTransition(dbSession, hotspot, transition, trimToNull(request.param(PARAM_COMMENT)));
      }
      response.noContent();
    }
  }

  @CheckForNull
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

  private static boolean needStatusUpdate(IssueDto hotspot, String newStatus, @Nullable String newResolution) {
    return !(hotspot.getStatus().equals(newStatus) && Objects.equals(hotspot.getResolution(), newResolution));
  }

  private static SecurityHotspotWorkflowTransition toTransition(String newStatus, @Nullable String newResolution) {
    if (STATUS_TO_REVIEW.equals(newStatus)) {
      return SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW;
    }

    if (STATUS_REVIEWED.equals(newStatus) && RESOLUTION_FIXED.equals(newResolution)) {
      return SecurityHotspotWorkflowTransition.RESOLVE_AS_REVIEWED;
    }

    if (STATUS_REVIEWED.equals(newStatus) && RESOLUTION_ACKNOWLEDGED.equals(newResolution)) {
      return SecurityHotspotWorkflowTransition.RESOLVE_AS_ACKNOWLEDGED;
    }

    return SecurityHotspotWorkflowTransition.RESOLVE_AS_SAFE;
  }

  private void doTransition(DbSession session, IssueDto issueDto, SecurityHotspotWorkflowTransition transition, @Nullable String comment) {
    DefaultIssue defaultIssue = issueDto.toDefaultIssue();
    IssueChangeContext context = hotspotWsSupport.newIssueChangeContextWithMeasureRefresh();
    transitionService.checkTransitionPermission(transition, defaultIssue);
    if (transitionService.doTransition(defaultIssue, context, transition)) {
      if (comment != null) {
        issueFieldsSetter.addComment(defaultIssue, comment, context);
      }

      issueUpdater.saveIssueAndPreloadSearchResponseData(session, issueDto, defaultIssue, context);

      BranchDto branch = issueUpdater.getBranch(session, defaultIssue);
      if (BRANCH.equals(branch.getBranchType())) {
        HotspotChangedEvent hotspotChangedEvent = buildEventData(defaultIssue, issueDto);
        hotspotChangeEventService.distributeHotspotChangedEvent(branch.getProjectUuid(), hotspotChangedEvent);
      }
    }
  }

  private static HotspotChangedEvent buildEventData(DefaultIssue defaultIssue, IssueDto issueDto) {
    return new HotspotChangedEvent.Builder()
      .setKey(defaultIssue.key())
      .setProjectKey(defaultIssue.projectKey())
      .setStatus(defaultIssue.status())
      .setResolution(defaultIssue.resolution())
      .setUpdateDate(defaultIssue.updateDate())
      .setAssignee(issueDto.getAssigneeLogin())
      .setFilePath(issueDto.getFilePath())
      .build();
  }

}
