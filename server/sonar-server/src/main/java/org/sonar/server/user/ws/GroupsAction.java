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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupMembershipDto;
import org.sonar.core.user.GroupMembershipQuery;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.util.List;

public class GroupsAction implements BaseUsersWsAction {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_SELECTED = "selected";

  private static final String SELECTION_ALL = "all";
  private static final String SELECTION_SELECTED = "selected";
  private static final String SELECTION_DESELECTED = "deselected";

  private final DbClient dbClient;

  public GroupsAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("groups")
      .setDescription("List the groups a user belongs to.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-groups.json"))
      .setSince("5.2");

    action.createParam(PARAM_LOGIN)
      .setDescription("A user login")
      .setExampleValue("admin")
      .setRequired(true);

    action.createParam(PARAM_SELECTED)
      .setDescription("If specified, only show groups the user is member of (selected) or not (deselected).")
      .setPossibleValues(SELECTION_SELECTED, SELECTION_DESELECTED, SELECTION_ALL)
      .setDefaultValue(SELECTION_ALL);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("If specified, only show groups whose name contains the query.")
      .setExampleValue("user");

    action.addPagingParams(25);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String login = request.mandatoryParam(PARAM_LOGIN);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    String queryString = request.param(Param.TEXT_QUERY);
    String selected = request.param(PARAM_SELECTED);

    GroupMembershipQuery query = GroupMembershipQuery.builder()
      .login(login)
      .groupSearch(queryString)
      .membership(getMembership(selected))
      .pageIndex(page)
      .pageSize(pageSize)
      .build();

    DbSession session = dbClient.openSession(false);
    try {
      UserDto user = dbClient.userDao().selectByLogin(session, login);
      int total = dbClient.groupMembershipDao().countGroups(session, query, user.getId());
      Paging paging = Paging.create(pageSize, page, total);
      List<GroupMembershipDto> groups = dbClient.groupMembershipDao().selectGroups(session, query, user.getId(), paging.offset(), pageSize);

      JsonWriter json = response.newJsonWriter().beginObject();
      writeGroups(json, groups);
      writePaging(json, paging);
      json.endObject().close();
    } finally {
      session.close();
    }
  }

  private void writeGroups(JsonWriter json, List<GroupMembershipDto> groups) {
    json.name("groups").beginArray();
    for (GroupMembershipDto group : groups) {
      json.beginObject()
        .prop("name", group.getName())
        .prop("description", group.getDescription())
        .prop("selected", group.getUserId() != null)
        .endObject();
    }
    json.endArray();
  }

  private void writePaging(JsonWriter json, Paging paging) {
    json.prop("p", paging.pageIndex())
      .prop("ps", paging.pageSize())
      .prop("total", paging.total());
  }

  private String getMembership(@Nullable String selected) {
    String membership = GroupMembershipQuery.ANY;
    if (SELECTION_SELECTED.equals(selected)) {
      membership = GroupMembershipQuery.IN;
    } else if (SELECTION_DESELECTED.equals(selected)) {
      membership = GroupMembershipQuery.OUT;
    }
    return membership;
  }
}
