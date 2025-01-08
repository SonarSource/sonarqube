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
package org.sonar.server.ce.ws;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Ce.AnalysisStatusWsResponse;

import static org.sonar.server.ce.ws.CeWsParameters.PARAM_BRANCH;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class AnalysisStatusAction implements CeWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public AnalysisStatusAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("analysis_status")
      .setDescription("Get the analysis status of a given component: a project, branch or pull request.<br>" +
        "Requires the following permission: 'Browse' on the specified component.")
      .setSince("7.4")
      .setResponseExample(getClass().getResource("analysis_status-example.json"))
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_COMPONENT);
    String branchKey = request.param(PARAM_BRANCH);
    String pullRequestKey = request.param(PARAM_PULL_REQUEST);

    checkRequest(branchKey == null || pullRequestKey == null,
      "Parameters '%s' and '%s' must not be specified at the same time", PARAM_BRANCH, PARAM_PULL_REQUEST);

    doHandle(request, response, projectKey, branchKey, pullRequestKey);
  }

  private void doHandle(Request request, Response response, String projectKey, @Nullable String branchKey, @Nullable String pullRequestKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      userSession.checkEntityPermission(UserRole.USER, project);
      BranchDto branch = componentFinder.getBranchOrPullRequest(dbSession, project, branchKey, pullRequestKey);

      AnalysisStatusWsResponse.Builder responseBuilder = AnalysisStatusWsResponse.newBuilder();
      CeActivityDto lastActivity = dbClient.ceActivityDao()
        .selectLastByComponentUuidAndTaskType(dbSession, branch.getUuid(), CeTaskTypes.REPORT).orElse(null);
      responseBuilder.setComponent(formatComponent(dbSession, project, lastActivity, branchKey, pullRequestKey));

      writeProtobuf(responseBuilder.build(), request, response);
    }
  }

  private AnalysisStatusWsResponse.Component formatComponent(DbSession dbSession, ProjectDto project, @Nullable CeActivityDto lastActivity,
    @Nullable String branchKey, @Nullable String pullRequestKey) {
    AnalysisStatusWsResponse.Component.Builder builder = AnalysisStatusWsResponse.Component.newBuilder()
      .setKey(project.getKey())
      .setName(project.getName());

    if (branchKey != null) {
      builder.setBranch(branchKey);
    } else if (pullRequestKey != null) {
      builder.setPullRequest(pullRequestKey);
    }

    if (lastActivity == null) {
      return builder.build();
    }

    List<CeTaskMessageDto> warnings;
    String userUuid = userSession.getUuid();
    if (userUuid != null) {
      warnings =  dbClient.ceTaskMessageDao().selectNonDismissedByUserAndTask(dbSession, lastActivity.getUuid(), userUuid);
    } else {
      warnings = lastActivity.getCeTaskMessageDtos();
    }

    List<AnalysisStatusWsResponse.Warning> result = warnings.stream().map(dto -> AnalysisStatusWsResponse.Warning.newBuilder()
      .setKey(dto.getUuid())
      .setMessage(dto.getMessage())
      .setDismissable(dto.getType().isDismissible())
      .build())
      .toList();
    builder.addAllWarnings(result);
    return builder.build();
  }

}
