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
package org.sonar.server.branch.pr.ws;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.BranchStatistics;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectPullRequests;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.branch.pr.ws.PullRequestsWs.addProjectParam;
import static org.sonar.server.branch.pr.ws.PullRequestsWsParameters.PARAM_PROJECT;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements PullRequestWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final IssueIndex issueIndex;

  public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, IssueIndex issueIndex) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.issueIndex = issueIndex;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list")
      .setSince("7.1")
      .setDescription("List the pull requests of a project.<br/>" +
        "One of the following permissions is required: " +
        "<ul>" +
        "<li>'Browse' rights on the specified project</li>" +
        "<li>'Execute Analysis' rights on the specified project</li>" +
        "</ul>")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);

    addProjectParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      checkPermission(project);
      checkArgument(project.isEnabled() && PROJECT.equals(project.qualifier()), "Invalid project key");

      List<BranchDto> pullRequests = dbClient.branchDao().selectByComponent(dbSession, project).stream()
        .filter(b -> b.getBranchType() == PULL_REQUEST)
        .collect(toList());
      List<String> pullRequestUuids = pullRequests.stream().map(BranchDto::getUuid).collect(toList());

      Map<String, BranchDto> mergeBranchesByUuid = dbClient.branchDao()
        .selectByUuids(dbSession, pullRequests.stream().map(BranchDto::getMergeBranchUuid).filter(Objects::nonNull).collect(toList()))
        .stream().collect(uniqueIndex(BranchDto::getUuid));
      Map<String, BranchStatistics> branchStatisticsByBranchUuid = issueIndex.searchBranchStatistics(project.uuid(), pullRequestUuids).stream()
        .collect(uniqueIndex(BranchStatistics::getBranchUuid, Function.identity()));
      Map<String, LiveMeasureDto> qualityGateMeasuresByComponentUuids = dbClient.liveMeasureDao()
        .selectByComponentUuidsAndMetricKeys(dbSession, pullRequestUuids, singletonList(ALERT_STATUS_KEY)).stream()
        .collect(uniqueIndex(LiveMeasureDto::getComponentUuid));
      Map<String, String> analysisDateByBranchUuid = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, pullRequestUuids).stream()
        .collect(uniqueIndex(SnapshotDto::getComponentUuid, s -> formatDateTime(s.getCreatedAt())));

      ProjectPullRequests.ListWsResponse.Builder protobufResponse = ProjectPullRequests.ListWsResponse.newBuilder();
      pullRequests
        .forEach(b -> addPullRequest(protobufResponse, b, mergeBranchesByUuid, qualityGateMeasuresByComponentUuids.get(b.getUuid()), branchStatisticsByBranchUuid.get(b.getUuid()),
          analysisDateByBranchUuid.get(b.getUuid())));
      writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private void checkPermission(ComponentDto component) {
    if (userSession.hasComponentPermission(USER, component) ||
      userSession.hasComponentPermission(UserRole.SCAN, component) ||
      userSession.hasPermission(OrganizationPermission.SCAN, component.getOrganizationUuid())) {
      return;
    }
    throw insufficientPrivilegesException();
  }

  private static void addPullRequest(ProjectPullRequests.ListWsResponse.Builder response, BranchDto branch, Map<String, BranchDto> mergeBranchesByUuid,
    @Nullable LiveMeasureDto qualityGateMeasure, BranchStatistics branchStatistics, @Nullable String analysisDate) {
    Optional<BranchDto> mergeBranch = Optional.ofNullable(mergeBranchesByUuid.get(branch.getMergeBranchUuid()));

    ProjectPullRequests.PullRequest.Builder builder = ProjectPullRequests.PullRequest.newBuilder();
    builder.setKey(branch.getKey());

    DbProjectBranches.PullRequestData pullRequestData = requireNonNull(branch.getPullRequestData(), "Pull request data should be available for branch type PULL_REQUEST");
    builder.setBranch(pullRequestData.getBranch());
    ofNullable(emptyToNull(pullRequestData.getUrl())).ifPresent(builder::setUrl);
    ofNullable(emptyToNull(pullRequestData.getTitle())).ifPresent(builder::setTitle);

    if (mergeBranch.isPresent()) {
      String mergeBranchKey = mergeBranch.get().getKey();
      builder.setBase(mergeBranchKey);
    } else {
      builder.setIsOrphan(true);
    }
    ofNullable(analysisDate).ifPresent(builder::setAnalysisDate);
    setQualityGate(builder, qualityGateMeasure, branchStatistics);
    response.addPullRequests(builder);
  }

  private static void setQualityGate(ProjectPullRequests.PullRequest.Builder builder, @Nullable LiveMeasureDto qualityGateMeasure, @Nullable BranchStatistics branchStatistics) {
    ProjectPullRequests.Status.Builder statusBuilder = ProjectPullRequests.Status.newBuilder();
    if (qualityGateMeasure != null) {
      ofNullable(qualityGateMeasure.getDataAsString()).ifPresent(statusBuilder::setQualityGateStatus);
    }
    statusBuilder.setBugs(branchStatistics == null ? 0L : branchStatistics.getBugs());
    statusBuilder.setVulnerabilities(branchStatistics == null ? 0L : branchStatistics.getVulnerabilities());
    statusBuilder.setCodeSmells(branchStatistics == null ? 0L : branchStatistics.getCodeSmells());
    builder.setStatus(statusBuilder);
  }
}
