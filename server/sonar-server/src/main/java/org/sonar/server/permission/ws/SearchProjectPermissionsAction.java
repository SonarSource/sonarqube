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
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.SearchProjectPermissionsResponse;
import org.sonarqube.ws.Permissions.SearchProjectPermissionsResponse.Project;

import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.PermissionWsCommons.createProjectKeyParameter;
import static org.sonar.server.permission.ws.PermissionWsCommons.createProjectUuidParameter;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchProjectPermissionsAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final SearchProjectPermissionsDataLoader dataLoader;

  public SearchProjectPermissionsAction(DbClient dbClient, UserSession userSession, I18n i18n, SearchProjectPermissionsDataLoader dataLoader) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.dataLoader = dataLoader;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_project_permissions")
      .setDescription("List project permissions. A project can be a technical project, a view or a developer.<br />" +
        "Requires 'Administer System' permission or 'Administer' rights on the specified project.")
      .setResponseExample(getClass().getResource("search_project_permissions-example.json"))
      .setSince("5.2")
      .addPagingParams(25)
      .addSearchQuery("sonarq", "project names", "project keys")
      .setHandler(this);

    createProjectUuidParameter(action);
    createProjectKeyParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkRequestAndPermissions(wsRequest);

    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchProjectPermissionsData data = dataLoader.load(wsRequest);
      SearchProjectPermissionsResponse response = buildReponse(data);
      writeProtobuf(response, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void checkRequestAndPermissions(Request wsRequest) {
    String projectUuid = wsRequest.param(PARAM_PROJECT_UUID);
    String projectKey = wsRequest.param(PARAM_PROJECT_KEY);
    boolean isProjectUuidNonNull = projectUuid != null;
    boolean isProjectKeyNonNull = projectKey != null;

    if (isProjectUuidNonNull || isProjectKeyNonNull) {
      checkRequest(projectUuid != null ^ projectKey != null, "Project id or project key can be provided, not both.");
    }
    userSession.checkLoggedIn();

    if (userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)) {
      return;
    }

    if (isProjectUuidNonNull) {
      userSession.checkProjectUuidPermission(UserRole.ADMIN, projectUuid);
      return;
    }

    if (isProjectKeyNonNull) {
      userSession.checkProjectPermission(UserRole.ADMIN, projectKey);
      return;
    }

    userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private SearchProjectPermissionsResponse buildReponse(SearchProjectPermissionsData data) {
    SearchProjectPermissionsResponse.Builder response = SearchProjectPermissionsResponse.newBuilder();
    Permission.Builder permissionResponse = Permission.newBuilder();

    Project.Builder rootComponentBuilder = Project.newBuilder();
    for (ComponentDto rootComponent : data.rootComponents()) {
      rootComponentBuilder
        .clear()
        .setUuid(rootComponent.uuid())
        .setKey(rootComponent.key())
        .setName(rootComponent.name());
      for (String permission : data.permissions(rootComponent.getId())) {
        rootComponentBuilder.addPermissions(
          permissionResponse
            .clear()
            .setKey(permission)
            .setUsersCount(data.userCount(rootComponent.getId(), permission))
            .setGroupsCount(data.groupCount(rootComponent.getId(), permission)));
      }
      response.addProjects(rootComponentBuilder);
    }

    for (String permissionKey : ComponentPermissions.ALL) {
      response.addPermissions(
        permissionResponse
          .clear()
          .setKey(permissionKey)
          .setName(i18nName(permissionKey))
          .setDescription(i18nDescriptionMessage(permissionKey))
        );
    }

    Paging paging = data.paging();
    response.setPaging(
      Common.Paging.newBuilder()
        .setPageIndex(paging.pageIndex())
        .setPageSize(paging.pageSize())
        .setTotal(paging.total())
      );

    return response.build();
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(userSession.locale(), PROPERTY_PREFIX + permissionKey, permissionKey);
  }
}
