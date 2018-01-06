/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component.ws;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.TreeWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_COMPONENT;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_TREE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;

public class TreeAction implements ComponentsWsAction {

  private static final int MAX_SIZE = 500;
  private static final int QUERY_MINIMUM_LENGTH = 3;
  private static final String ALL_STRATEGY = "all";
  private static final String CHILDREN_STRATEGY = "children";
  private static final String LEAVES_STRATEGY = "leaves";
  private static final Map<String, Strategy> STRATEGIES = ImmutableMap.of(
    ALL_STRATEGY, LEAVES,
    CHILDREN_STRATEGY, CHILDREN,
    LEAVES_STRATEGY, LEAVES);

  private static final String NAME_SORT = "name";
  private static final String PATH_SORT = "path";
  private static final String QUALIFIER_SORT = "qualifier";
  private static final Set<String> SORTS = ImmutableSortedSet.of(NAME_SORT, PATH_SORT, QUALIFIER_SORT);

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
        "Requires the following permission: 'Browse' on the specified project.<br>" +
        "When limiting search with the %s parameter, directories are not returned.",
        PARAM_COMPONENT_ID, PARAM_COMPONENT, Param.TEXT_QUERY))
      .setSince("5.4")
      .setResponseExample(getClass().getResource("tree-example.json"))
      .setChangelog(
        new Change("6.4", "The field 'id' is deprecated in the response"))
      .setHandler(this)
      .addPagingParams(100, MAX_SIZE);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Base component id. The search is based on this component.")
      .setDeprecatedKey("baseComponentId", "6.4")
      .setDeprecatedSince("6.4")
      .setExampleValue(UUID_EXAMPLE_02);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Base component key. The search is based on this component.")
      .setDeprecatedKey("baseComponentKey", "6.4")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");

    action.createSortParams(SORTS, NAME_SORT, true)
      .setDescription("Comma-separated list of sort fields")
      .setExampleValue(NAME_SORT + ", " + PATH_SORT);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that are exactly the same as the supplied string</li>" +
        "</ul>")
      .setMinimumLength(QUERY_MINIMUM_LENGTH)
      .setExampleValue("FILE_NAM");

    createQualifiersParameter(action, newQualifierParameterContext(i18n, resourceTypes));

    action.createParam(PARAM_STRATEGY)
      .setDescription("Strategy to search for base component descendants:" +
        "<ul>" +
        "<li>children: return the children components of the base component. Grandchildren components are not returned</li>" +
        "<li>all: return all the descendants components of the base component. Grandchildren are returned.</li>" +
        "<li>leaves: return all the descendant components (files, in general) which don't have other children. They are the leaves of the component tree.</li>" +
        "</ul>")
      .setPossibleValues(STRATEGIES.keySet())
      .setDefaultValue(ALL_STRATEGY);
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request request, Response response) throws Exception {
    TreeWsResponse treeWsResponse = doHandle(toTreeWsRequest(request));
    writeProtobuf(treeWsResponse, request, response);
  }

  private TreeWsResponse doHandle(Request treeRequest) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto baseComponent = loadComponent(dbSession, treeRequest);
      checkPermissions(baseComponent);
      OrganizationDto organizationDto = componentFinder.getOrganization(dbSession, baseComponent);

      ComponentTreeQuery query = toComponentTreeQuery(treeRequest, baseComponent);
      List<ComponentDto> components = dbClient.componentDao().selectDescendants(dbSession, query);
      int total = components.size();
      components = sortComponents(components, treeRequest);
      components = paginateComponents(components, treeRequest);

      Map<String, ComponentDto> referenceComponentsByUuid = searchReferenceComponentsByUuid(dbSession, components);

      return buildResponse(baseComponent, organizationDto, components, referenceComponentsByUuid,
        Paging.forPageIndex(treeRequest.getPage()).withPageSize(treeRequest.getPageSize()).andTotal(total));
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentId = request.getBaseComponentId();
    String componentKey = request.getBaseComponentKey();
    String branch = request.getBranch();
    checkArgument(componentId == null || branch == null, "'%s' and '%s' parameters cannot be used at the same time", PARAM_COMPONENT_ID, PARAM_BRANCH);
    return branch == null
      ? componentFinder.getByUuidOrKey(dbSession, componentId, componentKey, COMPONENT_ID_AND_COMPONENT)
      : componentFinder.getByKeyAndBranch(dbSession, componentKey, branch);
  }

  private Map<String, ComponentDto> searchReferenceComponentsByUuid(DbSession dbSession, List<ComponentDto> components) {
    List<String> referenceComponentIds = components.stream()
      .map(ComponentDto::getCopyResourceUuid)
      .filter(Objects::nonNull)
      .collect(toList());
    if (referenceComponentIds.isEmpty()) {
      return emptyMap();
    }

    return dbClient.componentDao().selectByUuids(dbSession, referenceComponentIds).stream()
      .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
  }

  private void checkPermissions(ComponentDto baseComponent) {
    userSession.checkComponentPermission(UserRole.USER, baseComponent);
  }

  private static TreeWsResponse buildResponse(ComponentDto baseComponent, OrganizationDto organizationDto, List<ComponentDto> components,
    Map<String, ComponentDto> referenceComponentsByUuid, Paging paging) {
    TreeWsResponse.Builder response = TreeWsResponse.newBuilder();
    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    response.setBaseComponent(toWsComponent(baseComponent, organizationDto, referenceComponentsByUuid));
    for (ComponentDto dto : components) {
      response.addComponents(toWsComponent(dto, organizationDto, referenceComponentsByUuid));
    }

    return response.build();
  }

  private static Components.Component.Builder toWsComponent(ComponentDto component, OrganizationDto organizationDto,
    Map<String, ComponentDto> referenceComponentsByUuid) {
    Components.Component.Builder wsComponent = componentDtoToWsComponent(component, organizationDto, Optional.empty());

    ComponentDto referenceComponent = referenceComponentsByUuid.get(component.getCopyResourceUuid());
    if (referenceComponent != null) {
      wsComponent.setRefId(referenceComponent.uuid());
      wsComponent.setRefKey(referenceComponent.getDbKey());
    }

    return wsComponent;
  }

  private ComponentTreeQuery toComponentTreeQuery(Request request, ComponentDto baseComponent) {
    List<String> childrenQualifiers = childrenQualifiers(request, baseComponent.qualifier());

    ComponentTreeQuery.Builder query = ComponentTreeQuery.builder()
      .setBaseUuid(baseComponent.uuid())
      .setStrategy(STRATEGIES.get(request.getStrategy()));
    if (request.getQuery() != null) {
      query.setNameOrKeyQuery(request.getQuery());
    }
    if (childrenQualifiers != null) {
      query.setQualifiers(childrenQualifiers);
    }

    return query.build();
  }

  @CheckForNull
  private List<String> childrenQualifiers(Request request, String baseQualifier) {
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

    Sets.SetView<String> qualifiersIntersection = Sets.intersection(new HashSet<>(childrenQualifiers), new HashSet<>(requestQualifiers));

    return new ArrayList<>(qualifiersIntersection);
  }

  private static Request toTreeWsRequest(org.sonar.api.server.ws.Request request) {
    return new Request()
      .setBaseComponentId(request.param(PARAM_COMPONENT_ID))
      .setBaseComponentKey(request.param(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setStrategy(request.mandatoryParam(PARAM_STRATEGY))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setQualifiers(request.paramAsStrings(PARAM_QUALIFIERS))
      .setSort(request.mandatoryParamAsStrings(Param.SORT))
      .setAsc(request.mandatoryParamAsBoolean(Param.ASCENDING))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
  }

  private static List<ComponentDto> paginateComponents(List<ComponentDto> components, Request wsRequest) {
    return components.stream().skip(offset(wsRequest.getPage(), wsRequest.getPageSize()))
      .limit(wsRequest.getPageSize()).collect(toList());
  }

  private static List<ComponentDto> sortComponents(List<ComponentDto> components, Request wsRequest) {
    List<String> sortParameters = wsRequest.getSort();
    if (sortParameters == null || sortParameters.isEmpty()) {
      return components;
    }
    boolean isAscending = wsRequest.getAsc();
    Map<String, Ordering<ComponentDto>> orderingsBySortField = ImmutableMap.<String, Ordering<ComponentDto>>builder()
      .put(NAME_SORT, stringOrdering(isAscending, ComponentDto::name))
      .put(QUALIFIER_SORT, stringOrdering(isAscending, ComponentDto::qualifier))
      .put(PATH_SORT, stringOrdering(isAscending, ComponentDto::path))
      .build();

    String firstSortParameter = sortParameters.get(0);
    Ordering<ComponentDto> primaryOrdering = orderingsBySortField.get(firstSortParameter);
    if (sortParameters.size() > 1) {
      for (int i = 1; i < sortParameters.size(); i++) {
        String secondarySortParameter = sortParameters.get(i);
        Ordering<ComponentDto> secondaryOrdering = orderingsBySortField.get(secondarySortParameter);
        primaryOrdering = primaryOrdering.compound(secondaryOrdering);
      }
    }
    return primaryOrdering.immutableSortedCopy(components);
  }

  private static Ordering<ComponentDto> stringOrdering(boolean isAscending, Function<ComponentDto, String> function) {
    Ordering<String> ordering = Ordering.from(CASE_INSENSITIVE_ORDER);
    if (!isAscending) {
      ordering = ordering.reverse();
    }
    return ordering.nullsLast().onResultOf(function);
  }

  private static class Request {
    private String baseComponentId;
    private String baseComponentKey;
    private String component;
    private String branch;
    private String strategy;
    private List<String> qualifiers;
    private String query;
    private List<String> sort;
    private Boolean asc;
    private Integer page;
    private Integer pageSize;

    /**
     * @deprecated since 6.4, please use {@link #getComponent()} instead
     */
    @Deprecated
    @CheckForNull
    private String getBaseComponentId() {
      return baseComponentId;
    }

    /**
     * @deprecated since 6.4, please use {@link #setComponent(String)} instead
     */
    @Deprecated
    private Request setBaseComponentId(@Nullable String baseComponentId) {
      this.baseComponentId = baseComponentId;
      return this;
    }

    /**
     * @deprecated since 6.4, please use {@link #getComponent()} instead
     */
    @Deprecated
    @CheckForNull
    private String getBaseComponentKey() {
      return baseComponentKey;
    }

    /**
     * @deprecated since 6.4, please use {@link #setComponent(String)} instead
     */
    @Deprecated
    private Request setBaseComponentKey(@Nullable String baseComponentKey) {
      this.baseComponentKey = baseComponentKey;
      return this;
    }

    public Request setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    private String getComponent() {
      return component;
    }

    @CheckForNull
    private String getBranch() {
      return branch;
    }

    private Request setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    @CheckForNull
    private String getStrategy() {
      return strategy;
    }

    private Request setStrategy(@Nullable String strategy) {
      this.strategy = strategy;
      return this;
    }

    @CheckForNull
    private List<String> getQualifiers() {
      return qualifiers;
    }

    private Request setQualifiers(@Nullable List<String> qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    @CheckForNull
    private String getQuery() {
      return query;
    }

    private Request setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    @CheckForNull
    private List<String> getSort() {
      return sort;
    }

    private Request setSort(@Nullable List<String> sort) {
      this.sort = sort;
      return this;
    }

    private Boolean getAsc() {
      return asc;
    }

    private Request setAsc(@Nullable Boolean asc) {
      this.asc = asc;
      return this;
    }

    @CheckForNull
    private Integer getPage() {
      return page;
    }

    private Request setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    @CheckForNull
    private Integer getPageSize() {
      return pageSize;
    }

    private Request setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }
  }

}
