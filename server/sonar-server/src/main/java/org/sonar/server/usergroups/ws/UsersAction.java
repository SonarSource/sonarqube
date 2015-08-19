/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserMembershipDto;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.api.utils.Paging.forPageIndex;

public class UsersAction implements UserGroupsWsAction {

  private static final String PARAM_ID = "id";

  private static final String FIELD_SELECTED = "selected";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_LOGIN = "login";

  private final DbClient dbClient;
  private final UserSession userSession;

  public UsersAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("users")
      .setDescription("Search for users with membership information with respect to a group.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-users.json"))
      .setSince("5.2");

    action.createParam(PARAM_ID)
      .setDescription("A group ID")
      .setExampleValue("42")
      .setRequired(true);

    action.addSelectionModeParam();

    action.addSearchQuery("freddy", "names", "logins");

    action.addPagingParams(25);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    Long groupId = request.mandatoryParamAsLong(PARAM_ID);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    String queryString = request.param(Param.TEXT_QUERY);
    String selected = request.mandatoryParam(Param.SELECTED);

    UserMembershipQuery query = UserMembershipQuery.builder()
      .groupId(groupId)
      .memberSearch(queryString)
      .membership(getMembership(selected))
      .pageIndex(page)
      .pageSize(pageSize)
      .build();

    DbSession dbSession = dbClient.openSession(false);
    try {
      GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
      if (group == null) {
        throw new NotFoundException(String.format("Could not find user group with id '%s'", groupId));
      }
      int total = dbClient.groupMembershipDao().countMembers(dbSession, query);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(total);
      List<UserMembershipDto> users = dbClient.groupMembershipDao().selectMembers(dbSession, query, paging.offset(), paging.pageSize());

      JsonWriter json = response.newJsonWriter().beginObject();
      writeMembers(json, users);
      writePaging(json, paging);
      json.endObject().close();
    } finally {
      MyBatis.closeQuietly(dbSession);
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

  private String getMembership(String selected) {
    SelectionMode selectionMode = SelectionMode.fromParam(selected);
    String membership = GroupMembershipQuery.ANY;
    if (SelectionMode.SELECTED == selectionMode) {
      membership = GroupMembershipQuery.IN;
    } else if (SelectionMode.DESELECTED == selectionMode) {
      membership = GroupMembershipQuery.OUT;
    }
    return membership;
  }
}
