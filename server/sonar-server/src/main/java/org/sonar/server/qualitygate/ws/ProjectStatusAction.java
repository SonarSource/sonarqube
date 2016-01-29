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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.client.qualitygate.ProjectStatusWsRequest;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class ProjectStatusAction implements QGateWsAction {
  private static final String QG_STATUSES_ONE_LINE = Joiner.on(", ")
    .join(Lists.transform(Arrays.asList(ProjectStatusWsResponse.Status.values()), new Function<ProjectStatusWsResponse.Status, String>() {
      @Nonnull
      @Override
      public String apply(@Nonnull ProjectStatusWsResponse.Status input) {
        return input.toString();
      }
    }));
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
    WebService.NewAction action = controller.createAction("project_status")
      .setDescription(String.format("Get the quality gate status of a project or a Compute Engine task.<br />" +
        MSG_ONE_PARAMETER_ONLY + "<br />" +
        "The different statuses returned are: %s. The %s status is returned when there is no quality gate associated with the analysis.<br />" +
        "Returns an HTTP code 404 if the analysis associated with the task is not found or does not exist.<br />" +
        "Requires 'Administer System' or 'Execute Analysis' permission.", QG_STATUSES_ONE_LINE, ProjectStatusWsResponse.Status.NONE))
      .setResponseExample(getClass().getResource("project_status-example.json"))
      .setSince("5.3")
      .setHandler(this);

    action.createParam(PARAM_ANALYSIS_ID)
      .setDescription("Analysis id")
      .setExampleValue("2963");

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
      Optional<String> measureData = getQualityGateDetailsMeasureData(dbSession, projectAndSnapshot.snapshotDto);

      return ProjectStatusWsResponse.newBuilder()
        .setProjectStatus(new QualityGateDetailsFormatter(measureData, projectAndSnapshot.snapshotDto).format())
        .build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private ProjectAndSnapshot getProjectAndSnapshot(DbSession dbSession, ProjectStatusWsRequest request) {
    String snapshotId = request.getAnalysisId();
    if (!isNullOrEmpty(request.getAnalysisId())) {
      return getSnapshotThenProject(dbSession, snapshotId);
    } else if (!isNullOrEmpty(request.getProjectId()) ^ !isNullOrEmpty(request.getProjectKey())) {
      return getProjectThenSnapshot(dbSession, request);
    }

    throw new BadRequestException(MSG_ONE_PARAMETER_ONLY);
  }

  private ProjectAndSnapshot getProjectThenSnapshot(DbSession dbSession, ProjectStatusWsRequest request) {
    ComponentDto projectDto = componentFinder.getByUuidOrKey(dbSession, request.getProjectId(), request.getProjectKey(), ParamNames.PROJECT_ID_AND_KEY);
    SnapshotDto snapshotDto = dbClient.snapshotDao().selectLastSnapshotByComponentId(dbSession, projectDto.getId());
    checkState(snapshotDto != null, "Last analysis of project '%s' not found", projectDto.getKey());
    return new ProjectAndSnapshot(projectDto, snapshotDto);
  }

  private ProjectAndSnapshot getSnapshotThenProject(DbSession dbSession, String snapshotId) {
    SnapshotDto snapshotDto = getSnapshot(dbSession, snapshotId);
    ComponentDto projectDto = dbClient.componentDao().selectOrFailById(dbSession, snapshotDto.getComponentId());
    return new ProjectAndSnapshot(projectDto, snapshotDto);
  }

  private SnapshotDto getSnapshot(DbSession dbSession, String snapshotIdFromRequest) {
    Long snapshotId = null;
    try {
      snapshotId = Long.parseLong(snapshotIdFromRequest);
    } catch (NumberFormatException e) {
      // checks String is a long
    }

    SnapshotDto snapshotDto = null;
    if (snapshotId != null) {
      snapshotDto = dbClient.snapshotDao().selectById(dbSession, snapshotId);
    }

    return checkFound(snapshotDto, "Analysis with id '%s' is not found", snapshotIdFromRequest);
  }

  private Optional<String> getQualityGateDetailsMeasureData(DbSession dbSession, Optional<SnapshotDto> snapshotDto) {
    if (!snapshotDto.isPresent()) {
      return Optional.absent();
    }

    List<MeasureDto> measures = dbClient.measureDao().selectBySnapshotIdAndMetricKeys(snapshotDto.get().getId(),
      Collections.singleton(CoreMetrics.QUALITY_GATE_DETAILS_KEY), dbSession);

    return measures.isEmpty()
      ? Optional.<String>absent()
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
    if (!userSession.hasPermission(SYSTEM_ADMIN)
      && !userSession.hasComponentUuidPermission(SCAN_EXECUTION, projectUuid)) {
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
