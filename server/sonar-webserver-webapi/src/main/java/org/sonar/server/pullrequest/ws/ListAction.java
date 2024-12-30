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
package org.sonar.server.pullrequest.ws;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.pullrequest.ws.PullRequestsWs.addProjectParam;
import static org.sonar.server.pullrequest.ws.PullRequestsWsParameters.PARAM_PROJECT;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.PrStatistics;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectPullRequests;

public class ListAction implements PullRequestWsAction {

    private final DbClient dbClient;
    private final UserSession userSession;
    private final ComponentFinder componentFinder;
    private final IssueIndex issueIndex;

    public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder,
            IssueIndex issueIndex) {
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
                .setHandler(this)
                .setChangelog(
                        new Change("8.4", "Response fields: 'bugs', 'vulnerabilities', 'codeSmells' are deprecated."));
        addProjectParam(action);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        String projectKey = request.mandatoryParam(PARAM_PROJECT);

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
            checkPermission(project);

            List<BranchDto> pullRequests = dbClient.branchDao().selectByProject(dbSession, project).stream()
                    .filter(b -> b.getBranchType() == PULL_REQUEST)
                    .toList();
            List<String> pullRequestUuids = pullRequests.stream().map(BranchDto::getUuid).toList();

            Map<String, BranchDto> mergeBranchesByUuid = dbClient.branchDao()
                    .selectByUuids(dbSession,
                            pullRequests.stream().map(BranchDto::getMergeBranchUuid).filter(Objects::nonNull)
                                    .toList())
                    .stream().collect(Collectors.toMap(BranchDto::getUuid, Function.identity()));

            Map<String, PrStatistics> branchStatisticsByBranchUuid = issueIndex.searchBranchStatistics(
                            project.getUuid(), pullRequestUuids).stream()
                    .collect(Collectors.toMap(PrStatistics::getBranchUuid, Function.identity()));
            Map<String, MeasureDto> qualityGateMeasuresByComponentUuids = dbClient.measureDao()
                    .selectByComponentUuidsAndMetricKeys(dbSession, pullRequestUuids, singletonList(ALERT_STATUS_KEY))
                    .stream()
                    .collect(Collectors.toMap(MeasureDto::getComponentUuid, Function.identity()));
            Map<String, String> analysisDateByBranchUuid = dbClient.snapshotDao()
                    .selectLastAnalysesByRootComponentUuids(dbSession, pullRequestUuids).stream()
                    .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, s -> formatDateTime(s.getCreatedAt())));

            ProjectPullRequests.ListWsResponse.Builder protobufResponse = ProjectPullRequests.ListWsResponse.newBuilder();
            pullRequests
                    .forEach(b -> addPullRequest(protobufResponse, b, mergeBranchesByUuid,
                            qualityGateMeasuresByComponentUuids.get(b.getUuid()),
                            branchStatisticsByBranchUuid.get(b.getUuid()),
                            analysisDateByBranchUuid.get(b.getUuid())));
            writeProtobuf(protobufResponse.build(), request, response);
        }
    }

    private void checkPermission(ProjectDto project) {
        if (userSession.hasEntityPermission(USER, project) ||
                userSession.hasEntityPermission(UserRole.SCAN, project) ||
                userSession.hasPermission(OrganizationPermission.SCAN, project.getOrganizationUuid())) {
            return;
        }
        throw insufficientPrivilegesException();
    }

    private static void addPullRequest(ProjectPullRequests.ListWsResponse.Builder response, BranchDto branch,
            Map<String, BranchDto> mergeBranchesByUuid,
            @Nullable MeasureDto qualityGateMeasure, PrStatistics prStatistics, @Nullable String analysisDate) {
        Optional<BranchDto> mergeBranch = Optional.ofNullable(mergeBranchesByUuid.get(branch.getMergeBranchUuid()));

        ProjectPullRequests.PullRequest.Builder builder = ProjectPullRequests.PullRequest.newBuilder();
        builder.setKey(branch.getKey());

        // The additional PR information might be optionally specified inside data attribute.
        DbProjectBranches.PullRequestData pullRequestData = branch.getPullRequestData();
        if (pullRequestData != null) {
            builder.setBranch(pullRequestData.getBranch());
            builder.setTarget(pullRequestData.getTarget());
            ofNullable(emptyToNull(pullRequestData.getUrl())).ifPresent(builder::setUrl);
            ofNullable(emptyToNull(pullRequestData.getTitle())).ifPresent(builder::setTitle);
        } else {
            builder.setBranch(branch.getKey());
            builder.setTitle(branch.getKey());
        }

        if (mergeBranch.isPresent()) {
            String mergeBranchKey = mergeBranch.get().getKey();
            builder.setBase(mergeBranchKey);
            builder.setTarget(mergeBranchKey);
        } else {
            builder.setIsOrphan(true);
        }

        ofNullable(analysisDate).ifPresent(builder::setAnalysisDate);
        setQualityGate(builder, qualityGateMeasure, prStatistics);
        response.addPullRequests(builder);
    }

    private static void setQualityGate(ProjectPullRequests.PullRequest.Builder builder,
            @Nullable MeasureDto qualityGateMeasure, @Nullable PrStatistics prStatistics) {
        ProjectPullRequests.Status.Builder statusBuilder = ProjectPullRequests.Status.newBuilder();
        if (qualityGateMeasure != null) {
            ofNullable(qualityGateMeasure.getString(ALERT_STATUS_KEY)).ifPresent(statusBuilder::setQualityGateStatus);
        }
        builder.setStatus(statusBuilder);
    }
}
