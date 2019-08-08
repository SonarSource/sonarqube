/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserMembershipDto;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.permission.GroupId;
import org.sonar.server.user.UserSession;

import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;

public class UsersAction implements UserGroupsWsAction {

  private static final String FIELD_SELECTED = "selected";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_LOGIN = "login";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;

  public UsersAction(DbClient dbClient, UserSession userSession, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("users")
      .setDescription("Search for users with membership information with respect to a group.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setSince("5.2")
      .setResponseExample(getClass().getResource("users-example.json"))
      .addSelectionModeParam()
      .addSearchQuery("freddy", "names", "logins")
      .addPagingParams(25);

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    String queryString = request.param(Param.TEXT_QUERY);
    String selected = request.mandatoryParam(Param.SELECTED);

    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupId group = support.findGroup(dbSession, request);
      userSession.checkPermission(OrganizationPermission.ADMINISTER, group.getOrganizationUuid());

      UserMembershipQuery query = UserMembershipQuery.builder()
        .groupId(group.getId())
        .organizationUuid(group.getOrganizationUuid())
        .memberSearch(queryString)
        .membership(getMembership(selected))
        .pageIndex(page)
        .pageSize(pageSize)
        .build();
      int total = dbClient.groupMembershipDao().countMembers(dbSession, query);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(total);
      List<UserMembershipDto> users = dbClient.groupMembershipDao().selectMembers(dbSession, query, paging.offset(), paging.pageSize());

      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        writeMembers(json, users);
        writePaging(json, paging);
        json.endObject();
      }
    }
  }

  private static void writeMembers(JsonWriter json, List<UserMembershipDto> users) {
    json.name("users").beginArray();
    for (UserMembershipDto user : users) {
      json.beginObject()
        .prop(FIELD_LOGIN, user.getLogin())
        .prop(FIELD_NAME, user.getName())
        .prop(FIELD_SELECTED, user.getGroupId() != null)
        .endObject();
    }
    json.endArray();
  }

  private static void writePaging(JsonWriter json, Paging paging) {
    json.prop(Param.PAGE, paging.pageIndex())
      .prop(Param.PAGE_SIZE, paging.pageSize())
      .prop("total", paging.total());
  }

  private static String getMembership(String selected) {
    SelectionMode selectionMode = SelectionMode.fromParam(selected);
    String membership = UserMembershipQuery.ANY;
    if (SelectionMode.SELECTED == selectionMode) {
      membership = UserMembershipQuery.IN;
    } else if (SelectionMode.DESELECTED == selectionMode) {
      membership = UserMembershipQuery.OUT;
    }
    return membership;
  }
}
