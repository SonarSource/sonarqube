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
package org.sonar.server.rule.ws;

import com.google.common.collect.Maps;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleListQuery;
import org.sonar.db.rule.RuleListResult;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.ws.RulesResponseFormatter.SearchResult;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.ListResponse;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.db.rule.RuleListQuery.RuleListQueryBuilder.newRuleListQueryBuilder;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements RulesWsAction {
  private final DbClient dbClient;
  private final RulesResponseFormatter rulesResponseFormatter;

  public ListAction(DbClient dbClient, RulesResponseFormatter rulesResponseFormatter) {
    this.dbClient = dbClient;
    this.rulesResponseFormatter = rulesResponseFormatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller
      .createAction("list")
      .setDescription("List rules, excluding the external rules and the rules with status REMOVED. " +
        "If a quality profile is provided, it returns only the rules activated in this quality profile. " +
        "If no quality profile is provided, it returns all the rules available to the user.")
      .setSince("5.2")
      .setInternal(true)
      .setResponseExample(getClass().getResource("list-example.txt"))
      .setHandler(this);

    action.setChangelog(
      new Change("10.4", "'repository' field changed to 'repo'"),
      new Change("10.4", "Extend the response with 'actives', 'qProfiles' fields"),
      new Change("10.4", "Add pagination"),
      new Change("10.4", String.format("Add the '%s' parameter", PARAM_AVAILABLE_SINCE)),
      new Change("10.4", String.format("Add the '%s' parameter", PARAM_QPROFILE)),
      new Change("10.4", "Add the 'createdAt' sorting field"),
      new Change("10.5", String.format("The sorting parameter '%s' no longer has a default value (was 'createdAt')",
        WebService.Param.SORT)));

    action.createParam(PARAM_AVAILABLE_SINCE)
      .setDescription("Filter rules available since the given date. If no value is provided, all rules are returned. Format is yyyy-MM-dd.")
      .setExampleValue("2014-06-22")
      .setSince("10.4");
    action.createParam(PARAM_QPROFILE)
      .setDescription("Filter rules that are activated in the given quality profile.")
      .setSince("10.4");
    action.createSortParams(Set.of("createdAt"), null, false)
      .setSince("10.4");
    action.addPagingParamsSince(100, 500, "10.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {

      WsRequest wsRequest = toWsRequest(dbSession, request);
      SearchResult searchResult = doSearch(dbSession, wsRequest);
      ListResponse listResponse = buildResponse(wsRequest, dbSession, searchResult);

      writeProtobuf(listResponse, request, response);
    }
  }

  private WsRequest toWsRequest(DbSession dbSession, Request request) {
    WsRequest wsRequest = new WsRequest();

    String sortParam = request.param(WebService.Param.SORT);
    if (sortParam != null) {
      wsRequest.setSortField(sortParam)
        .setAscendingSort(request.mandatoryParamAsBoolean(WebService.Param.ASCENDING));
    }

    return wsRequest
      .setQProfile(getQProfile(dbSession, request))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(request.mandatoryParamAsInt(PAGE_SIZE))
      .setAvailableSince(request.paramAsDate(PARAM_AVAILABLE_SINCE));
  }

  @Nullable
  private QProfileDto getQProfile(DbSession dbSession, Request request) {
    String profileUuid = request.param(PARAM_QPROFILE);
    if (profileUuid == null) {
      return null;
    }
    QProfileDto foundProfile = dbClient.qualityProfileDao().selectByUuid(dbSession, profileUuid);
    return checkFound(foundProfile, "The specified qualityProfile '%s' does not exist", profileUuid);
  }

  private SearchResult doSearch(DbSession dbSession, WsRequest wsRequest) {
    RuleListResult ruleListResult = dbClient.ruleDao().selectRules(dbSession,
      buildRuleListQuery(wsRequest),
      Pagination.forPage(wsRequest.page).andSize(wsRequest.pageSize));
    Map<String, RuleDto> rulesByUuid = Maps.uniqueIndex(dbClient.ruleDao().selectByUuids(dbSession, ruleListResult.getUuids()), RuleDto::getUuid);
    Set<String> ruleUuids = rulesByUuid.keySet();
    List<RuleDto> rules = ruleListResult.getUuids().stream().map(rulesByUuid::get).toList();

    List<String> templateRuleUuids = rules.stream()
      .map(RuleDto::getTemplateUuid)
      .filter(Objects::nonNull)
      .toList();
    List<RuleDto> templateRules = dbClient.ruleDao().selectByUuids(dbSession, templateRuleUuids);
    List<RuleParamDto> ruleParamDtos = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, ruleUuids);

    return new SearchResult()
      .setRules(rules)
      .setRuleParameters(ruleParamDtos)
      .setTemplateRules(templateRules)
      .setTotal(ruleListResult.getTotal());
  }

  private static RuleListQuery buildRuleListQuery(WsRequest wsRequest) {
    return newRuleListQueryBuilder()
      .profileUuid(wsRequest.qProfile != null ? wsRequest.qProfile.getRulesProfileUuid() : null)
      .createdAt(wsRequest.availableSince != null ? wsRequest.availableSince.getTime() : null)
      .sortField(wsRequest.sortField)
      .sortDirection(wsRequest.ascendingSort ? "asc" : "desc")
      .build();
  }

  private ListResponse buildResponse(WsRequest wsRequest, DbSession dbSession, SearchResult searchResult) {
    QProfileDto qProfile = wsRequest.qProfile;
    Rules.Actives actives = rulesResponseFormatter.formatActiveRules(dbSession, qProfile, searchResult.getRules());
    Set<String> qProfiles = actives.getActivesMap().values()
      .stream()
      .map(Rules.ActiveList::getActiveListList)
      .flatMap(List::stream)
      .map(Rules.Active::getQProfile)
      .collect(Collectors.toSet());

    return ListResponse.newBuilder()
      .addAllRules(rulesResponseFormatter.formatRulesList(dbSession, searchResult))
      .setActives(actives)
      .setQProfiles(rulesResponseFormatter.formatQualityProfiles(dbSession, qProfiles))
      .setPaging(Common.Paging.newBuilder()
        .setPageIndex(wsRequest.page)
        .setPageSize(searchResult.getRules().size())
        .setTotal(searchResult.getTotal().intValue())
        .build())
      .build();
  }

  private static class WsRequest {
    private String sortField = null;
    private boolean ascendingSort = true;
    private QProfileDto qProfile = null;
    private int page = 1;
    private int pageSize = 100;
    private Date availableSince = null;

    public WsRequest setSortField(String sortField) {
      this.sortField = sortField;
      return this;
    }

    public WsRequest setAscendingSort(boolean ascendingSort) {
      this.ascendingSort = ascendingSort;
      return this;
    }

    public WsRequest setQProfile(@Nullable QProfileDto qProfile) {
      this.qProfile = qProfile;
      return this;
    }

    public WsRequest setPage(int page) {
      this.page = page;
      return this;
    }

    public WsRequest setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public WsRequest setAvailableSince(@Nullable Date availableSince) {
      this.availableSince = availableSince;
      return this;
    }
  }
}
