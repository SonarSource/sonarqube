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

import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsUtils.nullToEmpty;

public class SearchAction implements HotspotsWsAction {
  private static final String PARAM_PROJECT_KEY = "projectKey";

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
      .setDescription("Key of the project")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);
    // FIXME add response example and test it
    // action.setResponseExample()
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsRequest wsRequest = toWsRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = validateRequest(dbSession, wsRequest);

      SearchResponseData searchResponseData = searchHotspots(wsRequest, dbSession, project);
      loadComponents(dbSession, searchResponseData);
      loadRules(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private static WsRequest toWsRequest(Request request) {
    return new WsRequest(request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE), request.mandatoryParam(PARAM_PROJECT_KEY));
  }

  private ComponentDto validateRequest(DbSession dbSession, WsRequest wsRequest) {
    ComponentDto project = dbClient.componentDao().selectByKey(dbSession, wsRequest.getProjectKey())
      .filter(t -> Scopes.PROJECT.equals(t.scope()) && Qualifiers.PROJECT.equals(t.qualifier()))
      .filter(ComponentDto::isEnabled)
      .filter(t -> t.getMainBranchProjectUuid() == null)
      .orElseThrow(() -> new NotFoundException(String.format("Project '%s' not found", wsRequest.getProjectKey())));
    userSession.checkComponentPermission(UserRole.USER, project);
    return project;
  }

  private SearchResponseData searchHotspots(WsRequest wsRequest, DbSession dbSession, ComponentDto project) {
    SearchResponse result = doIndexSearch(wsRequest, project);
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

  private SearchResponse doIndexSearch(WsRequest wsRequest, ComponentDto project) {
    IssueQuery.Builder builder = IssueQuery.builder()
      .projectUuids(Collections.singletonList(project.uuid()))
      .organizationUuid(project.getOrganizationUuid())
      .types(Collections.singleton(RuleType.SECURITY_HOTSPOT.name()))
      .resolved(false)
      .sort(IssueQuery.SORT_HOTSPOTS)
      .asc(true);
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

  private void formatPaging(SearchResponseData searchResponseData, SearchWsResponse.Builder responseBuilder) {
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
        .orElseThrow(() -> new IllegalStateException(String.format(
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
      // FIXME resolution field will be added later
      // ofNullable(hotspot.getResolution()).ifPresent(builder::setResolution);
      ofNullable(hotspot.getLine()).ifPresent(builder::setLine);
      builder.setMessage(nullToEmpty(hotspot.getMessage()));
      ofNullable(hotspot.getAssigneeUuid()).ifPresent(builder::setAssignee);
      // FIXME Filter author only if user is member of the organization (as done in issues/search WS)
      // if (data.getUserOrganizationUuids().contains(component.getOrganizationUuid())) {
      builder.setAuthor(nullToEmpty(hotspot.getAuthorLogin()));
      // }
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

    private WsRequest(int page, int index, String projectKey) {
      this.page = page;
      this.index = index;
      this.projectKey = projectKey;
    }

    int getPage() {
      return page;
    }

    int getIndex() {
      return index;
    }

    String getProjectKey() {
      return projectKey;
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
