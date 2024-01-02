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
package org.sonar.server.qualitygate.ws;

import java.util.List;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;
import org.sonar.db.user.SearchPermissionQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Qualitygates;

import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.fromParam;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.qualitygate.SearchQualityGatePermissionQuery.builder;
import static org.sonar.db.user.SearchPermissionQuery.ANY;
import static org.sonar.db.user.SearchPermissionQuery.IN;
import static org.sonar.db.user.SearchPermissionQuery.OUT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_SEARCH_GROUPS;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchGroupsAction implements QualityGatesWsAction {

  private static final Map<WebService.SelectionMode, String> MEMBERSHIP = Map.of(WebService.SelectionMode.SELECTED, IN, DESELECTED, OUT, ALL, ANY);

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;

  public SearchGroupsAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_SEARCH_GROUPS)
      .setDescription("List the groups that are allowed to edit a Quality Gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>Edit right on the specified quality gate</li>" +
        "</ul>")
      .setHandler(this)
      .setInternal(true)
      .addSelectionModeParam()
      .addSearchQuery("sonar", "group names")
      .addPagingParams(25)
      .setResponseExample(getClass().getResource("search_groups-example.json"))
      .setSince("9.2");

    action.createParam(PARAM_GATE_NAME)
      .setDescription("Quality Gate name")
      .setRequired(true)
      .setExampleValue("SonarSource Way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchQualityGateUsersRequest wsRequest = buildRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto gate = wsSupport.getByName(dbSession, wsRequest.getQualityGate());
      wsSupport.checkCanLimitedEdit(dbSession, gate);

      SearchPermissionQuery query = builder()
        .setQualityGate(gate)
        .setQuery(wsRequest.getQuery())
        .setMembership(MEMBERSHIP.get(fromParam(wsRequest.getSelected())))
        .build();
      int total = dbClient.qualityGateGroupPermissionsDao().countByQuery(dbSession, query);
      List<SearchGroupMembershipDto> groupMemberships = dbClient.qualityGateGroupPermissionsDao().selectByQuery(dbSession, query,
        forPage(wsRequest.getPage()).andSize(wsRequest.getPageSize()));
      Map<String, GroupDto> groupsByUuid = dbClient.groupDao().selectByUuids(dbSession,
          groupMemberships.stream().map(SearchGroupMembershipDto::getGroupUuid).collect(MoreCollectors.toList()))
        .stream()
        .collect(MoreCollectors.uniqueIndex(GroupDto::getUuid));
      writeProtobuf(
        Qualitygates.SearchGroupsResponse.newBuilder()
          .addAllGroups(groupMemberships.stream()
            .map(groupsMembership -> toGroup(groupsByUuid.get(groupsMembership.getGroupUuid()), groupsMembership.isSelected()))
            .collect(toList()))
          .setPaging(buildPaging(wsRequest, total)).build(),
        request, response);
    }
  }

  private static SearchQualityGateUsersRequest buildRequest(Request request) {
    return SearchQualityGateUsersRequest.builder()
      .setQualityGate(request.mandatoryParam(PARAM_GATE_NAME))
      .setQuery(request.param(TEXT_QUERY))
      .setSelected(request.mandatoryParam(SELECTED))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(request.mandatoryParamAsInt(PAGE_SIZE))
      .build();
  }

  private static Qualitygates.SearchGroupsResponse.Group toGroup(GroupDto group, boolean isSelected) {
    Qualitygates.SearchGroupsResponse.Group.Builder builder = Qualitygates.SearchGroupsResponse.Group.newBuilder()
      .setName(group.getName())
      .setSelected(isSelected);
    ofNullable(group.getDescription()).ifPresent(builder::setDescription);
    return builder.build();
  }

  private static Common.Paging buildPaging(SearchQualityGateUsersRequest wsRequest, int total) {
    return Common.Paging.newBuilder()
      .setPageIndex(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setTotal(total)
      .build();
  }

}
