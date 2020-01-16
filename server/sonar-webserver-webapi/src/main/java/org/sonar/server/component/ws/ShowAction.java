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
package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.ShowWsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.projectOrAppToWsComponent;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;

public class ShowAction implements ComponentsWsAction {
  private static final Set<String> PROJECT_OR_APP_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.APP);
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public ShowAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SHOW)
      .setDescription("Returns a component (file, directory, project, portfolio…) and its ancestors. " +
        "The ancestors are ordered from the parent to the root project. " +
        "Requires the following permission: 'Browse' on the project of the specified component.")
      .setResponseExample(getClass().getResource("show-example.json"))
      .setSince("5.4")
      .setChangelog(
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setInternal(true)
      .setSince("7.1");
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request request, Response response) throws Exception {
    Request showRequest = toShowWsRequest(request);
    ShowWsResponse showWsResponse = doHandle(showRequest);

    writeProtobuf(showWsResponse, request, response);
  }

  private ShowWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, request);
      userSession.checkComponentPermission(UserRole.USER, component);
      Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.projectUuid());
      List<ComponentDto> ancestors = dbClient.componentDao().selectAncestors(dbSession, component);
      OrganizationDto organizationDto = componentFinder.getOrganization(dbSession, component);
      return buildResponse(dbClient, dbSession, component, organizationDto, ancestors, lastAnalysis.orElse(null));
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentKey = request.getComponentKey();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();

    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private static ShowWsResponse buildResponse(DbClient dbClient, DbSession dbSession, ComponentDto component,
    OrganizationDto organizationDto, List<ComponentDto> orderedAncestors,
    @Nullable SnapshotDto lastAnalysis) {
    ShowWsResponse.Builder response = ShowWsResponse.newBuilder();
    response.setComponent(toWsComponent(dbClient, dbSession, component, organizationDto, lastAnalysis));
    addAncestorsToResponse(dbClient, dbSession, response, orderedAncestors, organizationDto, lastAnalysis);
    return response.build();
  }

  private static void addAncestorsToResponse(DbClient dbClient, DbSession dbSession, ShowWsResponse.Builder response, List<ComponentDto> orderedAncestors,
    OrganizationDto organizationDto,
    @Nullable SnapshotDto lastAnalysis) {
    // ancestors are ordered from root to leaf, whereas it's the opposite in WS response
    int size = orderedAncestors.size() - 1;
    IntStream.rangeClosed(0, size).forEach(
      index -> response.addAncestors(toWsComponent(dbClient, dbSession, orderedAncestors.get(size - index), organizationDto, lastAnalysis)));
  }

  private static Components.Component.Builder toWsComponent(DbClient dbClient, DbSession dbSession, ComponentDto component, OrganizationDto organizationDto,
    @Nullable SnapshotDto lastAnalysis) {
    if (isProjectOrApp(component)) {
      ProjectDto project = dbClient.projectDao().selectProjectOrAppByKey(dbSession, component.getKey())
        .orElseThrow(() -> new IllegalStateException("Project is in invalid state."));
      return projectOrAppToWsComponent(project, organizationDto, lastAnalysis);
    } else {
      Optional<ProjectDto> parentProject = dbClient.projectDao().selectByUuid(dbSession,
        ofNullable(component.getMainBranchProjectUuid()).orElse(component.projectUuid()));
      return componentDtoToWsComponent(component, parentProject.orElse(null), organizationDto, lastAnalysis);
    }
  }

  private static boolean isProjectOrApp(ComponentDto component) {
    return component.getMainBranchProjectUuid() == null && PROJECT_OR_APP_QUALIFIERS.contains(component.qualifier());
  }

  private static Request toShowWsRequest(org.sonar.api.server.ws.Request request) {
    return new Request()
      .setComponentKey(request.mandatoryParam(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST));
  }

  private static class Request {
    private String componentKey;
    private String branch;
    private String pullRequest;

    public String getComponentKey() {
      return componentKey;
    }

    public Request setComponentKey(String componentKey) {
      this.componentKey = componentKey;
      return this;
    }

    @CheckForNull
    public String getBranch() {
      return branch;
    }

    public Request setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }

    public Request setPullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }
  }
}
