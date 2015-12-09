/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.client.qualitygate.ProjectStatusWsRequest;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ProjectStatusAction implements QGateWsAction {
  private static final String QG_STATUSES_ONE_LINE = Joiner.on(", ")
    .join(Lists.transform(Arrays.asList(ProjectStatusWsResponse.Status.values()), new Function<ProjectStatusWsResponse.Status, String>() {
      @Nonnull
      @Override
      public String apply(ProjectStatusWsResponse.Status input) {
        return input.toString();
      }
    }));

  private final DbClient dbClient;
  private final UserSession userSession;

  public ProjectStatusAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("project_status")
      .setDescription(String.format("Quality gate status for a given Compute Engine task. <br />" +
        "The different statuses returned are: %s. The %s status is returned when there is no Quality Gate associated with the analysis.<br />" +
        "Returns a http code 404 if the analysis associated with the task is not found or does not exist.<br />" +
        "Requires 'Administer System' or 'Execute Analysis' permission.", QG_STATUSES_ONE_LINE, ProjectStatusWsResponse.Status.NONE))
      .setResponseExample(getClass().getResource("project_status-example.json"))
      .setSince("5.3")
      .setHandler(this);

    action.createParam("analysisId")
      .setDescription("Analysis id")
      .setRequired(true)
      .setExampleValue("2963");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ProjectStatusWsResponse projectStatusWsResponse = doHandle(toProjectStatusWsRequest(request));
    writeProtobuf(projectStatusWsResponse, request, response);
  }

  private ProjectStatusWsResponse doHandle(ProjectStatusWsRequest request) {
    checkScanOrAdminPermission();

    DbSession dbSession = dbClient.openSession(false);
    try {
      String snapshotId = request.getAnalysisId();
      SnapshotDto snapshotDto = getSnapshot(dbSession, snapshotId);
      String measureData = getQualityGateDetailsMeasureData(dbSession, snapshotDto);

      return ProjectStatusWsResponse.newBuilder()
        .setProjectStatus(new QualityGateDetailsFormatter(measureData, snapshotDto).format())
        .build();
    } finally {
      dbClient.closeSession(dbSession);
    }
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

  @CheckForNull
  private String getQualityGateDetailsMeasureData(DbSession dbSession, SnapshotDto snapshotDto) {
    List<MeasureDto> measures = dbClient.measureDao().selectBySnapshotIdAndMetricKeys(snapshotDto.getId(),
      Collections.singleton(CoreMetrics.QUALITY_GATE_DETAILS_KEY), dbSession);

    return measures.isEmpty()
      ? null
      : measures.get(0).getData();
  }

  private static ProjectStatusWsRequest toProjectStatusWsRequest(Request request) {
    return new ProjectStatusWsRequest()
      .setAnalysisId(request.mandatoryParam("analysisId"));
  }

  private void checkScanOrAdminPermission() {
    userSession.checkAnyGlobalPermissions(newHashSet(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.SYSTEM_ADMIN));
  }
}
