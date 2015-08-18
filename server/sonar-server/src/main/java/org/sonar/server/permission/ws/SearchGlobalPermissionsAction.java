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

package org.sonar.server.permission.ws;

import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.SearchGlobalPermissionsResponse;

public class SearchGlobalPermissionsAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "global_permissions.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;

  public SearchGlobalPermissionsAction(DbClient dbClient, UserSession userSession, I18n i18n) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("search_global_permissions")
      .setDescription("List global permissions. <br />" +
        "It requires administration permissions to access.")
      .setResponseExample(getClass().getResource("search_global_permissions-example.json"))
      .setSince("5.2")
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkPermissions();

    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchGlobalPermissionsResponse.Builder response = response(dbSession);
      writeResponse(wsResponse, response);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private SearchGlobalPermissionsResponse.Builder response(DbSession dbSession) {
    SearchGlobalPermissionsResponse.Builder response = SearchGlobalPermissionsResponse.newBuilder();
    SearchGlobalPermissionsResponse.Permission.Builder permission = SearchGlobalPermissionsResponse.Permission.newBuilder();

    for (String permissionKey : GlobalPermissions.ALL) {
      PermissionQuery permissionQuery = permissionQuery(permissionKey);

      response.addGlobalPermissions(
        permission
          .clear()
          .setKey(permissionKey)
          .setName(i18nName(permissionKey))
          .setDescription(i18nDescriptionMessage(permissionKey))
          .setUsersCount(countUsers(dbSession, permissionQuery))
          .setGroupsCount(countGroups(dbSession, permissionKey))
        );
    }

    return response;
  }

  private static void writeResponse(Response response, SearchGlobalPermissionsResponse.Builder searchGlobalPermissionResponse) {
    JsonWriter json = response.newJsonWriter();
    ProtobufJsonFormat.write(searchGlobalPermissionResponse.build(), json);
    json.close();
  }

  private void checkPermissions() {
    userSession
      .checkLoggedIn()
      .checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey, permissionKey);
  }

  private int countGroups(DbSession dbSession, String permissionKey) {
    return dbClient.permissionDao().countGroups(dbSession, permissionKey, null);
  }

  private int countUsers(DbSession dbSession, PermissionQuery permissionQuery) {
    return dbClient.permissionDao().countUsers(dbSession, permissionQuery, null);
  }

  private static PermissionQuery permissionQuery(String permissionKey) {
    return PermissionQuery.builder()
      .permission(permissionKey)
      .membership(GroupMembershipQuery.IN)
      .build();
  }
}
