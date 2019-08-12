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
package org.sonar.server.ce.ws;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Ce.AnalysisStatusWsResponse;

import static org.sonar.server.ce.ws.CeWsParameters.PARAM_BRANCH;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
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
    String componentKey = request.mandatoryParam(PARAM_COMPONENT);
    String branchKey = request.param(PARAM_BRANCH);
    String pullRequestKey = request.param(PARAM_PULL_REQUEST);

    checkRequest(branchKey == null || pullRequestKey == null,
      "Parameters '%s' and '%s' must not be specified at the same time", PARAM_BRANCH, PARAM_PULL_REQUEST);

    doHandle(request, response, componentKey, branchKey, pullRequestKey);
  }

  private void doHandle(Request request, Response response, String componentKey, @Nullable String branchKey, @Nullable String pullRequestKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, componentKey, branchKey, pullRequestKey);
      userSession.checkComponentPermission(UserRole.USER, component);

      checkRequest(isProject(component), "Component '%s' must be a project.", componentKey);

      AnalysisStatusWsResponse.Builder responseBuilder = AnalysisStatusWsResponse.newBuilder();
      CeActivityDto lastActivity = dbClient.ceActivityDao()
        .selectLastByComponentUuidAndTaskType(dbSession, component.uuid(), CeTaskTypes.REPORT).orElse(null);
      responseBuilder.setComponent(formatComponent(dbSession, component, lastActivity, branchKey, pullRequestKey));

      writeProtobuf(responseBuilder.build(), request, response);
    }
  }

  private static boolean isProject(ComponentDto project) {
    return Scopes.PROJECT.equals(project.scope()) && Qualifiers.PROJECT.equals(project.qualifier());
  }

  private ComponentDto loadComponent(DbSession dbSession, String componentKey, @Nullable String branchKey, @Nullable String pullRequestKey) {
    if (branchKey != null) {
      return componentFinder.getByKeyAndBranch(dbSession, componentKey, branchKey);
    }
    if (pullRequestKey != null) {
      return componentFinder.getByKeyAndPullRequest(dbSession, componentKey, pullRequestKey);
    }
    return componentFinder.getByKey(dbSession, componentKey);
  }

  private AnalysisStatusWsResponse.Component formatComponent(DbSession dbSession, ComponentDto component, @Nullable CeActivityDto lastActivity,
    @Nullable String branchKey, @Nullable String pullRequestKey) {

    AnalysisStatusWsResponse.Component.Builder builder = AnalysisStatusWsResponse.Component.newBuilder()
      .setOrganization(getOrganizationKey(dbSession, component))
      .setKey(component.getKey())
      .setName(component.name());

    if (branchKey != null) {
      builder.setBranch(branchKey);
    } else if (pullRequestKey != null) {
      builder.setPullRequest(pullRequestKey);
    }

    if (lastActivity != null) {
      List<String> warnings = dbClient.ceTaskMessageDao().selectByTask(dbSession, lastActivity.getUuid()).stream()
        .map(CeTaskMessageDto::getMessage)
        .collect(Collectors.toList());

      builder.addAllWarnings(warnings);
    }

    return builder.build();
  }

  private String getOrganizationKey(DbSession dbSession, ComponentDto component) {
    String organizationUuid = component.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
      .orElseThrow(() -> new IllegalStateException("Unknown organization: " + organizationUuid))
      .getKey();
  }

}
