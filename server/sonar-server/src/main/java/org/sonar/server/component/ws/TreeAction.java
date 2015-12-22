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

package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.TreeWsResponse;
import org.sonarqube.ws.client.component.TreeWsRequest;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.server.component.ComponentFinder.ParamNames.BASE_COMPONENT_ID_AND_KEY;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_TREE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

public class TreeAction implements ComponentsWsAction {
  private static final int MAX_SIZE = 500;
  private static final String ALL_STRATEGY = "all";
  private static final String CHILDREN_STRATEGY = "children";
  private static final String LEAVES_STRATEGY = "leaves";
  private static final Set<String> STRATEGIES = ImmutableSortedSet.of(ALL_STRATEGY, CHILDREN_STRATEGY, LEAVES_STRATEGY);
  private static final String NAME_SORT = "name";
  private static final Set<String> SORTS = ImmutableSortedSet.of(NAME_SORT, "path", "qualifier");

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ResourceTypes resourceTypes;
  private final UserSession userSession;
  private final I18n i18n;

  public TreeAction(DbClient dbClient, ComponentFinder componentFinder, ResourceTypes resourceTypes, UserSession userSession, I18n i18n) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.resourceTypes = resourceTypes;
    this.userSession = userSession;
    this.i18n = i18n;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_TREE)
      .setDescription(format("Navigate through components based on the chosen strategy. The %s or the %s parameter must be provided.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>",
        PARAM_BASE_COMPONENT_ID, PARAM_BASE_COMPONENT_KEY))
      .setSince("5.4")
      .setResponseExample(getClass().getResource("tree-example.json"))
      .setHandler(this)
      .addSearchQuery("sonar", "component names", "component keys")
      .addMultiSortsParams(newHashSet(SORTS), NAME_SORT, true)
      .addPagingParams(100, MAX_SIZE);

    action.createParam(PARAM_BASE_COMPONENT_ID)
      .setDescription("Base component id. The search is based on this component. It is not included in the response.")
      .setExampleValue(UUID_EXAMPLE_02);

    action.createParam(PARAM_BASE_COMPONENT_KEY)
      .setDescription("Base component key.The search is based on this component. It is not included in the response.")
      .setExampleValue("org.apache.hbas:hbase");

    createQualifiersParameter(action, newQualifierParameterContext(userSession, i18n, resourceTypes));

    action.createParam(PARAM_STRATEGY)
      .setDescription("Strategy to search for base component children:" +
        "<ul>" +
        "<li>children: return the direct children components of the base component. Grandchildren components are not returned</li>" +
        "<li>all: return all the children components of the base component. Grandchildren are returned.</li>" +
        "<li>leaves: return all the children components (files, in general) which don't have other children. They are the leaves of the component tree.</li>" +
        "</ul>")
      .setPossibleValues(STRATEGIES)
      .setDefaultValue(ALL_STRATEGY);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    TreeWsResponse treeWsResponse = doHandle(toTreeWsRequest(request));
    writeProtobuf(treeWsResponse, request, response);
  }

  private TreeWsResponse doHandle(TreeWsRequest treeWsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto baseComponent = componentFinder.getByUuidOrKey(dbSession, treeWsRequest.getBaseComponentId(), treeWsRequest.getBaseComponentKey(), BASE_COMPONENT_ID_AND_KEY);
      checkPermissions(baseComponent);
      SnapshotDto baseSnapshot = dbClient.snapshotDao().selectLastSnapshotByComponentId(dbSession, baseComponent.getId());
      if (baseSnapshot == null) {
        return emptyResponse(treeWsRequest);
      }

      ComponentTreeQuery query = toComponentTreeQuery(treeWsRequest, baseSnapshot);
      List<ComponentDto> components;
      int total;
      switch (treeWsRequest.getStrategy()) {
        case CHILDREN_STRATEGY:
          components = dbClient.componentDao().selectDirectChildren(dbSession, query);
          total = dbClient.componentDao().countDirectChildren(dbSession, query);
          break;
        case LEAVES_STRATEGY:
        case ALL_STRATEGY:
          components = dbClient.componentDao().selectAllChildren(dbSession, query);
          total = dbClient.componentDao().countAllChildren(dbSession, query);
          break;
        default:
          throw new IllegalStateException("Unknown component tree strategy");
      }

      return buildResponse(components,
        Paging.forPageIndex(query.getPage()).withPageSize(query.getPageSize()).andTotal(total));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void checkPermissions(ComponentDto baseComponent) {
    String projectUuid = firstNonNull(baseComponent.projectUuid(), baseComponent.uuid());
    if (!userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN) &&
      !userSession.hasProjectPermissionByUuid(UserRole.ADMIN, projectUuid) &&
      !userSession.hasProjectPermissionByUuid(UserRole.USER, projectUuid)) {
      throw insufficientPrivilegesException();
    }
  }

  private static TreeWsResponse buildResponse(List<ComponentDto> components, Paging paging) {
    TreeWsResponse.Builder response = TreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    for (ComponentDto dto : components) {
      response.addComponents(componentDtoToWsComponent(dto));
    }

    return response.build();
  }

  private static WsComponents.Component.Builder componentDtoToWsComponent(ComponentDto dto) {
    WsComponents.Component.Builder wsComponent = WsComponents.Component.newBuilder()
      .setId(dto.uuid())
      .setKey(dto.key())
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    if (dto.path() != null) {
      wsComponent.setPath(dto.path());
    }
    if (dto.description() != null) {
      wsComponent.setDescription(dto.description());
    }

    return wsComponent;
  }

  private static TreeWsResponse emptyResponse(TreeWsRequest request) {
    TreeWsResponse.Builder response = TreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setTotal(0)
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize());

    return response.build();
  }

  private ComponentTreeQuery toComponentTreeQuery(TreeWsRequest request, SnapshotDto baseSnapshot) {
    List<String> childrenQualifiers = childrenQualifiers(request, baseSnapshot.getQualifier());

    ComponentTreeQuery.Builder query = ComponentTreeQuery.builder()
      .setBaseSnapshot(baseSnapshot)
      .setPage(request.getPage())
      .setPageSize(request.getPageSize())
      .setSortFields(request.getSort())
      .setAsc(request.getAsc());
    if (request.getQuery() != null) {
      query.setNameOrKeyQuery(request.getQuery());
    }
    if (childrenQualifiers != null) {
      query.setQualifiers(childrenQualifiers);
    }

    return query.build();
  }

  @CheckForNull
  private List<String> childrenQualifiers(TreeWsRequest request, String baseQualifier) {
    List<String> requestQualifiers = request.getQualifiers();
    List<String> childrenQualifiers = null;
    if (LEAVES_STRATEGY.equals(request.getStrategy())) {
      childrenQualifiers = resourceTypes.getLeavesQualifiers(baseQualifier);
    }

    if (requestQualifiers == null) {
      return childrenQualifiers;
    }

    if (childrenQualifiers == null) {
      return requestQualifiers;
    }

    // intersection of request and children qualifiers
    childrenQualifiers.retainAll(requestQualifiers);

    return childrenQualifiers;
  }

  private static TreeWsRequest toTreeWsRequest(Request request) {
    TreeWsRequest treeWsRequest = new TreeWsRequest()
      .setBaseComponentId(request.param(PARAM_BASE_COMPONENT_ID))
      .setBaseComponentKey(request.param(PARAM_BASE_COMPONENT_KEY))
      .setStrategy(request.param(PARAM_STRATEGY))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setQualifiers(request.paramAsStrings(PARAM_QUALIFIERS))
      .setSort(request.mandatoryParamAsStrings(Param.SORT))
      .setAsc(request.mandatoryParamAsBoolean(Param.ASCENDING))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
    checkRequest(treeWsRequest.getPageSize() <= MAX_SIZE, "The '%s' parameter must be less thant %d", Param.PAGE_SIZE, MAX_SIZE);

    return treeWsRequest;
  }
}
