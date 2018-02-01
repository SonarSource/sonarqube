/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.BranchStatistics;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.ProjectBranches;
import org.sonarqube.ws.ProjectBranches.ListWsResponse;
import org.sonarqube.ws.ProjectBranches.PullRequest;
import org.sonarqube.ws.ProjectBranches.Status;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.server.branch.ws.BranchesWs.addProjectParam;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_LIST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements BranchWsAction {

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
    WebService.NewAction action = context.createAction(ACTION_LIST)
      .setSince("6.6")
      .setDescription("List the branches of a project.<br/>" +
        "Requires 'Administer' rights on the specified project.")
      .setResponseExample(Resources.getResource(getClass(), "list-example.json"))
      .setHandler(this);

    addProjectParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      userSession.checkComponentPermission(UserRole.USER, project);
      checkArgument(project.isEnabled() && PROJECT.equals(project.qualifier()), "Invalid project key");

      Collection<BranchDto> branches = dbClient.branchDao().selectByComponent(dbSession, project);
      Map<String, BranchDto> mergeBranchesByUuid = dbClient.branchDao()
        .selectByUuids(dbSession, branches.stream().map(BranchDto::getMergeBranchUuid).filter(Objects::nonNull).collect(toList()))
        .stream().collect(uniqueIndex(BranchDto::getUuid));
      Map<String, LiveMeasureDto> qualityGateMeasuresByComponentUuids = dbClient.liveMeasureDao()
        .selectByComponentUuidsAndMetricKeys(dbSession, branches.stream().map(BranchDto::getUuid).collect(toList()), singletonList(ALERT_STATUS_KEY))
        .stream().collect(uniqueIndex(LiveMeasureDto::getComponentUuid));
      Map<String, BranchStatistics> branchStatisticsByBranchUuid = issueIndex.searchBranchStatistics(project.uuid(), branches.stream()
        .filter(b -> b.getBranchType().equals(SHORT))
        .map(BranchDto::getUuid).collect(toList()))
        .stream().collect(uniqueIndex(BranchStatistics::getBranchUuid, Function.identity()));
      Map<String, String> analysisDateByBranchUuid = dbClient.snapshotDao()
        .selectLastAnalysesByRootComponentUuids(dbSession, branches.stream().map(BranchDto::getUuid).collect(Collectors.toList()))
        .stream().collect(uniqueIndex(SnapshotDto::getComponentUuid, s -> formatDateTime(s.getCreatedAt())));

      ListWsResponse.Builder protobufResponse = ListWsResponse.newBuilder();
      branches.forEach(b -> addBranch(protobufResponse, b, mergeBranchesByUuid, qualityGateMeasuresByComponentUuids.get(b.getUuid()), branchStatisticsByBranchUuid.get(b.getUuid()),
          analysisDateByBranchUuid.get(b.getUuid())));
      setStubPullRequests(protobufResponse);
      writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private static void setStubPullRequests(ListWsResponse.Builder protobufResponse) {
    PullRequest.Builder pullRequest = PullRequest.newBuilder();
    pullRequest
      .setAnalysisDate("2017-01-02T00:00:00.000Z")
      .setBase("master")
      .setBranch("feature/stas/pr-api")
      .setId("2734")
      .setName("SONAR-10374 Support pull request in the web app")
      .setStatus(Status.newBuilder().setBugs(1).setCodeSmells(3).setVulnerabilities(0))
      .setIsOrphan(false)
      .setUrl("https://github.com/SonarSource/sonarqube/pull/2734");
    protobufResponse.addPullRequests(pullRequest);
    pullRequest
      .setAnalysisDate("2017-01-01T00:00:00.000Z")
      .setBase("branch-6.7")
      .setBranch("feature/stas/my-bug-fix")
      .setId("2725")
      .setName("fix critical LTS issue")
      .setStatus(Status.newBuilder().setBugs(0).setCodeSmells(0).setVulnerabilities(0))
      .setIsOrphan(false)
      .setUrl("https://github.com/SonarSource/sonarqube/pull/2725");
    protobufResponse.addPullRequests(pullRequest);
    pullRequest
      .setAnalysisDate("2017-01-03T00:00:00.000Z")
      .setBase("unknown-branch")
      .setBranch("feature/stas/unknown-branch")
      .setId("9999")
      .setName("create orphan pull request")
      .setIsOrphan(true)
      .setStatus(Status.newBuilder().setBugs(0).setCodeSmells(0).setVulnerabilities(0));
    protobufResponse.addPullRequests(pullRequest);
  }

  private static void addBranch(ListWsResponse.Builder response, BranchDto branch, Map<String, BranchDto> mergeBranchesByUuid,
    @Nullable LiveMeasureDto qualityGateMeasure, BranchStatistics branchStatistics, @Nullable String analysisDate) {
    ProjectBranches.Branch.Builder builder = toBranchBuilder(branch, Optional.ofNullable(mergeBranchesByUuid.get(branch.getMergeBranchUuid())));
    setBranchStatus(builder, branch, qualityGateMeasure, branchStatistics);
    if (analysisDate != null) {
      builder.setAnalysisDate(analysisDate);
    }
    response.addBranches(builder);
  }

  private static ProjectBranches.Branch.Builder toBranchBuilder(BranchDto branch, Optional<BranchDto> mergeBranch) {
    ProjectBranches.Branch.Builder builder = ProjectBranches.Branch.newBuilder();
    String branchKey = branch.getKey();
    setNullable(branchKey, builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(Common.BranchType.valueOf(branch.getBranchType().name()));
    if (branch.getBranchType().equals(SHORT)) {
      if (mergeBranch.isPresent()) {
        String mergeBranchKey = mergeBranch.get().getKey();
        builder.setMergeBranch(mergeBranchKey);
      } else {
        builder.setIsOrphan(true);
      }
    }
    return builder;
  }

  private static void setBranchStatus(ProjectBranches.Branch.Builder builder, BranchDto branch, @Nullable LiveMeasureDto qualityGateMeasure,
    @Nullable BranchStatistics branchStatistics) {
    Status.Builder statusBuilder = Status.newBuilder();
    if (branch.getBranchType() == LONG && qualityGateMeasure != null) {
      Protobuf.setNullable(qualityGateMeasure.getDataAsString(), statusBuilder::setQualityGateStatus);
    }
    if (branch.getBranchType() == BranchType.SHORT) {
      statusBuilder.setBugs(branchStatistics == null ? 0L : branchStatistics.getBugs());
      statusBuilder.setVulnerabilities(branchStatistics == null ? 0L : branchStatistics.getVulnerabilities());
      statusBuilder.setCodeSmells(branchStatistics == null ? 0L : branchStatistics.getCodeSmells());
    }
    builder.setStatus(statusBuilder);
  }
}
