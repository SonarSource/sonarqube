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
package org.sonar.server.qualitygate.ws;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.SearchPermissionQuery;
import org.sonar.db.user.SearchUserMembershipDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Qualitygates.SearchUsersResponse;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.fromParam;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.qualitygate.SearchQualityGatePermissionQuery.builder;
import static org.sonar.db.user.SearchPermissionQuery.ANY;
import static org.sonar.db.user.SearchPermissionQuery.IN;
import static org.sonar.db.user.SearchPermissionQuery.OUT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_SEARCH_USERS;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchUsersAction implements QualityGatesWsAction {

  private static final Map<SelectionMode, String> MEMBERSHIP = Map.of(SelectionMode.SELECTED, IN, DESELECTED, OUT, ALL, ANY);

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;
  private final AvatarResolver avatarResolver;

  public SearchUsersAction(DbClient dbClient, QualityGatesWsSupport wsSupport, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.avatarResolver = avatarResolver;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_SEARCH_USERS)
      .setDescription("List the users that are allowed to edit a Quality Gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>Edit right on the specified quality gate</li>" +
        "</ul>")
      .setHandler(this)
      .addSearchQuery("freddy", "names", "logins")
      .addSelectionModeParam()
      .addPagingParams(25)
      .setResponseExample(getClass().getResource("search_users-example.json"))
      .setSince("9.2");

    action.createParam(PARAM_GATE_NAME)
      .setDescription("Quality Gate name")
      .setRequired(true)
      .setExampleValue("Recommended quality gate");
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
      int total = dbClient.qualityGateUserPermissionDao().countByQuery(dbSession, query);
      List<SearchUserMembershipDto> usersMembership = dbClient.qualityGateUserPermissionDao().selectByQuery(dbSession, query,
        forPage(wsRequest.getPage()).andSize(wsRequest.getPageSize()));
      Map<String, UserDto> usersById = dbClient.userDao().selectByUuids(dbSession, usersMembership.stream().map(SearchUserMembershipDto::getUserUuid).toList())
        .stream().collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
      writeProtobuf(
        SearchUsersResponse.newBuilder()
          .addAllUsers(usersMembership.stream()
            .map(userMembershipDto -> toUser(usersById.get(userMembershipDto.getUserUuid()), userMembershipDto.isSelected()))
            .toList())
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

  private SearchUsersResponse.User toUser(UserDto user, boolean isSelected) {
    SearchUsersResponse.User.Builder builder = SearchUsersResponse.User.newBuilder()
      .setLogin(user.getLogin())
      .setSelected(isSelected);
    ofNullable(user.getName()).ifPresent(builder::setName);
    ofNullable(emptyToNull(user.getEmail())).ifPresent(e -> builder.setAvatar(avatarResolver.create(user)));
    return builder
      .build();
  }

  private static Common.Paging buildPaging(SearchQualityGateUsersRequest wsRequest, int total) {
    return Common.Paging.newBuilder()
      .setPageIndex(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setTotal(total)
      .build();
  }


}
