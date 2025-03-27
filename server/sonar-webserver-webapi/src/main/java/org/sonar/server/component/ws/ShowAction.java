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
package org.sonar.server.component.ws;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.ShowWsResponse;

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
  private static final Set<String> PROJECT_OR_APP_QUALIFIERS = Set.of(ComponentQualifiers.PROJECT, ComponentQualifiers.APP);
  private static final Set<String> APP_VIEW_OR_SUBVIEW_QUALIFIERS = Set.of(ComponentQualifiers.APP, ComponentQualifiers.VIEW,
    ComponentQualifiers.SUBVIEW);
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;

  public ShowAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder,
    IssueIndexSyncProgressChecker issueIndexSyncProgressChecker) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
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
        new Change("10.1", String.format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("6.6");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
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
      userSession.checkComponentPermission(ProjectPermission.USER, component);
      Optional<SnapshotDto> lastAnalysis;
      if (component.getCopyComponentUuid() != null) {
        lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.getCopyComponentUuid());
      } else {
        lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.branchUuid());
      }
      List<ComponentDto> ancestors = dbClient.componentDao().selectAncestors(dbSession, component);
      return buildResponse(dbSession, component, ancestors, lastAnalysis.orElse(null), request);
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentKey = request.getComponentKey();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();

    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private ShowWsResponse buildResponse(DbSession dbSession, ComponentDto component, List<ComponentDto> orderedAncestors,
    @Nullable SnapshotDto lastAnalysis, Request request) {
    ShowWsResponse.Builder response = ShowWsResponse.newBuilder();
    response.setComponent(toWsComponent(dbSession, component, lastAnalysis, request));
    addAncestorsToResponse(dbSession, response, orderedAncestors, lastAnalysis, request);
    return response.build();
  }

  private void addAncestorsToResponse(DbSession dbSession, ShowWsResponse.Builder response, List<ComponentDto> orderedAncestors,
    @Nullable SnapshotDto lastAnalysis, Request request) {
    // ancestors are ordered from root to leaf, whereas it's the opposite in WS response
    int size = orderedAncestors.size() - 1;
    IntStream.rangeClosed(0, size).forEach(
      index -> response.addAncestors(toWsComponent(dbSession, orderedAncestors.get(size - index), lastAnalysis, request)));
  }

  private Components.Component.Builder toWsComponent(DbSession dbSession, ComponentDto component, @Nullable SnapshotDto lastAnalysis,
    Request request) {

    // project or application
    if (isMainBranchOfProjectOrApp(component, dbSession)) {
      ProjectDto project = dbClient.projectDao().selectProjectOrAppByKey(dbSession, component.getKey())
        .orElseThrow(() -> new IllegalStateException("Project is in invalid state."));
      boolean needIssueSync = needIssueSync(dbSession, component, project);
      return projectOrAppToWsComponent(project, lastAnalysis).setNeedIssueSync(needIssueSync);
    }

    // parent project can an application. For components in portfolios, it will be null
    ProjectDto parentProject = dbClient.projectDao().selectByBranchUuid(dbSession, component.branchUuid()).orElse(null);
    boolean needIssueSync = needIssueSync(dbSession, component, parentProject);

    // if this is a project calculated in a portfolio or app, we need to include the original branch name (if any)
    if (component.getCopyComponentUuid() != null) {
      String branch = dbClient.branchDao().selectByUuid(dbSession, component.getCopyComponentUuid())
        .filter(b -> !b.isMain())
        .map(BranchDto::getKey)
        .orElse(null);
      return componentDtoToWsComponent(component, parentProject, lastAnalysis, true, branch, null)
        .setNeedIssueSync(needIssueSync);
    }

    // branch won't exist for portfolios
    Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, component.branchUuid());
    if (branchDto.isPresent() && !branchDto.get().isMain()) {
      return componentDtoToWsComponent(component, parentProject, lastAnalysis, false, request.branch, request.pullRequest)
        .setNeedIssueSync(needIssueSync);
    } else {
      return componentDtoToWsComponent(component, parentProject, lastAnalysis, true, null, null)
        .setNeedIssueSync(needIssueSync);
    }
  }

  private boolean isMainBranchOfProjectOrApp(ComponentDto component, DbSession dbSession) {
    if (!PROJECT_OR_APP_QUALIFIERS.contains(component.qualifier()) || !ComponentScopes.PROJECT.equals(component.scope())) {
      return false;
    }
    Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, component.branchUuid());
    return branchDto.isPresent() && branchDto.get().isMain();
  }

  private boolean needIssueSync(DbSession dbSession, ComponentDto component, @Nullable ProjectDto projectDto) {
    if (projectDto == null || APP_VIEW_OR_SUBVIEW_QUALIFIERS.contains(component.qualifier())) {
      return issueIndexSyncProgressChecker.isIssueSyncInProgress(dbSession);
    }
    return issueIndexSyncProgressChecker.doProjectNeedIssueSync(dbSession, projectDto.getUuid());
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
