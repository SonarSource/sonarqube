/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.GroupMembershipDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.SearchGroupsQuery;
import org.sonar.db.user.GroupDto;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Qualityprofiles;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.fromParam;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.ANY;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.IN;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.OUT;
import static org.sonar.db.qualityprofile.SearchGroupsQuery.builder;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH_GROUPS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchGroupsAction implements QProfileWsAction {

  private static final Map<WebService.SelectionMode, String> MEMBERSHIP = ImmutableMap.of(WebService.SelectionMode.SELECTED, IN, DESELECTED, OUT, ALL, ANY);

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
      .setPossibleValues(Arrays.stream(languages.all()).map(Language::getKey).collect(toSet()));

    createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchUsersRequest wsRequest = buildRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganizationByKey(dbSession, wsRequest.getOrganization());
      QProfileDto profile = wsSupport.getProfile(dbSession, organization, wsRequest.getQualityProfile(), wsRequest.getLanguage());
      wsSupport.checkCanEdit(dbSession, organization, profile);

      SearchGroupsQuery query = builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setQuery(wsRequest.getQuery())
        .setMembership(MEMBERSHIP.get(fromParam(wsRequest.getSelected())))
        .build();
      int total = dbClient.qProfileEditGroupsDao().countByQuery(dbSession, query);
      List<GroupMembershipDto> groupMemberships = dbClient.qProfileEditGroupsDao().selectByQuery(dbSession, query,
        forPage(wsRequest.getPage()).andSize(wsRequest.getPageSize()));
      Map<Integer, GroupDto> groupsById = dbClient.groupDao().selectByIds(dbSession,
        groupMemberships.stream().map(GroupMembershipDto::getGroupId).collect(MoreCollectors.toList()))
        .stream()
        .collect(MoreCollectors.uniqueIndex(GroupDto::getId));
      writeProtobuf(
        Qualityprofiles.SearchGroupsResponse.newBuilder()
          .addAllGroups(groupMemberships.stream()
            .map(groupsMembership -> toGroup(groupsById.get(groupsMembership.getGroupId()), groupsMembership.isSelected()))
            .collect(toList()))
          .setPaging(buildPaging(wsRequest, total)).build(),
        request, response);
    }
  }

  private static SearchUsersRequest buildRequest(Request request) {
    return SearchUsersRequest.builder()
      .setOrganization(request.param(PARAM_ORGANIZATION))
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
    setNullable(group.getDescription(), builder::setDescription);
    return builder.build();
  }

  private static Common.Paging buildPaging(SearchUsersRequest wsRequest, int total) {
    return Common.Paging.newBuilder()
      .setPageIndex(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setTotal(total)
      .build();
  }
}
