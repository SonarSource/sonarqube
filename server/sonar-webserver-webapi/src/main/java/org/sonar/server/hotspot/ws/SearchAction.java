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
package org.sonar.server.hotspot.ws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.Hotspots.SearchWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
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
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsUtils.nullToEmpty;

public class SearchAction implements HotspotsWsAction {
  private static final Set<String> SUPPORTED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.APP);
  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_STATUS = "status";
  private static final String PARAM_RESOLUTION = "resolution";
  private static final String PARAM_HOTSPOTS = "hotspots";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private static final String PARAM_SINCE_LEAK_PERIOD = "sinceLeakPeriod";
  private static final String PARAM_ONLY_MINE = "onlyMine";
  private static final List<String> STATUSES = ImmutableList.of(STATUS_TO_REVIEW, STATUS_REVIEWED);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final HotspotWsResponseFormatter responseFormatter;

  public SearchAction(DbClient dbClient, UserSession userSession, IssueIndex issueIndex,
    HotspotWsResponseFormatter responseFormatter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.responseFormatter = responseFormatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setHandler(this)
      .setDescription("Search for Security Hotpots.")
      .setSince("8.1")
      .setInternal(true);

    action.addPagingParams(100);
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription(format(
        "Key of the project or application. This parameter is required unless %s is provided.",
        PARAM_HOTSPOTS))
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true);
    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setInternal(true);
    action.createParam(PARAM_HOTSPOTS)
      .setDescription(format(
        "Comma-separated list of Security Hotspot keys. This parameter is required unless %s is provided.",
        PARAM_PROJECT_KEY))
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
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

    action.setResponseExample(getClass().getResource("search-example.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsRequest wsRequest = toWsRequest(request);
    validateParameters(wsRequest);
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> project = getAndValidateProjectOrApplication(dbSession, wsRequest);

      SearchResponseData searchResponseData = searchHotspots(wsRequest, dbSession, project, wsRequest.getHotspotKeys());
      loadComponents(dbSession, searchResponseData);
      loadRules(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private static WsRequest toWsRequest(Request request) {
    List<String> hotspotKeysList = request.paramAsStrings(PARAM_HOTSPOTS);
    Set<String> hotspotKeys = hotspotKeysList == null ? ImmutableSet.of() : hotspotKeysList.stream().collect(MoreCollectors.toSet(hotspotKeysList.size()));
    return new WsRequest(
      request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE),
      request.param(PARAM_PROJECT_KEY), request.param(PARAM_BRANCH), request.param(PARAM_PULL_REQUEST),
      hotspotKeys,
      request.param(PARAM_STATUS), request.param(PARAM_RESOLUTION),
      request.paramAsBoolean(PARAM_SINCE_LEAK_PERIOD),
      request.paramAsBoolean(PARAM_ONLY_MINE));
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
      !branch.isPresent() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_BRANCH, PARAM_PROJECT_KEY);
    checkArgument(
      !pullRequest.isPresent() || projectKey.isPresent(),
      "Parameter '%s' must be used with parameter '%s'", PARAM_PULL_REQUEST, PARAM_PROJECT_KEY);
    checkArgument(
      !(branch.isPresent() && pullRequest.isPresent()),
      "Only one of parameters '%s' and '%s' can be provided", PARAM_BRANCH, PARAM_PULL_REQUEST);

    Optional<String> status = wsRequest.getStatus();
    Optional<String> resolution = wsRequest.getResolution();
    checkArgument(!status.isPresent() || hotspotKeys.isEmpty(),
      "Parameter '%s' can't be used with parameter '%s'", PARAM_STATUS, PARAM_HOTSPOTS);
    checkArgument(!resolution.isPresent() || hotspotKeys.isEmpty(),
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
      ComponentDto project = getProject(dbSession, projectKey, wsRequest.getBranch(), wsRequest.getPullRequest())
        .filter(t -> Scopes.PROJECT.equals(t.scope()) && SUPPORTED_QUALIFIERS.contains(t.qualifier()))
        .filter(ComponentDto::isEnabled)
        .orElseThrow(() -> new NotFoundException(format("Project '%s' not found", projectKey)));
      userSession.checkComponentPermission(UserRole.USER, project);
      return project;
    });
  }

  private Optional<ComponentDto> getProject(DbSession dbSession, String projectKey, Optional<String> branch, Optional<String> pullRequest) {
    if (branch.isPresent()) {
      return dbClient.componentDao().selectByKeyAndBranch(dbSession, projectKey, branch.get());
    } else if (pullRequest.isPresent()) {
      return dbClient.componentDao().selectByKeyAndPullRequest(dbSession, projectKey, pullRequest.get());
    }
    return dbClient.componentDao().selectByKey(dbSession, projectKey);
  }

  private SearchResponseData searchHotspots(WsRequest wsRequest, DbSession dbSession, Optional<ComponentDto> project, Set<String> hotspotKeys) {
    SearchResponse result = doIndexSearch(wsRequest, dbSession, project, hotspotKeys);
    List<String> issueKeys = Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .collect(toList(result.getHits().getHits().length));

    List<IssueDto> hotspots = toIssueDtos(dbSession, issueKeys);

    Paging paging = forPageIndex(wsRequest.getPage()).withPageSize(wsRequest.getIndex()).andTotal((int) result.getHits().getTotalHits());
    return new SearchResponseData(paging, hotspots);
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

  private SearchResponse doIndexSearch(WsRequest wsRequest, DbSession dbSession, Optional<ComponentDto> project, Set<String> hotspotKeys) {
    IssueQuery.Builder builder = IssueQuery.builder()
      .types(singleton(RuleType.SECURITY_HOTSPOT.name()))
      .sort(IssueQuery.SORT_HOTSPOTS)
      .asc(true)
      .statuses(wsRequest.getStatus().map(Collections::singletonList).orElse(STATUSES));
    project.ifPresent(p -> {
      builder.organizationUuid(p.getOrganizationUuid());

      String projectUuid = firstNonNull(p.getMainBranchProjectUuid(), p.uuid());
      if (Qualifiers.APP.equals(p.qualifier())) {
        builder.viewUuids(singletonList(projectUuid));
      } else {
        builder.projectUuids(singletonList(projectUuid));
      }

      if (p.getMainBranchProjectUuid() == null) {
        builder.mainBranch(true);
      } else {
        builder.branchUuid(p.uuid());
        builder.mainBranch(false);
      }

      if (wsRequest.isSinceLeakPeriod()) {
        dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, p.uuid())
          .map(s -> longToDate(s.getPeriodDate()))
          .ifPresent(d -> builder.createdAfter(d, false));
      }
    });
    if (!hotspotKeys.isEmpty()) {
      builder.issueKeys(hotspotKeys);
    }

    if (wsRequest.isOnlyMine()) {
      userSession.checkLoggedIn();
      builder.assigneeUuids(Collections.singletonList(userSession.getUuid()));
    }

    wsRequest.getStatus().ifPresent(status -> builder.resolved(STATUS_REVIEWED.equals(status)));
    wsRequest.getResolution().ifPresent(resolution -> builder.resolutions(singleton(resolution)));

    IssueQuery query = builder.build();
    SearchOptions searchOptions = new SearchOptions()
      .setPage(wsRequest.page, wsRequest.index);
    return issueIndex.search(query, searchOptions);
  }

  private void loadComponents(DbSession dbSession, SearchResponseData searchResponseData) {
    Set<String> componentKeys = searchResponseData.getOrderedHotspots()
      .stream()
      .flatMap(hotspot -> Stream.of(hotspot.getComponentKey(), hotspot.getProjectKey()))
      .collect(Collectors.toSet());
    if (!componentKeys.isEmpty()) {
      searchResponseData.addComponents(dbClient.componentDao().selectByDbKeys(dbSession, componentKeys));
    }
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

  private static void formatHotspots(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
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

      responseBuilder.addHotspots(builder.build());
    }
  }

  private void formatComponents(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
    Set<ComponentDto> components = searchResponseData.getComponents();
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

    private WsRequest(int page, int index,
      @Nullable String projectKey, @Nullable String branch, @Nullable String pullRequest,
      Set<String> hotspotKeys,
      @Nullable String status, @Nullable String resolution, @Nullable Boolean sinceLeakPeriod,
      @Nullable Boolean onlyMine) {
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
  }

  private static final class SearchResponseData {
    private final Paging paging;
    private final List<IssueDto> orderedHotspots;
    private final Set<ComponentDto> components = new HashSet<>();
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
      this.components.addAll(components);
    }

    Set<ComponentDto> getComponents() {
      return components;
    }

    void addRules(Collection<RuleDefinitionDto> rules) {
      rules.forEach(t -> rulesByRuleKey.put(t.getKey(), t));
    }

    Optional<RuleDefinitionDto> getRule(RuleKey ruleKey) {
      return ofNullable(rulesByRuleKey.get(ruleKey));
    }

  }
}
