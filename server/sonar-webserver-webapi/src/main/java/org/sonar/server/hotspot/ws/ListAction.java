/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueListQuery;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ProjectAndBranch;
import org.sonar.server.hotspot.ws.HotspotWsResponseFormatter.SearchResponseData;
import org.sonar.server.issue.NewCodePeriodResolver;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements HotspotsWsAction {
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_IN_NEW_CODE_PERIOD = "inNewCodePeriod";
  private static final List<String> STATUSES = List.of(STATUS_TO_REVIEW, STATUS_REVIEWED);
  private final DbClient dbClient;
  private final UserSession userSession;
  private final HotspotWsResponseFormatter responseFormatter;
  private final NewCodePeriodResolver newCodePeriodResolver;
  private final ComponentFinder componentFinder;

  public ListAction(DbClient dbClient, UserSession userSession, HotspotWsResponseFormatter responseFormatter,
    NewCodePeriodResolver newCodePeriodResolver, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.responseFormatter = responseFormatter;
    this.newCodePeriodResolver = newCodePeriodResolver;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("list")
      .setHandler(this)
      .setInternal(true)
      .setDescription("List Security Hotpots. This endpoint is used in degraded mode, when issue indexing is running." +
        "<br>Total number of Security Hotspots will be always equal to a page size, as counting all issues is not supported. " +
        "<br>Requires the 'Browse' permission on the specified project. ")
      .setSince("10.2");

    action.addPagingParams(100, MAX_PAGE_SIZE);
    action.createParam(PARAM_PROJECT)
      .setDescription("Key of the project")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
    action.createParam(PARAM_STATUS)
      .setDescription("If '%s' is provided, only Security Hotspots with the specified status are returned.", PARAM_PROJECT)
      .setPossibleValues(STATUSES)
      .setRequired(false);
    action.createParam(PARAM_RESOLUTION)
      .setDescription(format(
        "If '%s' is provided and if status is '%s', only Security Hotspots with the specified resolution are returned.",
        PARAM_PROJECT, STATUS_REVIEWED))
      .setPossibleValues(RESOLUTION_FIXED, RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED)
      .setRequired(false);
    action.createParam(PARAM_IN_NEW_CODE_PERIOD)
      .setDescription("If '%s' is provided, only Security Hotspots created in the new code period are returned.", PARAM_IN_NEW_CODE_PERIOD)
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.setResponseExample(getClass().getResource("search-example.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsRequest wsRequest = toWsRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectAndBranch projectAndBranch = validate(dbSession, wsRequest);
      SearchResponseData searchResponseData = searchHotspots(dbSession, wsRequest, projectAndBranch);
      loadComponents(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private static WsRequest toWsRequest(Request request) {
    return new WsRequest(
      request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE))
      .project(request.param(PARAM_PROJECT))
      .branch(request.param(PARAM_BRANCH))
      .pullRequest(request.param(PARAM_PULL_REQUEST))
      .status(request.param(PARAM_STATUS))
      .resolution(request.param(PARAM_RESOLUTION))
      .inNewCodePeriod(request.paramAsBoolean(PARAM_IN_NEW_CODE_PERIOD));
  }

  private ProjectAndBranch validate(DbSession dbSession, WsRequest wsRequest) {
    Preconditions.checkArgument(isNullOrEmpty(wsRequest.branch) || isNullOrEmpty(wsRequest.pullRequest),
      "Only one of parameters '%s' and '%s' can be provided", PARAM_BRANCH, PARAM_PULL_REQUEST);

    ProjectAndBranch projectAndBranch = componentFinder.getProjectAndBranch(dbSession, wsRequest.project,
      wsRequest.branch, wsRequest.pullRequest);

    userSession.checkEntityPermission(USER, projectAndBranch.getProject());
    return projectAndBranch;
  }

  private SearchResponseData searchHotspots(DbSession dbSession, WsRequest wsRequest, ProjectAndBranch projectAndBranch) {
    List<String> hotspotKeys = getHotspotKeys(dbSession, wsRequest, projectAndBranch);

    Paging paging = forPageIndex(wsRequest.page).withPageSize(wsRequest.pageSize).andTotal(hotspotKeys.size());
    List<IssueDto> hotspots = dbClient.issueDao().selectByKeys(dbSession, hotspotKeys);
    return new SearchResponseData(paging, hotspots);
  }

  private List<String> getHotspotKeys(DbSession dbSession, WsRequest wsRequest, ProjectAndBranch projectAndBranch) {
    BranchDto branch = projectAndBranch.getBranch();
    IssueListQuery.IssueListQueryBuilder queryBuilder = IssueListQuery.IssueListQueryBuilder.newIssueListQueryBuilder()
      .project(wsRequest.project)
      .branch(branch.getBranchKey())
      .pullRequest(branch.getPullRequestKey())
      .statuses(wsRequest.status != null ? singletonList(wsRequest.status) : emptyList())
      .resolutions(wsRequest.resolution != null ? singletonList(wsRequest.resolution) : emptyList())
      .types(singletonList(RuleType.SECURITY_HOTSPOT.getDbConstant()));

    String branchKey = branch.getBranchKey();
    if (wsRequest.inNewCodePeriod && wsRequest.pullRequest == null && branchKey != null) {
      NewCodePeriodResolver.ResolvedNewCodePeriod newCodePeriod = newCodePeriodResolver.resolveForProjectAndBranch(dbSession, wsRequest.project, branchKey);
      if (NewCodePeriodType.REFERENCE_BRANCH == newCodePeriod.type()) {
        queryBuilder.newCodeOnReference(true);
      } else {
        queryBuilder.createdAfter(newCodePeriod.periodDate());
      }
    }

    Pagination pagination = Pagination.forPage(wsRequest.page).andSize(wsRequest.pageSize);
    return dbClient.issueDao().selectIssueKeysByQuery(dbSession, queryBuilder.build(), pagination);
  }

  private void loadComponents(DbSession dbSession, SearchResponseData searchResponseData) {
    Set<String> componentUuids = searchResponseData.getHotspots().stream()
      .flatMap(hotspot -> Stream.of(hotspot.getComponentUuid(), hotspot.getProjectUuid()))
      .collect(Collectors.toSet());

    Set<String> locationComponentUuids = searchResponseData.getHotspots()
      .stream()
      .flatMap(hotspot -> getHotspotLocationComponentUuids(hotspot).stream())
      .collect(Collectors.toSet());

    Set<String> aggregatedComponentUuids = Stream.of(componentUuids, locationComponentUuids)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    if (!aggregatedComponentUuids.isEmpty()) {
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, aggregatedComponentUuids);
      searchResponseData.addComponents(componentDtos);
    }
  }

  private static Set<String> getHotspotLocationComponentUuids(IssueDto hotspot) {
    Set<String> locationComponentUuids = new HashSet<>();
    DbIssues.Locations locations = hotspot.parseLocations();

    if (locations == null) {
      return locationComponentUuids;
    }

    List<DbIssues.Flow> flows = locations.getFlowList();

    for (DbIssues.Flow flow : flows) {
      List<DbIssues.Location> flowLocations = flow.getLocationList();
      for (DbIssues.Location location : flowLocations) {
        if (location.hasComponentId()) {
          locationComponentUuids.add(location.getComponentId());
        }
      }
    }

    return locationComponentUuids;
  }

  private Hotspots.ListWsResponse formatResponse(SearchResponseData searchResponseData) {
    Hotspots.ListWsResponse.Builder responseBuilder = Hotspots.ListWsResponse.newBuilder();
    formatPaging(searchResponseData, responseBuilder);
    if (searchResponseData.isPresent()) {
      responseFormatter.formatHotspots(searchResponseData, responseBuilder);
    }
    return responseBuilder.build();
  }

  private static void formatPaging(SearchResponseData searchResponseData, Hotspots.ListWsResponse.Builder responseBuilder) {
    Paging paging = searchResponseData.getPaging();
    Common.Paging.Builder pagingBuilder = Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(searchResponseData.getHotspots().size());

    responseBuilder.setPaging(pagingBuilder.build());
  }

  private static final class WsRequest {
    private final int page;
    private final int pageSize;
    private String project;
    private String branch;
    private String pullRequest;
    private String status;
    private String resolution;
    private boolean inNewCodePeriod;

    private WsRequest(int page, int pageSize) {
      this.page = page;
      this.pageSize = pageSize;
    }

    public WsRequest project(@Nullable String project) {
      this.project = project;
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

    public WsRequest status(@Nullable String status) {
      this.status = status;
      return this;
    }

    public WsRequest resolution(@Nullable String resolution) {
      this.resolution = resolution;
      return this;
    }

    public WsRequest inNewCodePeriod(@Nullable Boolean inNewCodePeriod) {
      this.inNewCodePeriod = inNewCodePeriod != null && inNewCodePeriod;
      return this;
    }
  }
}
