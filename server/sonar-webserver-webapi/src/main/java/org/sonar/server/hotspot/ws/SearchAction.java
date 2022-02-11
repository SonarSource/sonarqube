/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsUtils.nullToEmpty;

public class SearchAction implements HotspotsWsAction {
  private static final Set<String> SUPPORTED_QUALIFIERS = Set.of(Qualifiers.PROJECT, Qualifiers.APP);
  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_HOTSPOTS = "hotspots";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_SINCE_LEAK_PERIOD = "sinceLeakPeriod";
  private static final String PARAM_ONLY_MINE = "onlyMine";
  private static final String PARAM_OWASP_TOP_10 = "owaspTop10";
  private static final String PARAM_SANS_TOP_25 = "sansTop25";
  private static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  private static final String PARAM_CWE = "cwe";
  private static final String PARAM_FILES = "files";

  private static final List<String> STATUSES = List.of(STATUS_TO_REVIEW, STATUS_REVIEWED);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final HotspotWsResponseFormatter responseFormatter;
  private final TextRangeResponseFormatter textRangeFormatter;
  private final System2 system2;

  public SearchAction(DbClient dbClient, UserSession userSession, IssueIndex issueIndex,
    IssueIndexSyncProgressChecker issueIndexSyncProgressChecker,
    HotspotWsResponseFormatter responseFormatter, TextRangeResponseFormatter textRangeFormatter, System2 system2) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
    this.responseFormatter = responseFormatter;
    this.textRangeFormatter = textRangeFormatter;
    this.system2 = system2;
  }

  private static Set<String> setFromList(@Nullable List<String> list) {
    return list != null ? Set.copyOf(list) : Set.of();
  }

  private static WsRequest toWsRequest(Request request) {
    Set<String> hotspotKeys = setFromList(request.paramAsStrings(PARAM_HOTSPOTS));
    Set<String> owaspTop10 = setFromList(request.paramAsStrings(PARAM_OWASP_TOP_10));
    Set<String> sansTop25 = setFromList(request.paramAsStrings(PARAM_SANS_TOP_25));
    Set<String> sonarsourceSecurity = setFromList(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY));
    Set<String> cwes = setFromList(request.paramAsStrings(PARAM_CWE));
    Set<String> files = setFromList(request.paramAsStrings(PARAM_FILES));

    return new WsRequest(
      request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE), request.param(PARAM_PROJECT_KEY), request.param(PARAM_BRANCH),
      request.param(PARAM_PULL_REQUEST), hotspotKeys, request.param(PARAM_STATUS), request.param(PARAM_RESOLUTION),
      request.paramAsBoolean(PARAM_SINCE_LEAK_PERIOD), request.paramAsBoolean(PARAM_ONLY_MINE), owaspTop10, sansTop25, sonarsourceSecurity, cwes,
      files);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsRequest wsRequest = toWsRequest(request);
    validateParameters(wsRequest);
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkIfNeedIssueSync(dbSession, wsRequest);
      Optional<ComponentDto> project = getAndValidateProjectOrApplication(dbSession, wsRequest);
      SearchResponseData searchResponseData = searchHotspots(wsRequest, dbSession, project.orElse(null));
      loadComponents(dbSession, searchResponseData);
      loadRules(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private void checkIfNeedIssueSync(DbSession dbSession, WsRequest wsRequest) {
    Optional<String> projectKey = wsRequest.getProjectKey();
    if (projectKey.isPresent()) {
      issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(dbSession, projectKey.get());
    } else {
      // component keys not provided - asking for global
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(dbSession);
    }
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setHandler(this)
      .setDescription("Search for Security Hotpots. <br>"
        + "Requires the 'Browse' permission on the specified project(s). <br>"
        + "For applications, it also requires 'Browse' permission on its child projects. <br>"
        + "When issue indexation is in progress returns 503 service unavailable HTTP code.")
      .setSince("8.1")
      .setInternal(true);

    action.addPagingParams(100);
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription(format(
        "Key of the project or application. This parameter is required unless %s is provided.",
        PARAM_HOTSPOTS))
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
    action.createParam(PARAM_HOTSPOTS)
      .setDescription(format(
        "Comma-separated list of Security Hotspot keys. This parameter is required unless %s is provided.",
        PARAM_PROJECT_KEY))
      .setExampleValue("AWhXpLoInp4On-Y3xc8x");
    action.createParam(PARAM_STATUS)
      .setDescription("If '%s' is provided, only Security Hotspots with the specified status are returned.", PARAM_PROJECT_KEY)
      .setPossibleValues(STATUSES)
      .setRequired(false);
    action.createParam(PARAM_RESOLUTION)
      .setDescription(format(
        "If '%s' is provided and if status is '%s', only Security Hotspots with the specified resolution are returned.",
        PARAM_PROJECT_KEY, STATUS_REVIEWED))
      .setPossibleValues(RESOLUTION_FIXED, RESOLUTION_SAFE)
      .setRequired(false);
    action.createParam(PARAM_SINCE_LEAK_PERIOD)
      .setDescription("If '%s' is provided, only Security Hotspots created since the leak period are returned.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
    action.createParam(PARAM_ONLY_MINE)
      .setDescription("If 'projectKey' is provided, returns only Security Hotspots assigned to the current user")
      .setBooleanPossibleValues()
      .setRequired(false);
    action.createParam(PARAM_OWASP_TOP_10)
      .setDescription("Comma-separated list of OWASP Top 10 lowercase categories.")
      .setSince("8.6")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    action.createParam(PARAM_SANS_TOP_25)
      .setDescription("Comma-separated list of SANS Top 25 categories.")
      .setSince("8.6")
      .setPossibleValues(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES);
    action.createParam(PARAM_SONARSOURCE_SECURITY)
      .setDescription("Comma-separated list of SonarSource security categories. Use '" + SecurityStandards.SQCategory.OTHERS.getKey() +
        "' to select issues not associated with any category")
      .setSince("8.6")
      .setPossibleValues(Arrays.stream(SecurityStandards.SQCategory.values()).map(SecurityStandards.SQCategory::getKey).collect(Collectors.toList()));
    action.createParam(PARAM_CWE)
      .setDescription("Comma-separated list of CWE numbers")
      .setExampleValue("89,434,352")
      .setSince("8.8");
    action.createParam(PARAM_FILES)
      .setDescription("Comma-separated list of files. Returns only hotspots found in those files")
      .setExampleValue("src/main/java/org/sonar/server/Test.java")
      .setSince("9.0");

    action.setResponseExample(getClass().getResource("search-example.json"));
  }

  private void validateParameters(WsRequest wsRequest) {
    Optional<String> projectKey = wsRequest.getProjectKey();
    Optional<String> branch = wsRequest.getBranch();
    Optional<String> pullRequest = wsRequest.getPullRequest();
    Set<String> hotspotKeys = wsRequest.getHotspotKeys();
    checkArgument(
      projectKey.isPresent() || !hotspotKeys.isEmpty(),
      "A value must be provided for either parameter '%s' or parameter '%s'", PARAM_PROJECT_KEY, PARAM_HOTSPOTS);

    checkArgument(
      branch.isEmpty() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_BRANCH, PARAM_PROJECT_KEY);
    checkArgument(
      pullRequest.isEmpty() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_PULL_REQUEST, PARAM_PROJECT_KEY);
    checkArgument(
      !(branch.isPresent() && pullRequest.isPresent()),
      "Only one of parameters '%s' and '%s' can be provided", PARAM_BRANCH, PARAM_PULL_REQUEST);

    Optional<String> status = wsRequest.getStatus();
    Optional<String> resolution = wsRequest.getResolution();
    checkArgument(status.isEmpty() || hotspotKeys.isEmpty(),
      "Parameter '%s' can't be used with parameter '%s'", PARAM_STATUS, PARAM_HOTSPOTS);
    checkArgument(resolution.isEmpty() || hotspotKeys.isEmpty(),
      "Parameter '%s' can't be used with parameter '%s'", PARAM_RESOLUTION, PARAM_HOTSPOTS);

    resolution.ifPresent(
      r -> checkArgument(status.filter(STATUS_REVIEWED::equals).isPresent(),
        "Value '%s' of parameter '%s' can only be provided if value of parameter '%s' is '%s'",
        r, PARAM_RESOLUTION, PARAM_STATUS, STATUS_REVIEWED));

    if (wsRequest.isOnlyMine()) {
      checkArgument(userSession.isLoggedIn(),
        "Parameter '%s' requires user to be logged in", PARAM_ONLY_MINE);
      checkArgument(wsRequest.getProjectKey().isPresent(),
        "Parameter '%s' can be used with parameter '%s' only", PARAM_ONLY_MINE, PARAM_PROJECT_KEY);
    }
  }

  private Optional<ComponentDto> getAndValidateProjectOrApplication(DbSession dbSession, WsRequest wsRequest) {
    return wsRequest.getProjectKey().map(projectKey -> {
      ComponentDto project = getProject(dbSession, projectKey, wsRequest.getBranch().orElse(null), wsRequest.getPullRequest().orElse(null))
        .filter(t -> Scopes.PROJECT.equals(t.scope()) && SUPPORTED_QUALIFIERS.contains(t.qualifier()))
        .filter(ComponentDto::isEnabled)
        .orElseThrow(() -> new NotFoundException(format("Project '%s' not found", projectKey)));
      userSession.checkComponentPermission(USER, project);
      userSession.checkChildProjectsPermission(USER, project);
      return project;
    });
  }

  private Optional<ComponentDto> getProject(DbSession dbSession, String projectKey, @Nullable String branch, @Nullable String pullRequest) {
    if (branch != null) {
      return dbClient.componentDao().selectByKeyAndBranch(dbSession, projectKey, branch);
    } else if (pullRequest != null) {
      return dbClient.componentDao().selectByKeyAndPullRequest(dbSession, projectKey, pullRequest);
    }
    return dbClient.componentDao().selectByKey(dbSession, projectKey);
  }

  private SearchResponseData searchHotspots(WsRequest wsRequest, DbSession dbSession, @Nullable ComponentDto project) {
    SearchResponse result = doIndexSearch(wsRequest, dbSession, project);
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .collect(toList(result.getHits().getHits().length));

    List<IssueDto> hotspots = toIssueDtos(dbSession, issueKeys);

    Paging paging = forPageIndex(wsRequest.getPage()).withPageSize(wsRequest.getIndex()).andTotal((int) getTotalHits(result).value);
    return new SearchResponseData(paging, hotspots);
  }

  private static TotalHits getTotalHits(SearchResponse response) {
    return ofNullable(response.getHits().getTotalHits()).orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  private List<IssueDto> toIssueDtos(DbSession dbSession, List<String> issueKeys) {
    List<IssueDto> unorderedHotspots = dbClient.issueDao().selectByKeys(dbSession, issueKeys);
    Map<String, IssueDto> hotspotsByKey = unorderedHotspots
      .stream()
      .collect(uniqueIndex(IssueDto::getKey, unorderedHotspots.size()));

    return issueKeys.stream()
      .map(hotspotsByKey::get)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private SearchResponse doIndexSearch(WsRequest wsRequest, DbSession dbSession, @Nullable ComponentDto project) {
    var builder = IssueQuery.builder()
      .types(singleton(RuleType.SECURITY_HOTSPOT.name()))
      .sort(IssueQuery.SORT_HOTSPOTS)
      .asc(true)
      .statuses(wsRequest.getStatus().map(Collections::singletonList).orElse(STATUSES));

    if (project != null) {
      String projectUuid = firstNonNull(project.getMainBranchProjectUuid(), project.uuid());
      if (Qualifiers.APP.equals(project.qualifier())) {
        builder.viewUuids(singletonList(projectUuid));
        if (wsRequest.isSinceLeakPeriod() && wsRequest.getPullRequest().isEmpty()) {
          addSinceLeakPeriodFilterByProjects(builder, dbSession, project);
        }
      } else {
        builder.projectUuids(singletonList(projectUuid));
        if (wsRequest.isSinceLeakPeriod() && wsRequest.getPullRequest().isEmpty()) {
          addSinceLeakPeriodFilter(dbSession, project, builder);
        }
      }

      addMainBranchFilter(project, builder);
    }

    if (!wsRequest.getHotspotKeys().isEmpty()) {
      builder.issueKeys(wsRequest.getHotspotKeys());
    }

    if (!wsRequest.getFiles().isEmpty()) {
      builder.files(wsRequest.getFiles());
    }

    if (wsRequest.isOnlyMine()) {
      userSession.checkLoggedIn();
      builder.assigneeUuids(Collections.singletonList(userSession.getUuid()));
    }

    wsRequest.getStatus().ifPresent(status -> builder.resolved(STATUS_REVIEWED.equals(status)));
    wsRequest.getResolution().ifPresent(resolution -> builder.resolutions(singleton(resolution)));
    addSecurityStandardFilters(wsRequest, builder);

    IssueQuery query = builder.build();
    SearchOptions searchOptions = new SearchOptions()
      .setPage(wsRequest.page, wsRequest.index);
    return issueIndex.search(query, searchOptions);
  }

  private static void addSecurityStandardFilters(WsRequest wsRequest, IssueQuery.Builder builder) {
    if (!wsRequest.getOwaspTop10().isEmpty()) {
      builder.owaspTop10(wsRequest.getOwaspTop10());
    }
    if (!wsRequest.getSansTop25().isEmpty()) {
      builder.sansTop25(wsRequest.getSansTop25());
    }
    if (!wsRequest.getSonarsourceSecurity().isEmpty()) {
      builder.sonarsourceSecurity(wsRequest.getSonarsourceSecurity());
    }
    if (!wsRequest.getCwe().isEmpty()) {
      builder.cwe(wsRequest.getCwe());
    }
  }

  private static void addMainBranchFilter(@NotNull ComponentDto project, IssueQuery.Builder builder) {
    if (project.getMainBranchProjectUuid() == null) {
      builder.mainBranch(true);
    } else {
      builder.branchUuid(project.uuid());
      builder.mainBranch(false);
    }
  }

  private void addSinceLeakPeriodFilter(DbSession dbSession, @NotNull ComponentDto project, IssueQuery.Builder builder) {
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, project.uuid());

    boolean isLastAnalysisUsingReferenceBranch = snapshot.map(SnapshotDto::getPeriodMode)
      .orElse("").equals(REFERENCE_BRANCH.name());

    if (isLastAnalysisUsingReferenceBranch) {
      builder.newCodeOnReference(true);
    } else {
      var sinceDate = snapshot
        .map(s -> longToDate(s.getPeriodDate()))
        .orElseGet(() -> new Date(system2.now()));

      builder.createdAfter(sinceDate, false);
    }
  }

  private void addSinceLeakPeriodFilterByProjects(IssueQuery.Builder builder, DbSession dbSession, ComponentDto application) {
    Set<String> projectUuids;
    if (application.getMainBranchProjectUuid() == null) {
      projectUuids = dbClient.applicationProjectsDao().selectProjects(dbSession, application.uuid()).stream()
        .map(ProjectDto::getUuid)
        .collect(Collectors.toSet());
    } else {
      projectUuids = dbClient.applicationProjectsDao().selectProjectBranchesFromAppBranchUuid(dbSession, application.uuid()).stream()
        .map(BranchDto::getUuid)
        .collect(Collectors.toSet());
    }

    long now = system2.now();

    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids);

    Set<String> newCodeReferenceByProjects = snapshots
      .stream()
      .filter(s -> !isNullOrEmpty(s.getPeriodMode()) && s.getPeriodMode().equals(REFERENCE_BRANCH.name()))
      .map(SnapshotDto::getComponentUuid)
      .collect(toSet());

    Map<String, IssueQuery.PeriodStart> leakByProjects = snapshots
      .stream()
      .filter(s -> isNullOrEmpty(s.getPeriodMode()) || !s.getPeriodMode().equals(REFERENCE_BRANCH.name()))
      .collect(uniqueIndex(SnapshotDto::getComponentUuid, s ->
        new IssueQuery.PeriodStart(longToDate(s.getPeriodDate() == null ? now : s.getPeriodDate()), false)));

    builder.createdAfterByProjectUuids(leakByProjects);
    builder.newCodeOnReferenceByProjectUuids(newCodeReferenceByProjects);
  }

  private void loadComponents(DbSession dbSession, SearchResponseData searchResponseData) {
    Set<String> componentUuids = searchResponseData.getOrderedHotspots().stream()
      .flatMap(hotspot -> Stream.of(hotspot.getComponentUuid(), hotspot.getProjectUuid()))
      .collect(Collectors.toSet());

    Set<String> locationComponentUuids = searchResponseData.getOrderedHotspots()
      .stream()
      .flatMap(hotspot -> getHotspotLocationComponentUuids(hotspot).stream())
      .collect(Collectors.toSet());

    Set<String> aggregatedComponentUuids = Stream.of(componentUuids, locationComponentUuids)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    if (!aggregatedComponentUuids.isEmpty()) {
      searchResponseData.addComponents(dbClient.componentDao().selectByUuids(dbSession, aggregatedComponentUuids));
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

  private void loadRules(DbSession dbSession, SearchResponseData searchResponseData) {
    Set<RuleKey> ruleKeys = searchResponseData.getOrderedHotspots()
      .stream()
      .map(IssueDto::getRuleKey)
      .collect(Collectors.toSet());
    if (!ruleKeys.isEmpty()) {
      searchResponseData.addRules(dbClient.ruleDao().selectDefinitionByKeys(dbSession, ruleKeys));
    }
  }

  private SearchWsResponse formatResponse(SearchResponseData searchResponseData) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    formatPaging(searchResponseData, responseBuilder);
    if (!searchResponseData.isEmpty()) {
      formatHotspots(searchResponseData, responseBuilder);
      formatComponents(searchResponseData, responseBuilder);
    }
    return responseBuilder.build();
  }

  private static void formatPaging(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    Paging paging = searchResponseData.getPaging();
    Common.Paging.Builder pagingBuilder = Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());

    responseBuilder.setPaging(pagingBuilder.build());
  }

  private void formatHotspots(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    List<IssueDto> orderedHotspots = searchResponseData.getOrderedHotspots();
    if (orderedHotspots.isEmpty()) {
      return;
    }

    SearchWsResponse.Hotspot.Builder builder = SearchWsResponse.Hotspot.newBuilder();
    for (IssueDto hotspot : orderedHotspots) {
      RuleDefinitionDto rule = searchResponseData.getRule(hotspot.getRuleKey())
        // due to join with table Rule when retrieving data from Issues, this can't happen
        .orElseThrow(() -> new IllegalStateException(format(
          "Rule with key '%s' not found for Hotspot '%s'", hotspot.getRuleKey(), hotspot.getKey())));
      SecurityStandards.SQCategory sqCategory = fromSecurityStandards(rule.getSecurityStandards()).getSqCategory();
      builder
        .clear()
        .setKey(hotspot.getKey())
        .setComponent(hotspot.getComponentKey())
        .setProject(hotspot.getProjectKey())
        .setSecurityCategory(sqCategory.getKey())
        .setVulnerabilityProbability(sqCategory.getVulnerability().name());
      ofNullable(hotspot.getStatus()).ifPresent(builder::setStatus);
      ofNullable(hotspot.getResolution()).ifPresent(builder::setResolution);
      ofNullable(hotspot.getLine()).ifPresent(builder::setLine);
      builder.setMessage(nullToEmpty(hotspot.getMessage()));
      ofNullable(hotspot.getAssigneeUuid()).ifPresent(builder::setAssignee);
      builder.setAuthor(nullToEmpty(hotspot.getAuthorLogin()));
      builder.setCreationDate(formatDateTime(hotspot.getIssueCreationDate()));
      builder.setUpdateDate(formatDateTime(hotspot.getIssueUpdateDate()));
      completeHotspotLocations(hotspot, builder, searchResponseData);
      responseBuilder.addHotspots(builder.build());
    }
  }

  private void completeHotspotLocations(IssueDto hotspot, SearchWsResponse.Hotspot.Builder hotspotBuilder, SearchResponseData data) {
    DbIssues.Locations locations = hotspot.parseLocations();

    if (locations == null) {
      return;
    }

    textRangeFormatter.formatTextRange(locations, hotspotBuilder::setTextRange);

    for (DbIssues.Flow flow : locations.getFlowList()) {
      Common.Flow.Builder targetFlow = Common.Flow.newBuilder();
      for (DbIssues.Location flowLocation : flow.getLocationList()) {
        targetFlow.addLocations(textRangeFormatter.formatLocation(flowLocation, hotspotBuilder.getComponent(), data.getComponentsByUuid()));
      }
      hotspotBuilder.addFlows(targetFlow.build());
    }
  }

  private void formatComponents(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    Collection<ComponentDto> components = searchResponseData.getComponents();
    if (components.isEmpty()) {
      return;
    }

    Hotspots.Component.Builder builder = Hotspots.Component.newBuilder();
    for (ComponentDto component : components) {
      responseBuilder.addComponents(responseFormatter.formatComponent(builder, component));
    }
  }

  private static final class WsRequest {
    private final int page;
    private final int index;
    private final String projectKey;
    private final String branch;
    private final String pullRequest;
    private final Set<String> hotspotKeys;
    private final String status;
    private final String resolution;
    private final boolean sinceLeakPeriod;
    private final boolean onlyMine;
    private final Set<String> owaspTop10;
    private final Set<String> sansTop25;
    private final Set<String> sonarsourceSecurity;
    private final Set<String> cwe;
    private final Set<String> files;

    private WsRequest(int page, int index,
      @Nullable String projectKey, @Nullable String branch, @Nullable String pullRequest,
      Set<String> hotspotKeys,
      @Nullable String status, @Nullable String resolution, @Nullable Boolean sinceLeakPeriod,
      @Nullable Boolean onlyMine, Set<String> owaspTop10, Set<String> sansTop25, Set<String> sonarsourceSecurity,
      Set<String> cwe, @Nullable Set<String> files) {
      this.page = page;
      this.index = index;
      this.projectKey = projectKey;
      this.branch = branch;
      this.pullRequest = pullRequest;
      this.hotspotKeys = hotspotKeys;
      this.status = status;
      this.resolution = resolution;
      this.sinceLeakPeriod = sinceLeakPeriod != null && sinceLeakPeriod;
      this.onlyMine = onlyMine != null && onlyMine;
      this.owaspTop10 = owaspTop10;
      this.sansTop25 = sansTop25;
      this.sonarsourceSecurity = sonarsourceSecurity;
      this.cwe = cwe;
      this.files = files;
    }

    int getPage() {
      return page;
    }

    int getIndex() {
      return index;
    }

    Optional<String> getProjectKey() {
      return ofNullable(projectKey);
    }

    Optional<String> getBranch() {
      return ofNullable(branch);
    }

    Optional<String> getPullRequest() {
      return ofNullable(pullRequest);
    }

    Set<String> getHotspotKeys() {
      return hotspotKeys;
    }

    Optional<String> getStatus() {
      return ofNullable(status);
    }

    Optional<String> getResolution() {
      return ofNullable(resolution);
    }

    boolean isSinceLeakPeriod() {
      return sinceLeakPeriod;
    }

    boolean isOnlyMine() {
      return onlyMine;
    }

    public Set<String> getOwaspTop10() {
      return owaspTop10;
    }

    public Set<String> getSansTop25() {
      return sansTop25;
    }

    public Set<String> getSonarsourceSecurity() {
      return sonarsourceSecurity;
    }

    public Set<String> getCwe() {
      return cwe;
    }

    public Set<String> getFiles() {
      return files;
    }
  }

  private static final class SearchResponseData {
    private final Paging paging;
    private final List<IssueDto> orderedHotspots;
    private final Map<String, ComponentDto> componentsByUuid = new HashMap<>();
    private final Map<RuleKey, RuleDefinitionDto> rulesByRuleKey = new HashMap<>();

    private SearchResponseData(Paging paging, List<IssueDto> orderedHotspots) {
      this.paging = paging;
      this.orderedHotspots = orderedHotspots;
    }

    boolean isEmpty() {
      return orderedHotspots.isEmpty();
    }

    public Paging getPaging() {
      return paging;
    }

    List<IssueDto> getOrderedHotspots() {
      return orderedHotspots;
    }

    void addComponents(Collection<ComponentDto> components) {
      for (ComponentDto component : components) {
        componentsByUuid.put(component.uuid(), component);
      }
    }

    Collection<ComponentDto> getComponents() {
      return componentsByUuid.values();
    }

    public Map<String, ComponentDto> getComponentsByUuid() {
      return componentsByUuid;
    }

    void addRules(Collection<RuleDefinitionDto> rules) {
      rules.forEach(t -> rulesByRuleKey.put(t.getKey(), t));
    }

    Optional<RuleDefinitionDto> getRule(RuleKey ruleKey) {
      return ofNullable(rulesByRuleKey.get(ruleKey));
    }

  }
}
