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
package org.sonar.server.permission.ws;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.permission.CountPerProjectPermission;
import org.sonar.server.permission.PermissionPrivilegeChecker;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.SearchProjectPermissionsWsResponse;
import org.sonarqube.ws.Permissions.SearchProjectPermissionsWsResponse.Project;

import static java.util.Collections.singletonList;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.permission.ws.ProjectWsRef.newOptionalWsProjectRef;
import static org.sonar.server.permission.ws.SearchProjectPermissionsData.newBuilder;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsParameterBuilder.createRootQualifierParameter;
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
  private final PermissionWsSupport wsSupport;
  private final String[] rootQualifiers;
  private final PermissionService permissionService;

  public SearchProjectPermissionsAction(DbClient dbClient, UserSession userSession, I18n i18n, ResourceTypes resourceTypes,
    PermissionWsSupport wsSupport, PermissionService permissionService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
    this.wsSupport = wsSupport;
    this.rootQualifiers = Collections2.transform(resourceTypes.getRoots(), ResourceType::getQualifier).toArray(new String[resourceTypes.getRoots().size()]);
    this.permissionService = permissionService;
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

  private SearchProjectPermissionsWsResponse doHandle(SearchProjectPermissionsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkAuthorized(dbSession, request);
      RequestValidator.validateQualifier(request.getQualifier(), resourceTypes);
      SearchProjectPermissionsData data = load(dbSession, request);
      return buildResponse(data);
    }
  }

  private static SearchProjectPermissionsRequest toSearchProjectPermissionsWsRequest(Request request) {
    return new SearchProjectPermissionsRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setQualifier(request.param(PARAM_QUALIFIER))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY));
  }

  private void checkAuthorized(DbSession dbSession, SearchProjectPermissionsRequest request) {
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
        .setKey(rootComponent.getDbKey())
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

    for (String permissionKey : permissionService.getAllProjectPermissions()) {
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

  private SearchProjectPermissionsData load(DbSession dbSession, SearchProjectPermissionsRequest request) {
    SearchProjectPermissionsData.Builder data = newBuilder();
    int countRootComponents = countRootComponents(dbSession, request);
    List<ComponentDto> rootComponents = searchRootComponents(dbSession, request, paging(request, countRootComponents));
    List<Long> rootComponentIds = Lists.transform(rootComponents, ComponentDto::getId);

    data.rootComponents(rootComponents)
            .paging(paging(request, countRootComponents))
            .userCountByProjectIdAndPermission(userCountByRootComponentIdAndPermission(dbSession, rootComponentIds))
            .groupCountByProjectIdAndPermission(groupCountByRootComponentIdAndPermission(dbSession, rootComponentIds));

    return data.build();
  }

  private static Paging paging(SearchProjectPermissionsRequest request, int total) {
    return forPageIndex(request.getPage())
            .withPageSize(request.getPageSize())
            .andTotal(total);
  }

  private int countRootComponents(DbSession dbSession, SearchProjectPermissionsRequest request) {
    return dbClient.componentDao().countByQuery(dbSession, toDbQuery(request));
  }

  private List<ComponentDto> searchRootComponents(DbSession dbSession, SearchProjectPermissionsRequest request, Paging paging) {
    com.google.common.base.Optional<ProjectWsRef> project = newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey());

    if (project.isPresent()) {
      return singletonList(wsSupport.getRootComponentOrModule(dbSession, project.get()));
    }

    return dbClient.componentDao().selectByQuery(dbSession, toDbQuery(request), paging.offset(), paging.pageSize());
  }

  private ComponentQuery toDbQuery(SearchProjectPermissionsRequest wsRequest) {
    return ComponentQuery.builder()
            .setQualifiers(qualifiers(wsRequest.getQualifier()))
            .setNameOrKeyQuery(wsRequest.getQuery())
            .build();
  }

  private String[] qualifiers(@Nullable String requestQualifier) {
    return requestQualifier == null
            ? rootQualifiers
            : (new String[] {requestQualifier});
  }

  private Table<Long, String, Integer> userCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.userPermissionDao().countUsersByProjectPermission(dbSession, rootComponentIds).forEach(
            row -> userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount()));

    return userCountByRootComponentIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByRootComponentIdAndPermission(DbSession dbSession, List<Long> rootComponentIds) {
    final Table<Long, String, Integer> userCountByRootComponentIdAndPermission = TreeBasedTable.create();

    dbClient.groupPermissionDao().groupsCountByComponentIdAndPermission(dbSession, rootComponentIds, context -> {
      CountPerProjectPermission row = (CountPerProjectPermission) context.getResultObject();
      userCountByRootComponentIdAndPermission.put(row.getComponentId(), row.getPermission(), row.getCount());
    });

    return userCountByRootComponentIdAndPermission;
  }

  private static class SearchProjectPermissionsRequest {
    private String projectId;
    private String projectKey;
    private String qualifier;
    private Integer page;
    private Integer pageSize;
    private String query;

    @CheckForNull
    public String getProjectId() {
      return projectId;
    }

    public SearchProjectPermissionsRequest setProjectId(@Nullable String projectId) {
      this.projectId = projectId;
      return this;
    }

    @CheckForNull
    public String getProjectKey() {
      return projectKey;
    }

    public SearchProjectPermissionsRequest setProjectKey(@Nullable String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    public SearchProjectPermissionsRequest setPage(int page) {
      this.page = page;
      return this;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public SearchProjectPermissionsRequest setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public SearchProjectPermissionsRequest setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    @CheckForNull
    public String getQualifier() {
      return qualifier;
    }

    public SearchProjectPermissionsRequest setQualifier(@Nullable String qualifier) {
      this.qualifier = qualifier;
      return this;
    }
  }
}
