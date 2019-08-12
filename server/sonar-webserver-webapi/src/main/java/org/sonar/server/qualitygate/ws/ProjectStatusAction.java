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
package org.sonar.server.qualitygate.ws;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_PROJECT_STATUS;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_BRANCH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ProjectStatusAction implements QualityGatesWsAction {
  private static final String QG_STATUSES_ONE_LINE = Arrays.stream(ProjectStatusResponse.Status.values())
    .map(Enum::toString)
    .collect(Collectors.joining(", "));
  private static final String MSG_ONE_PROJECT_PARAMETER_ONLY = String.format("Either '%s', '%s' or '%s' must be provided", PARAM_ANALYSIS_ID, PARAM_PROJECT_ID, PARAM_PROJECT_KEY);
  private static final String MSG_ONE_BRANCH_PARAMETER_ONLY = String.format("Either '%s' or '%s' can be provided, not both", PARAM_BRANCH, PARAM_PULL_REQUEST);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public ProjectStatusAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_PROJECT_STATUS)
      .setDescription(String.format("Get the quality gate status of a project or a Compute Engine task.<br />" +
        MSG_ONE_PROJECT_PARAMETER_ONLY + "<br />" +
        "The different statuses returned are: %s. The %s status is returned when there is no quality gate associated with the analysis.<br />" +
        "Returns an HTTP code 404 if the analysis associated with the task is not found or does not exist.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>", QG_STATUSES_ONE_LINE, ProjectStatusResponse.Status.NONE))
      .setResponseExample(getClass().getResource("project_status-example.json"))
      .setSince("5.3")
      .setHandler(this)
      .setChangelog(
        new Change("7.7", "The parameters 'branch' and 'pullRequest' were added"),
        new Change("7.6", "The field 'warning' is deprecated from the response"),
        new Change("6.4", "The field 'ignoredConditions' is added to the response"));

    action.createParam(PARAM_ANALYSIS_ID)
      .setDescription("Analysis id")
      .setExampleValue(Uuids.UUID_EXAMPLE_04);

    action.createParam(PARAM_PROJECT_ID)
      .setSince("5.4")
      .setDescription("Project id. Doesn't work with branches or pull requests")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setSince("5.4")
      .setDescription("Project key")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setSince("7.7")
      .setDescription("Branch key")
      .setExampleValue(KeyExamples.KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_PULL_REQUEST)
      .setSince("7.7")
      .setDescription("Pull request id")
      .setExampleValue(KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String analysisId = request.param(PARAM_ANALYSIS_ID);
    String projectId = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);
    String branchKey = request.param(PARAM_BRANCH);
    String pullRequestId = request.param(PARAM_PULL_REQUEST);
    checkRequest(
      !isNullOrEmpty(analysisId)
        ^ !isNullOrEmpty(projectId)
        ^ !isNullOrEmpty(projectKey),
      MSG_ONE_PROJECT_PARAMETER_ONLY);
    checkRequest(isNullOrEmpty(branchKey) || isNullOrEmpty(pullRequestId), MSG_ONE_BRANCH_PARAMETER_ONLY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectStatusResponse projectStatusResponse = doHandle(dbSession, analysisId, projectId, projectKey, branchKey, pullRequestId);
      writeProtobuf(projectStatusResponse, request, response);
    }
  }

  private ProjectStatusResponse doHandle(DbSession dbSession, @Nullable String analysisId, @Nullable String projectId,
    @Nullable String projectKey, @Nullable String branchKey, @Nullable String pullRequestId) {
    ProjectAndSnapshot projectAndSnapshot = getProjectAndSnapshot(dbSession, analysisId, projectId, projectKey, branchKey, pullRequestId);
    checkPermission(projectAndSnapshot.project);
    Optional<String> measureData = loadQualityGateDetails(dbSession, projectAndSnapshot, analysisId != null);

    return ProjectStatusResponse.newBuilder()
      .setProjectStatus(new QualityGateDetailsFormatter(measureData, projectAndSnapshot.snapshotDto).format())
      .build();
  }

  private ProjectAndSnapshot getProjectAndSnapshot(DbSession dbSession, @Nullable String analysisId, @Nullable String projectId,
    @Nullable String projectKey, @Nullable String branchKey, @Nullable String pullRequestId) {
    if (!isNullOrEmpty(analysisId)) {
      return getSnapshotThenProject(dbSession, analysisId);
    }
    if (!isNullOrEmpty(projectId) ^ !isNullOrEmpty(projectKey)) {
      return getProjectThenSnapshot(dbSession, projectId, projectKey, branchKey, pullRequestId);
    }

    throw BadRequestException.create(MSG_ONE_PROJECT_PARAMETER_ONLY);
  }

  private ProjectAndSnapshot getProjectThenSnapshot(DbSession dbSession, @Nullable String projectId, @Nullable String projectKey,
    @Nullable String branchKey, @Nullable String pullRequestId) {
    ComponentDto projectDto;
    if (projectId != null) {
      projectDto = componentFinder.getRootComponentByUuidOrKey(dbSession, projectId, null);
    } else {
      projectDto = componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, projectKey, branchKey, pullRequestId);
    }
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, projectDto.projectUuid());
    return new ProjectAndSnapshot(projectDto, snapshot.orElse(null));
  }

  private ProjectAndSnapshot getSnapshotThenProject(DbSession dbSession, String analysisUuid) {
    SnapshotDto snapshotDto = getSnapshot(dbSession, analysisUuid);
    ComponentDto projectDto = dbClient.componentDao().selectOrFailByUuid(dbSession, snapshotDto.getComponentUuid());
    return new ProjectAndSnapshot(projectDto, snapshotDto);
  }

  private SnapshotDto getSnapshot(DbSession dbSession, String analysisUuid) {
    Optional<SnapshotDto> snapshotDto = dbClient.snapshotDao().selectByUuid(dbSession, analysisUuid);
    return checkFoundWithOptional(snapshotDto, "Analysis with id '%s' is not found", analysisUuid);
  }

  private Optional<String> loadQualityGateDetails(DbSession dbSession, ProjectAndSnapshot projectAndSnapshot, boolean onAnalysis) {
    if (onAnalysis) {
      if (!projectAndSnapshot.snapshotDto.isPresent()) {
        return Optional.empty();
      }
      // get the gate status as it was computed during the specified analysis
      String analysisUuid = projectAndSnapshot.snapshotDto.get().getUuid();
      return dbClient.measureDao().selectMeasure(dbSession, analysisUuid, projectAndSnapshot.project.projectUuid(), CoreMetrics.QUALITY_GATE_DETAILS_KEY)
        .map(MeasureDto::getData);
    }

    // do not restrict to a specified analysis, use the live measure
    Optional<LiveMeasureDto> measure = dbClient.liveMeasureDao().selectMeasure(dbSession, projectAndSnapshot.project.projectUuid(), CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    return measure.map(LiveMeasureDto::getDataAsString);
  }

  private void checkPermission(ComponentDto project) {
    if (!userSession.hasComponentPermission(UserRole.ADMIN, project) &&
      !userSession.hasComponentPermission(UserRole.USER, project)) {
      throw insufficientPrivilegesException();
    }
  }

  @Immutable
  private static class ProjectAndSnapshot {
    private final ComponentDto project;
    private final Optional<SnapshotDto> snapshotDto;

    private ProjectAndSnapshot(ComponentDto project, @Nullable SnapshotDto snapshotDto) {
      this.project = project;
      this.snapshotDto = Optional.ofNullable(snapshotDto);
    }
  }
}
