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
package org.sonar.server.issue.ws;

import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueListQuery;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ProjectAndBranch;
import org.sonar.server.issue.NewCodePeriodResolver;
import org.sonar.server.issue.NewCodePeriodResolver.ResolvedNewCodePeriod;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.issue.index.IssueQueryFactory.ISSUE_STATUSES;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_LIST;

public class ListAction implements IssuesWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_COMPONENT = "component";
  private static final String PARAM_TYPES = "types";
  private static final String PARAM_RESOLVED = "resolved";
  private static final String PARAM_IN_NEW_CODE_PERIOD = "inNewCodePeriod";
  private final UserSession userSession;
  private final DbClient dbClient;
  private final NewCodePeriodResolver newCodePeriodResolver;
  private final SearchResponseLoader searchResponseLoader;
  private final SearchResponseFormat searchResponseFormat;
  private final ComponentFinder componentFinder;

  public ListAction(UserSession userSession, DbClient dbClient, NewCodePeriodResolver newCodePeriodResolver, SearchResponseLoader searchResponseLoader,
    SearchResponseFormat searchResponseFormat, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.newCodePeriodResolver = newCodePeriodResolver;
    this.searchResponseLoader = searchResponseLoader;
    this.searchResponseFormat = searchResponseFormat;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(ACTION_LIST)
      .setHandler(this)
      .setInternal(true)
      .setDescription("List issues. This endpoint is used in degraded mode, when issue indexing is running." +
                      "<br>Either 'project' or 'component' parameter is required." +
                      "<br>Total number of issues will be always equal to a page size, as this counting all issues is not supported. " +
                      "<br>Requires the 'Browse' permission on the specified project. ")
      .setSince("10.2")
      .setChangelog(
        new Change("10.4", format("Parameter '%s' is deprecated.", PARAM_TYPES)),
        new Change("10.4", "The response fields 'severity' and 'type' are deprecated. Please use 'impacts' instead."),
        new Change("10.4", "The response fields 'status' and 'resolution' are deprecated. Please use 'issueStatus' instead."),
        new Change("10.4", "Add 'issueStatus' field to the response.")
      )
      .setResponseExample(getClass().getResource("list-example.json"));

    action.addPagingParams(100, MAX_PAGE_SIZE);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue("my-project-key");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue("feature/my-new-feature");

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Filter issues that belong to the specified pull request. Not available in the community edition.")
      .setExampleValue("42");

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue("my_project:my_file.js");

    action.createParam(PARAM_TYPES)
      .setDescription("Comma-separated list of issue types")
      .setExampleValue("BUG, VULNERABILITY")
      .setDeprecatedSince("10.4")
      .setPossibleValues(RuleType.BUG.name(), RuleType.VULNERABILITY.name(), RuleType.CODE_SMELL.name());

    action.createParam(PARAM_IN_NEW_CODE_PERIOD)
      .setDescription("Filter issues created in the new code period of the project")
      .setExampleValue("true")
      .setDefaultValue(false)
      .setBooleanPossibleValues();

    action.createParam(PARAM_RESOLVED)
      .setDescription("Filter issues that are resolved or not, if not provided all issues will be returned")
      .setExampleValue("true")
      .setBooleanPossibleValues();

  }

  @Override
  public final void handle(Request request, Response response) {
    WsRequest wsRequest = toWsRequest(request);
    ProjectAndBranch projectAndBranch = validateRequest(wsRequest);
    List<String> issueKeys = getIssueKeys(wsRequest, projectAndBranch);
    Issues.ListWsResponse wsResponse = formatResponse(wsRequest, issueKeys);
    writeProtobuf(wsResponse, request, response);
  }

  private static WsRequest toWsRequest(Request request) {
    WsRequest wsRequest = new WsRequest();
    wsRequest.project(request.param(PARAM_PROJECT));
    wsRequest.component(request.param(PARAM_COMPONENT));
    wsRequest.branch(request.param(PARAM_BRANCH));
    wsRequest.pullRequest(request.param(PARAM_PULL_REQUEST));
    List<String> types = request.paramAsStrings(PARAM_TYPES);
    wsRequest.types(types == null ? List.of(RuleType.BUG.getDbConstant(), RuleType.VULNERABILITY.getDbConstant(), RuleType.CODE_SMELL.getDbConstant())
      : types.stream().map(RuleType::valueOf).map(RuleType::getDbConstant).toList());
    wsRequest.newCodePeriod(request.mandatoryParamAsBoolean(PARAM_IN_NEW_CODE_PERIOD));
    wsRequest.resolved(request.paramAsBoolean(PARAM_RESOLVED));
    wsRequest.page(request.mandatoryParamAsInt(PAGE));
    wsRequest.pageSize(request.mandatoryParamAsInt(PAGE_SIZE));
    return wsRequest;
  }

  private ProjectAndBranch validateRequest(WsRequest wsRequest) {
    checkArgument(!isNullOrEmpty(wsRequest.project) || !isNullOrEmpty(wsRequest.component),
      "Either '%s' or '%s' parameter must be provided", PARAM_PROJECT, PARAM_COMPONENT);
    Preconditions.checkArgument(isNullOrEmpty(wsRequest.branch) || isNullOrEmpty(wsRequest.pullRequest),
      "Only one of parameters '%s' and '%s' can be provided", PARAM_BRANCH, PARAM_PULL_REQUEST);

    ProjectAndBranch projectAndBranch;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!isNullOrEmpty(wsRequest.component)) {
        projectAndBranch = checkComponentPermission(wsRequest, dbSession);
      } else {
        projectAndBranch = checkProjectAndBranchPermission(wsRequest, dbSession);
      }
    }
    return projectAndBranch;
  }

  private ProjectAndBranch checkComponentPermission(WsRequest wsRequest, DbSession dbSession) {
    ComponentDto componentDto = componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, wsRequest.component, wsRequest.branch, wsRequest.pullRequest);
    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbSession, componentDto.branchUuid())
      .orElseThrow(() -> new IllegalStateException("Branch does not exist: " + componentDto.branchUuid()));
    ProjectDto projectDto = dbClient.projectDao().selectByUuid(dbSession, branchDto.getProjectUuid())
      .orElseThrow(() -> new IllegalArgumentException("Project does not exist: " + wsRequest.project));
    userSession.checkEntityPermission(UserRole.USER, projectDto);
    return new ProjectAndBranch(projectDto, branchDto);
  }

  private ProjectAndBranch checkProjectAndBranchPermission(WsRequest wsRequest, DbSession dbSession) {
    ProjectAndBranch projectAndBranch = componentFinder.getProjectAndBranch(dbSession, wsRequest.project, wsRequest.branch, wsRequest.pullRequest);
    userSession.checkEntityPermission(UserRole.USER, projectAndBranch.getProject());
    return projectAndBranch;
  }

  private List<String> getIssueKeys(WsRequest wsRequest, ProjectAndBranch projectAndBranch) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BranchDto branch = projectAndBranch.getBranch();
      IssueListQuery.IssueListQueryBuilder queryBuilder = IssueListQuery.IssueListQueryBuilder.newIssueListQueryBuilder()
        .project(wsRequest.project)
        .component(wsRequest.component)
        .branch(branch.getBranchKey())
        .pullRequest(branch.getPullRequestKey())
        .resolved(wsRequest.resolved)
        .statuses(ISSUE_STATUSES)
        .types(wsRequest.types);

      String branchKey = branch.getBranchKey();
      if (wsRequest.inNewCodePeriod && wsRequest.pullRequest == null && branchKey != null) {
        ResolvedNewCodePeriod newCodePeriod = newCodePeriodResolver.resolveForProjectAndBranch(dbSession, wsRequest.project, branchKey);
        if (NewCodePeriodType.REFERENCE_BRANCH == newCodePeriod.type()) {
          queryBuilder.newCodeOnReference(true);
        } else {
          queryBuilder.createdAfter(newCodePeriod.periodDate());
        }
      }

      Pagination pagination = Pagination.forPage(wsRequest.page).andSize(wsRequest.pageSize);
      return dbClient.issueDao().selectIssueKeysByQuery(dbSession, queryBuilder.build(), pagination);
    }
  }

  private Issues.ListWsResponse formatResponse(WsRequest request, List<String> issueKeys) {
    Issues.ListWsResponse.Builder response = Issues.ListWsResponse.newBuilder();
    response.setPaging(Common.Paging.newBuilder()
      .setPageIndex(request.page)
      .setPageSize(issueKeys.size())
      .build());

    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(issueKeys);
    collectLoggedInUser(collector);
    SearchResponseData preloadedData = new SearchResponseData(emptyList());
    EnumSet<SearchAdditionalField> additionalFields = EnumSet.of(SearchAdditionalField.ACTIONS, SearchAdditionalField.COMMENTS, SearchAdditionalField.TRANSITIONS);
    SearchResponseData data = searchResponseLoader.load(preloadedData, collector, additionalFields, null);

    Paging paging = forPageIndex(request.page)
      .withPageSize(request.pageSize)
      .andTotal(request.pageSize);
    return searchResponseFormat.formatList(additionalFields, data, paging);
  }

  private void collectLoggedInUser(SearchResponseLoader.Collector collector) {
    if (userSession.isLoggedIn()) {
      collector.addUserUuids(singletonList(userSession.getUuid()));
    }
  }

  private static class WsRequest {
    private String project = null;
    private String component = null;
    private String branch = null;
    private String pullRequest = null;
    private List<Integer> types = null;
    private boolean inNewCodePeriod = false;
    private Boolean resolved = null;
    private int page = 1;
    private int pageSize = 100;

    public WsRequest project(@Nullable String project) {
      this.project = project;
      return this;
    }

    public WsRequest component(@Nullable String component) {
      this.component = component;
      return this;
    }

    public WsRequest branch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public WsRequest pullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    public WsRequest types(@Nullable List<Integer> types) {
      this.types = types;
      return this;
    }

    public WsRequest newCodePeriod(boolean newCodePeriod) {
      inNewCodePeriod = newCodePeriod;
      return this;
    }

    public WsRequest resolved(@Nullable Boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    public WsRequest page(int page) {
      this.page = page;
      return this;
    }

    public WsRequest pageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }
  }

}
