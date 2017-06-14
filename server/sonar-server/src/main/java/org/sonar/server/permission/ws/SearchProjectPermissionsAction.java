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
package org.sonar.server.permission.ws;

import java.util.Locale;
import java.util.Optional;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.permission.PermissionPrivilegeChecker;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsPermissions.Permission;
import org.sonarqube.ws.WsPermissions.SearchProjectPermissionsWsResponse;
import org.sonarqube.ws.WsPermissions.SearchProjectPermissionsWsResponse.Project;
import org.sonarqube.ws.client.permission.SearchProjectPermissionsWsRequest;

import static org.sonar.server.permission.ws.PermissionRequestValidator.validateQualifier;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameters;
import static org.sonar.server.permission.ws.ProjectWsRef.newOptionalWsProjectRef;
import static org.sonar.server.ws.WsParameterBuilder.createRootQualifierParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class SearchProjectPermissionsAction implements PermissionsWsAction {
  private static final String PROPERTY_PREFIX = "projects_role.";
  private static final String DESCRIPTION_SUFFIX = ".desc";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final I18n i18n;
  private final ResourceTypes resourceTypes;
  private final SearchProjectPermissionsDataLoader dataLoader;
  private final PermissionWsSupport wsSupport;

  public SearchProjectPermissionsAction(DbClient dbClient, UserSession userSession, I18n i18n, ResourceTypes resourceTypes,
    SearchProjectPermissionsDataLoader dataLoader, PermissionWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
    this.dataLoader = dataLoader;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_project_permissions")
      .setDescription("List project permissions. A project can be a technical project, a view or a developer.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setResponseExample(getClass().getResource("search_project_permissions-example.json"))
      .setSince("5.2")
      .setDeprecatedSince("6.5")
      .addPagingParams(25)
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>project names that contain the supplied string</li>" +
        "<li>project keys that are exactly the same as the supplied string</li>" +
        "</ul>")
      .setExampleValue("apac");
    createProjectParameters(action);
    createRootQualifierParameter(action, newQualifierParameterContext(i18n, resourceTypes))
      .setSince("5.3");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SearchProjectPermissionsWsResponse searchProjectPermissionsWsResponse = doHandle(toSearchProjectPermissionsWsRequest(wsRequest));
    writeProtobuf(searchProjectPermissionsWsResponse, wsRequest, wsResponse);
  }

  private SearchProjectPermissionsWsResponse doHandle(SearchProjectPermissionsWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkAuthorized(dbSession, request);
      validateQualifier(request.getQualifier(), resourceTypes);
      SearchProjectPermissionsData data = dataLoader.load(dbSession, request);
      return buildResponse(data);
    }
  }

  private static SearchProjectPermissionsWsRequest toSearchProjectPermissionsWsRequest(Request request) {
    return new SearchProjectPermissionsWsRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setQualifier(request.param(PARAM_QUALIFIER))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY));
  }

  private void checkAuthorized(DbSession dbSession, SearchProjectPermissionsWsRequest request) {
    com.google.common.base.Optional<ProjectWsRef> projectRef = newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey());
    if (projectRef.isPresent()) {
      ComponentDto project = wsSupport.getRootComponentOrModule(dbSession, projectRef.get());
      PermissionPrivilegeChecker.checkProjectAdmin(userSession, project.getOrganizationUuid(), Optional.of(new ProjectId(project)));
    } else {
      userSession.checkLoggedIn().checkIsSystemAdministrator();
    }
  }

  private SearchProjectPermissionsWsResponse buildResponse(SearchProjectPermissionsData data) {
    SearchProjectPermissionsWsResponse.Builder response = SearchProjectPermissionsWsResponse.newBuilder();
    Permission.Builder permissionResponse = Permission.newBuilder();

    Project.Builder rootComponentBuilder = Project.newBuilder();
    for (ComponentDto rootComponent : data.rootComponents()) {
      rootComponentBuilder
        .clear()
        .setId(rootComponent.uuid())
        .setKey(rootComponent.key())
        .setQualifier(rootComponent.qualifier())
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

    for (String permissionKey : ProjectPermissions.ALL) {
      response.addPermissions(
        permissionResponse
          .clear()
          .setKey(permissionKey)
          .setName(i18nName(permissionKey))
          .setDescription(i18nDescriptionMessage(permissionKey)));
    }

    Paging paging = data.paging();
    response.setPaging(
      Common.Paging.newBuilder()
        .setPageIndex(paging.pageIndex())
        .setPageSize(paging.pageSize())
        .setTotal(paging.total()));

    return response.build();
  }

  private String i18nDescriptionMessage(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey + DESCRIPTION_SUFFIX, "");
  }

  private String i18nName(String permissionKey) {
    return i18n.message(Locale.ENGLISH, PROPERTY_PREFIX + permissionKey, permissionKey);
  }
}
