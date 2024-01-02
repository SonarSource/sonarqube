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
package org.sonar.server.branch.ws;

import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.ProjectBranches;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.branch.ws.BranchesWs.addProjectParam;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_LIST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class ListAction implements BranchWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_LIST)
      .setSince("6.6")
      .setDescription("List the branches of a project or application.<br/>" +
        "Requires 'Browse' or 'Execute analysis' rights on the specified project or application.")
      .setResponseExample(Resources.getResource(getClass(), "list-example.json"))
      .setChangelog(new Change("7.2", "Application can be used on this web service"))
      .setHandler(this);

    addProjectParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto projectOrApp = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
      checkPermission(projectOrApp);

      Collection<BranchDto> branches = dbClient.branchDao().selectByProject(dbSession, projectOrApp).stream()
        .filter(b -> b.getBranchType() == BRANCH)
        .collect(toList());
      List<String> branchUuids = branches.stream().map(BranchDto::getUuid).collect(toList());

      Map<String, LiveMeasureDto> qualityGateMeasuresByComponentUuids = dbClient.liveMeasureDao()
        .selectByComponentUuidsAndMetricKeys(dbSession, branchUuids, singletonList(ALERT_STATUS_KEY)).stream()
        .collect(uniqueIndex(LiveMeasureDto::getComponentUuid));
      Map<String, String> analysisDateByBranchUuid = dbClient.snapshotDao()
        .selectLastAnalysesByRootComponentUuids(dbSession, branchUuids).stream()
        .collect(uniqueIndex(SnapshotDto::getComponentUuid, s -> formatDateTime(s.getCreatedAt())));

      ProjectBranches.ListWsResponse.Builder protobufResponse = ProjectBranches.ListWsResponse.newBuilder();
      branches.forEach(b -> addBranch(protobufResponse, b, qualityGateMeasuresByComponentUuids.get(b.getUuid()),
        analysisDateByBranchUuid.get(b.getUuid())));
      WsUtils.writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private static void addBranch(ProjectBranches.ListWsResponse.Builder response, BranchDto branch,
    @Nullable LiveMeasureDto qualityGateMeasure, @Nullable String analysisDate) {
    ProjectBranches.Branch.Builder builder = toBranchBuilder(branch);
    setBranchStatus(builder, qualityGateMeasure);
    if (analysisDate != null) {
      builder.setAnalysisDate(analysisDate);
    }
    response.addBranches(builder);
  }

  private static ProjectBranches.Branch.Builder toBranchBuilder(BranchDto branch) {
    ProjectBranches.Branch.Builder builder = ProjectBranches.Branch.newBuilder();
    String branchKey = branch.getKey();
    ofNullable(branchKey).ifPresent(builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(Common.BranchType.valueOf(branch.getBranchType().name()));
    builder.setExcludedFromPurge(branch.isExcludeFromPurge());
    return builder;
  }

  private static void setBranchStatus(ProjectBranches.Branch.Builder builder, @Nullable LiveMeasureDto qualityGateMeasure) {
    ProjectBranches.Status.Builder statusBuilder = ProjectBranches.Status.newBuilder();
    if (qualityGateMeasure != null) {
      ofNullable(qualityGateMeasure.getDataAsString()).ifPresent(statusBuilder::setQualityGateStatus);
    }

    builder.setStatus(statusBuilder);
  }

  private void checkPermission(ProjectDto project) {
    if (!userSession.hasProjectPermission(USER, project) &&
      !userSession.hasProjectPermission(UserRole.SCAN, project) &&
      !userSession.hasPermission(SCAN)) {
      throw insufficientPrivilegesException();
    }
  }
}
