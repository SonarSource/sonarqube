/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.user.ws;

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
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.api.utils.Paging.forPageIndex;

public class GroupsAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";

  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_SELECTED = "selected";

  private final DbClient dbClient;
  private final UserSession userSession;

  public GroupsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("groups")
      .setDescription("Lists the groups a user belongs to. Requires Administer System permission.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-groups.json"))
      .setSince("5.2");

    action.createParam(PARAM_LOGIN)
      .setDescription("A user login")
      .setExampleValue("admin")
      .setRequired(true);

    action.addSelectionModeParam();

    action.addSearchQuery("users", "group names");

    action.addPagingParams(25);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();

    String login = request.mandatoryParam(PARAM_LOGIN);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    String queryString = request.param(Param.TEXT_QUERY);
    String selected = request.mandatoryParam(Param.SELECTED);

    GroupMembershipQuery query = GroupMembershipQuery.builder()
      .login(login)
      .groupSearch(queryString)
      .membership(getMembership(selected))
      .pageIndex(page)
      .pageSize(pageSize)
      .build();

    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      if (user == null) {
        throw new NotFoundException(String.format("User with login '%s' has not been found", login));
      }
      int total = dbClient.groupMembershipDao().countGroups(dbSession, query, user.getId());
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(total);
      List<GroupMembershipDto> groups = dbClient.groupMembershipDao().selectGroups(dbSession, query, user.getId(), paging.offset(), pageSize);

      JsonWriter json = response.newJsonWriter().beginObject();
      writeGroups(json, groups);
      writePaging(json, paging);
      json.endObject().close();
    }
  }

  private static void writeGroups(JsonWriter json, List<GroupMembershipDto> groups) {
    json.name("groups").beginArray();
    for (GroupMembershipDto group : groups) {
      json.beginObject()
        .prop(FIELD_ID, group.getId().toString())
        .prop(FIELD_NAME, group.getName())
        .prop(FIELD_DESCRIPTION, group.getDescription())
        .prop(FIELD_SELECTED, group.getUserId() != null)
        .endObject();
    }
    json.endArray();
  }

  private static void writePaging(JsonWriter json, Paging paging) {
    json.prop("p", paging.pageIndex())
      .prop("ps", paging.pageSize())
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
