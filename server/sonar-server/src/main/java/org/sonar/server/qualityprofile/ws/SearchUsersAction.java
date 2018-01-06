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
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.SearchUsersQuery;
import org.sonar.db.qualityprofile.UserMembershipDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.ws.AvatarResolver;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Qualityprofiles.SearchUsersResponse;

import static com.google.common.base.Strings.emptyToNull;
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
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.db.qualityprofile.SearchUsersQuery.ANY;
import static org.sonar.db.qualityprofile.SearchUsersQuery.IN;
import static org.sonar.db.qualityprofile.SearchUsersQuery.OUT;
import static org.sonar.db.qualityprofile.SearchUsersQuery.builder;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH_USERS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchUsersAction implements QProfileWsAction {

  private static final Map<SelectionMode, String> MEMBERSHIP = ImmutableMap.of(SelectionMode.SELECTED, IN, DESELECTED, OUT, ALL, ANY);

  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;
  private final AvatarResolver avatarResolver;

  public SearchUsersAction(DbClient dbClient, QProfileWsSupport wsSupport, Languages languages, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.languages = languages;
    this.avatarResolver = avatarResolver;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_SEARCH_USERS)
      .setDescription("List the users that are allowed to edit a Quality Profile.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setHandler(this)
      .setInternal(true)
      .addSearchQuery("freddy", "names", "logins")
      .addSelectionModeParam()
      .addPagingParams(25)
      .setResponseExample(getClass().getResource("search_users-example.json"))
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

      SearchUsersQuery query = builder()
        .setOrganization(organization)
        .setProfile(profile)
        .setQuery(wsRequest.getQuery())
        .setMembership(MEMBERSHIP.get(fromParam(wsRequest.getSelected())))
        .build();
      int total = dbClient.qProfileEditUsersDao().countByQuery(dbSession, query);
      List<UserMembershipDto> usersMembership = dbClient.qProfileEditUsersDao().selectByQuery(dbSession, query,
        forPage(wsRequest.getPage()).andSize(wsRequest.getPageSize()));
      Map<Integer, UserDto> usersById = dbClient.userDao().selectByIds(dbSession, usersMembership.stream().map(UserMembershipDto::getUserId).collect(toList()))
        .stream().collect(uniqueIndex(UserDto::getId));
      writeProtobuf(
        SearchUsersResponse.newBuilder()
          .addAllUsers(usersMembership.stream()
            .map(userMembershipDto -> toUser(usersById.get(userMembershipDto.getUserId()), userMembershipDto.isSelected()))
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

  private SearchUsersResponse.User toUser(UserDto user, boolean isSelected) {
    SearchUsersResponse.User.Builder builder = SearchUsersResponse.User.newBuilder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setSelected(isSelected);
    setNullable(emptyToNull(user.getEmail()), e -> builder.setAvatar(avatarResolver.create(user)));
    return builder
      .build();
  }

  private static Common.Paging buildPaging(SearchUsersRequest wsRequest, int total) {
    return Common.Paging.newBuilder()
      .setPageIndex(wsRequest.getPage())
      .setPageSize(wsRequest.getPageSize())
      .setTotal(total)
      .build();
  }

}
