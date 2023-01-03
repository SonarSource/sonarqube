/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.permission.ws;

import java.util.Locale;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.WsSearchGlobalPermissionsResponse;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Permissions.Permission.newBuilder;

public class SearchGlobalPermissionsAction implements PermissionsWsAction {

  public static final String ACTION = "search_global_permissions";
  private static final String PROPERTY_PREFIX = "global_permissions.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final PermissionService permissionService;

  public SearchGlobalPermissionsAction(DbClient dbClient, UserSession userSession, I18n i18n, PermissionService permissionService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.permissionService = permissionService;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setDescription("List global permissions. <br />" +
        "Requires the following permission: 'Administer System'")
      .setResponseExample(getClass().getResource("search_global_permissions-example.json"))
      .setSince("5.2")
      .setDeprecatedSince("6.5")
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkGlobalAdmin(userSession);

      WsSearchGlobalPermissionsResponse response = buildResponse(dbSession);
      writeProtobuf(response, wsRequest, wsResponse);
    }
  }

  private WsSearchGlobalPermissionsResponse buildResponse(DbSession dbSession) {
    WsSearchGlobalPermissionsResponse.Builder response = WsSearchGlobalPermissionsResponse.newBuilder();
    Permission.Builder permission = newBuilder();

    permissionService.getGlobalPermissions().stream()
      .map(GlobalPermission::getKey)
      .forEach(permissionKey -> {
        PermissionQuery query = permissionQuery(permissionKey);
        response.addPermissions(
          permission
            .clear()
            .setKey(permissionKey)
            .setName(i18nName(permissionKey))
            .setDescription(i18nDescriptionMessage(permissionKey))
            .setUsersCount(countUsers(dbSession, query))
            .setGroupsCount(countGroups(dbSession, permissionKey)));
      });

    return response.build();
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey, permissionKey);
  }

  private int countGroups(DbSession dbSession, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).build();
    return dbClient.groupPermissionDao().countGroupsByQuery(dbSession, query);
  }

  private int countUsers(DbSession dbSession, PermissionQuery permissionQuery) {
    return dbClient.userPermissionDao().countUsersByQuery(dbSession, permissionQuery);
  }

  private static PermissionQuery permissionQuery(String permissionKey) {
    return PermissionQuery.builder()
      .setPermission(permissionKey)
      .withAtLeastOnePermission()
      .build();
  }
}
