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
import org.sonarqube.ws.Permissions;

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
        "Requires 'Administer System' permission.")
      .setResponseExample(getClass().getResource("search_global_permissions-example.json"))
      .setSince("5.2")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession
      .checkLoggedIn()
      .checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    Permissions.SearchGlobalPermissionsResponse.Builder searchGlobalPermissionResponse = Permissions.SearchGlobalPermissionsResponse.newBuilder();
    Permissions.SearchGlobalPermissionsResponse.Permission.Builder permission = Permissions.SearchGlobalPermissionsResponse.Permission.newBuilder();

    DbSession dbSession = dbClient.openSession(false);
    try {
      for (String permissionKey : GlobalPermissions.ALL) {
        PermissionQuery permissionQuery = PermissionQuery.builder()
          .permission(permissionKey)
          .membership(GroupMembershipQuery.IN)
          .build();
        searchGlobalPermissionResponse.addGlobalPermissions(
          permission
            .clear()
            .setKey(permissionKey)
            .setName(i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey, permissionKey))
            .setDescription(i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, permissionKey))
            .setUsersCount(dbClient.permissionDao().countUsers(dbSession, permissionQuery, null))
            .setGroupsCount(dbClient.permissionDao().countGroups(dbSession, permissionKey, null))
          );
      }
    } finally {
      dbClient.closeSession(dbSession);
    }

    JsonWriter json = response.newJsonWriter();
    ProtobufJsonFormat.write(searchGlobalPermissionResponse.build(), json);
    json.close();
  }
}
