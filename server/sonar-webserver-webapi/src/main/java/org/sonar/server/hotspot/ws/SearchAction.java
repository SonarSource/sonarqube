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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Hotspots;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements HotspotsWsAction {
  private static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueIndex issueIndex;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public SearchAction(DbClient dbClient, UserSession userSession, IssueIndex issueIndex, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueIndex = issueIndex;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
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
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);
      ComponentDto project = dbClient.componentDao().selectByKey(dbSession, projectKey)
        .filter(t -> Scopes.PROJECT.equals(t.scope()) && Qualifiers.PROJECT.equals(t.qualifier()))
        .filter(ComponentDto::isEnabled)
        .filter(t -> t.getMainBranchProjectUuid() == null)
        .orElseThrow(() -> new NotFoundException(String.format("Project '%s' not found", projectKey)));
      userSession.checkComponentPermission(UserRole.USER, project);

      List<IssueDto> orderedIssues = searchHotspots(request, dbSession, project);
      SearchResponseData searchResponseData = new SearchResponseData(orderedIssues);
      loadComponents(dbSession, searchResponseData);
      loadRules(dbSession, searchResponseData);
      writeProtobuf(formatResponse(searchResponseData), request, response);
    }
  }

  private List<IssueDto> searchHotspots(Request request, DbSession dbSession, ComponentDto project) {
    List<String> issueKeys = searchHotspots(request, project);

    List<IssueDto> unorderedIssues = dbClient.issueDao().selectByKeys(dbSession, issueKeys);
    Map<String, IssueDto> unorderedIssuesMap = unorderedIssues
      .stream()
      .collect(uniqueIndex(IssueDto::getKey, unorderedIssues.size()));

    return issueKeys.stream()
      .map(unorderedIssuesMap::get)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private List<String> searchHotspots(Request request, ComponentDto project) {
    IssueQuery.Builder builder = IssueQuery.builder()
      .projectUuids(Collections.singletonList(project.uuid()))
      .organizationUuid(project.getOrganizationUuid())
      .types(Collections.singleton(RuleType.SECURITY_HOTSPOT.name()))
      .resolved(false);
    IssueQuery query = builder.build();
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE));
    SearchResponse result = issueIndex.search(query, searchOptions);
    return Arrays.stream(result.getHits().getHits())
      .map(SearchHit::getId)
      .collect(MoreCollectors.toList(result.getHits().getHits().length));
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

  private Hotspots.SearchWsResponse formatResponse(SearchResponseData searchResponseData) {
    Hotspots.SearchWsResponse.Builder responseBuilder = Hotspots.SearchWsResponse.newBuilder();
    if (!searchResponseData.isEmpty()) {
      formatHotspots(searchResponseData, responseBuilder);
      formatComponents(searchResponseData, responseBuilder);
      formatRules(searchResponseData, responseBuilder);
    }
    return responseBuilder.build();
  }

  private static void formatHotspots(SearchResponseData searchResponseData, Hotspots.SearchWsResponse.Builder responseBuilder) {
    List<IssueDto> orderedHotspots = searchResponseData.getOrderedHotspots();
    if (orderedHotspots.isEmpty()) {
      return;
    }

    Hotspots.Hotspot.Builder builder = Hotspots.Hotspot.newBuilder();
    for (IssueDto hotspot : orderedHotspots) {
      builder
        .clear()
        .setKey(hotspot.getKey())
        .setComponent(hotspot.getComponentKey())
        .setProject(hotspot.getProjectKey())
        .setRule(hotspot.getRuleKey().toString());
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

  private void formatComponents(SearchResponseData searchResponseData, Hotspots.SearchWsResponse.Builder responseBuilder) {
    Set<ComponentDto> components = searchResponseData.getComponents();
    if (components.isEmpty()) {
      return;
    }

    Hotspots.Component.Builder builder = Hotspots.Component.newBuilder();
    for (ComponentDto component : components) {
      builder
        .clear()
        .setOrganization(defaultOrganizationProvider.get().getKey())
        .setKey(component.getKey())
        .setQualifier(component.qualifier())
        .setName(component.name())
        .setLongName(component.longName());
      ofNullable(component.path()).ifPresent(builder::setPath);

      responseBuilder.addComponents(builder.build());
    }
  }

  private void formatRules(SearchResponseData searchResponseData, Hotspots.SearchWsResponse.Builder responseBuilder) {
    Set<RuleDefinitionDto> rules = searchResponseData.getRules();
    if (rules.isEmpty()) {
      return;
    }

    Hotspots.Rule.Builder ruleBuilder = Hotspots.Rule.newBuilder();
    for (RuleDefinitionDto rule : rules) {
      SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(rule.getSecurityStandards());
      SecurityStandards.SQCategory sqCategory = securityStandards.getSqCategory();
      ruleBuilder
        .clear()
        .setKey(rule.getKey().toString())
        .setName(nullToEmpty(rule.getName()))
        .setSecurityCategory(sqCategory.getKey())
        .setVulnerabilityProbability(sqCategory.getVulnerability().name());

      responseBuilder.addRules(ruleBuilder.build());
    }
  }

  private static final class SearchResponseData {
    private final List<IssueDto> orderedHotspots;
    private final Set<ComponentDto> components = new HashSet<>();
    private final Set<RuleDefinitionDto> rules = new HashSet<>();

    private SearchResponseData(List<IssueDto> orderedHotspots) {
      this.orderedHotspots = orderedHotspots;
    }

    boolean isEmpty() {
      return orderedHotspots.isEmpty();
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
      this.rules.addAll(rules);
    }

    Set<RuleDefinitionDto> getRules() {
      return rules;
    }

  }
}
