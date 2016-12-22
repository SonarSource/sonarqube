/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.client.qualitygate.ProjectStatusWsRequest;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_PROJECT_STATUS;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class ProjectStatusAction implements QualityGatesWsAction {
  private static final String QG_STATUSES_ONE_LINE = Arrays.stream(ProjectStatusWsResponse.Status.values())
    .map(Enum::toString)
    .collect(Collectors.joining(", "));
  private static final String MSG_ONE_PARAMETER_ONLY = String.format("One (and only one) of the following parameters must be provided '%s', '%s', '%s'",
    PARAM_ANALYSIS_ID, PARAM_PROJECT_ID, PARAM_PROJECT_KEY);

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
        MSG_ONE_PARAMETER_ONLY + "<br />" +
        "The different statuses returned are: %s. The %s status is returned when there is no quality gate associated with the analysis.<br />" +
        "Returns an HTTP code 404 if the analysis associated with the task is not found or does not exist.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>", QG_STATUSES_ONE_LINE, ProjectStatusWsResponse.Status.NONE))
      .setResponseExample(getClass().getResource("project_status-example.json"))
      .setSince("5.3")
      .setHandler(this);

    action.createParam(PARAM_ANALYSIS_ID)
      .setDescription("Analysis id")
      .setExampleValue(Uuids.UUID_EXAMPLE_04);

    action.createParam(PARAM_PROJECT_ID)
      .setSince("5.4")
      .setDescription("Project id")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setSince("5.4")
      .setDescription("Project key")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ProjectStatusWsResponse projectStatusWsResponse = doHandle(toProjectStatusWsRequest(request));
    writeProtobuf(projectStatusWsResponse, request, response);
  }

  private ProjectStatusWsResponse doHandle(ProjectStatusWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ProjectAndSnapshot projectAndSnapshot = getProjectAndSnapshot(dbSession, request);
      checkPermission(projectAndSnapshot.project.uuid());
      Optional<String> measureData = getQualityGateDetailsMeasureData(dbSession, projectAndSnapshot.project);

      return ProjectStatusWsResponse.newBuilder()
        .setProjectStatus(new QualityGateDetailsFormatter(measureData, projectAndSnapshot.snapshotDto).format())
        .build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private ProjectAndSnapshot getProjectAndSnapshot(DbSession dbSession, ProjectStatusWsRequest request) {
    String analysisUuid = request.getAnalysisId();
    if (!isNullOrEmpty(request.getAnalysisId())) {
      return getSnapshotThenProject(dbSession, analysisUuid);
    } else if (!isNullOrEmpty(request.getProjectId()) ^ !isNullOrEmpty(request.getProjectKey())) {
      return getProjectThenSnapshot(dbSession, request);
    }

    throw new BadRequestException(MSG_ONE_PARAMETER_ONLY);
  }

  private ProjectAndSnapshot getProjectThenSnapshot(DbSession dbSession, ProjectStatusWsRequest request) {
    ComponentDto projectDto = componentFinder.getByUuidOrKey(dbSession, request.getProjectId(), request.getProjectKey(), ParamNames.PROJECT_ID_AND_KEY);
    java.util.Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, projectDto.projectUuid());
    return new ProjectAndSnapshot(projectDto, snapshot.orElse(null));
  }

  private ProjectAndSnapshot getSnapshotThenProject(DbSession dbSession, String analysisUuid) {
    SnapshotDto snapshotDto = getSnapshot(dbSession, analysisUuid);
    ComponentDto projectDto = dbClient.componentDao().selectOrFailByUuid(dbSession, snapshotDto.getComponentUuid());
    return new ProjectAndSnapshot(projectDto, snapshotDto);
  }

  private SnapshotDto getSnapshot(DbSession dbSession, String analysisUuid) {
    java.util.Optional<SnapshotDto> snapshotDto = dbClient.snapshotDao().selectByUuid(dbSession, analysisUuid);
    return checkFoundWithOptional(snapshotDto, "Analysis with id '%s' is not found", analysisUuid);
  }

  private Optional<String> getQualityGateDetailsMeasureData(DbSession dbSession, ComponentDto project) {
    MeasureQuery measureQuery = MeasureQuery.builder()
      .setProjectUuids(singletonList(project.projectUuid()))
      .setMetricKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY)
      .build();
    List<MeasureDto> measures = dbClient.measureDao().selectByQuery(dbSession, measureQuery);

    return measures.isEmpty()
      ? Optional.absent()
      : Optional.fromNullable(measures.get(0).getData());
  }

  private static ProjectStatusWsRequest toProjectStatusWsRequest(Request request) {
    ProjectStatusWsRequest projectStatusWsRequest = new ProjectStatusWsRequest()
      .setAnalysisId(request.param(PARAM_ANALYSIS_ID))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY));
    checkRequest(
      !isNullOrEmpty(projectStatusWsRequest.getAnalysisId())
        ^ !isNullOrEmpty(projectStatusWsRequest.getProjectId())
        ^ !isNullOrEmpty(projectStatusWsRequest.getProjectKey()),
      MSG_ONE_PARAMETER_ONLY);
    return projectStatusWsRequest;
  }

  private void checkPermission(String projectUuid) {
    if (!userSession.hasComponentUuidPermission(UserRole.ADMIN, projectUuid) &&
      !userSession.hasComponentUuidPermission(UserRole.USER, projectUuid)) {
      throw insufficientPrivilegesException();
    }
  }

  private static class ProjectAndSnapshot {
    private final ComponentDto project;
    private final Optional<SnapshotDto> snapshotDto;

    private ProjectAndSnapshot(ComponentDto project, @Nullable SnapshotDto snapshotDto) {
      this.project = project;
      this.snapshotDto = Optional.fromNullable(snapshotDto);
    }
  }
}
