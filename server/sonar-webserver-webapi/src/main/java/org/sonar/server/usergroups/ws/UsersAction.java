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
package org.sonar.server.usergroups.ws;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.server.ws.Change;
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
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.permission.GroupUuid;
import org.sonar.server.user.UserSession;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;

public class UsersAction implements UserGroupsWsAction {

  private static final String FIELD_SELECTED = "selected";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_LOGIN = "login";
  private static final String FIELD_MANAGED = "managed";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ManagedInstanceService managedInstanceService;
  private final GroupWsSupport support;

  public UsersAction(DbClient dbClient, UserSession userSession, ManagedInstanceService managedInstanceService, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.managedInstanceService = managedInstanceService;
    this.support = support;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("users")
      .setDescription("Search for users with membership information with respect to a group.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setSince("5.2")
      .setDeprecatedSince("10.4")
      .setResponseExample(getClass().getResource("users-example.json"))
      .addSelectionModeParam()
      .addSearchQuery("freddy", "names", "logins")
      .addPagingParams(25)
      .setChangelog(
        new Change("10.4", "Deprecated. Use GET /api/v2/authorizations/group-memberships instead"),
        new Change("10.0", "Field 'managed' added to the payload."),
        new Change("10.0", "Parameter 'id' is removed. Use 'name' instead."),
        new Change("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead."),
        new Change("9.8", "The field 'paging' has been added to the response."),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    String queryString = request.param(Param.TEXT_QUERY);
    String selected = request.mandatoryParam(Param.SELECTED);

    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupUuid group = support.findGroup(dbSession, request);
      userSession.checkPermission(OrganizationPermission.ADMINISTER, group.getOrganizationUuid());

      UserMembershipQuery query = UserMembershipQuery.builder()
        .groupUuid(group.getUuid())
        .organizationUuid(group.getOrganizationUuid())
        .memberSearch(queryString)
        .membership(getMembership(selected))
        .pageIndex(page)
        .pageSize(pageSize)
        .build();
      int total = dbClient.groupMembershipDao().countMembers(dbSession, query);
      List<UserMembershipDto> users = dbClient.groupMembershipDao().selectMembers(dbSession, query);
      Map<String, Boolean> userUuidToIsManaged = managedInstanceService.getUserUuidToManaged(dbSession, getUserUuids(users));
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        writeMembers(json, users, userUuidToIsManaged);
        Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(total);
        writePaging(json, paging);
        json.name("paging").beginObject()
          .prop("pageIndex", page)
          .prop("pageSize", pageSize)
          .prop("total", total)
          .endObject();
        json.endObject();
      }
    }
  }

  private static Set<String> getUserUuids(List<UserMembershipDto> users) {
    return users.stream().map(UserMembershipDto::getUuid).collect(toSet());
  }

  private static void writeMembers(JsonWriter json, List<UserMembershipDto> users, Map<String, Boolean> userUuidToIsManaged) {
    json.name("users").beginArray();
    for (UserMembershipDto user : users) {
      json.beginObject()
        .prop(FIELD_LOGIN, user.getLogin())
        .prop(FIELD_NAME, user.getName())
        .prop(FIELD_SELECTED, user.getGroupUuid() != null)
        .prop(FIELD_MANAGED, TRUE.equals(userUuidToIsManaged.get(user.getUuid())))
        .endObject();
    }
    json.endArray();
  }

  /**
   * @deprecated since 9.8 - replaced by 'paging' object structure.
   */
  @Deprecated(since = "9.8")
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
