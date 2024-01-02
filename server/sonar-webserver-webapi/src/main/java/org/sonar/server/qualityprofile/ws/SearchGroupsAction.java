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
package org.sonar.server.qualityprofile.ws;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Qualityprofiles;

import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.fromParam;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.ANY;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.IN;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.OUT;
import static org.sonar.db.qualityprofile.SearchQualityProfilePermissionQuery.builder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH_GROUPS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchGroupsAction implements QProfileWsAction {

  private static final Map<WebService.SelectionMode, String> MEMBERSHIP = Map.of(WebService.SelectionMode.SELECTED, IN, DESELECTED, OUT, ALL, ANY);

  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public SearchGroupsAction(DbClient dbClient, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_SEARCH_GROUPS)
      .setDescription("List the groups that are allowed to edit a Quality Profile.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setHandler(this)
      .setInternal(true)
      .addSelectionModeParam()
      .addSearchQuery("sonar", "group names")
      .addPagingParams(25)
      .setResponseExample(getClass().getResource("search_groups-example.json"))
      .setSince("6.6");

    action.createParam(PARAM_QUALITY_PROFILE)
      .setDescription("Quality Profile name")
      .setRequired(true)
      .setExampleValue("Recommended quality profile");

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription("Quality profile language")
      .setRequired(true)
      .setPossibleValues(Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet()));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchQualityProfileUsersRequest wsRequest = buildRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, wsRequest.getQualityProfile(), wsRequest.getLanguage());
      wsSupport.checkCanEdit(dbSession, profile);

      SearchQualityProfilePermissionQuery query = builder()
        .setProfile(profile)
        .setQuery(wsRequest.getQuery())
        .setMembership(MEMBERSHIP.get(fromParam(wsRequest.getSelected())))
        .build();
      int total = dbClient.qProfileEditGroupsDao().countByQuery(dbSession, query);
      List<SearchGroupMembershipDto> groupMemberships = dbClient.qProfileEditGroupsDao().selectByQuery(dbSession, query,
        forPage(wsRequest.getPage()).andSize(wsRequest.getPageSize()));
      Map<String, GroupDto> groupsByUuid = dbClient.groupDao().selectByUuids(dbSession,
        groupMemberships.stream().map(SearchGroupMembershipDto::getGroupUuid).toList())
        .stream()
        .collect(Collectors.toMap(GroupDto::getUuid, Function.identity()));
      writeProtobuf(
        Qualityprofiles.SearchGroupsResponse.newBuilder()
          .addAllGroups(groupMemberships.stream()
            .map(groupsMembership -> toGroup(groupsByUuid.get(groupsMembership.getGroupUuid()), groupsMembership.isSelected()))
            .toList())
          .setPaging(buildPaging(wsRequest, total)).build(),
        request, response);
    }
  }

  private static SearchQualityProfileUsersRequest buildRequest(Request request) {
    return SearchQualityProfileUsersRequest.builder()
      .setQualityProfile(request.mandatoryParam(PARAM_QUALITY_PROFILE))
      .setLanguage(request.mandatoryParam(PARAM_LANGUAGE))
      .setQuery(request.param(TEXT_QUERY))
      .setSelected(request.mandatoryParam(SELECTED))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(request.mandatoryParamAsInt(PAGE_SIZE))
      .build();
  }

  private static Qualityprofiles.SearchGroupsResponse.Group toGroup(GroupDto group, boolean isSelected) {
    Qualityprofiles.SearchGroupsResponse.Group.Builder builder = Qualityprofiles.SearchGroupsResponse.Group.newBuilder()
      .setName(group.getName())
      .setSelected(isSelected);
    ofNullable(group.getDescription()).ifPresent(builder::setDescription);
    return builder.build();
  }

  private static Common.Paging buildPaging(SearchQualityProfileUsersRequest wsRequest, int total) {
    return Common.Paging.newBuilder()
      .setPageIndex(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setTotal(total)
      .build();
  }
}
