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
package org.sonar.server.project.ws;

import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsProjects.SearchWsResponse;
import org.sonarqube.ws.client.project.SearchWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.project.Visibility.PRIVATE;
import static org.sonar.server.project.Visibility.PUBLIC;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsProjects.SearchWsResponse.Component;
import static org.sonarqube.ws.WsProjects.SearchWsResponse.newBuilder;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.MAX_PAGE_SIZE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class SearchAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final ProjectsWsSupport support;

  public SearchAction(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, ProjectsWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.support = support;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setSince("6.3")
      .setDescription("Search for projects or views.<br>" +
        "Requires 'System Administrator' permission")
      .setInternal(true)
      .addPagingParams(100, MAX_PAGE_SIZE)
      .addSearchQuery("sona", "component names", "component keys")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    action.setChangelog(new Change("6.4", "The 'uuid' field is deprecated in the response"));

    action.createParam(PARAM_QUALIFIERS)
      .setDescription("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers")
      .setPossibleValues(PROJECT, VIEW)
      .setDefaultValue(PROJECT);
    support.addOrganizationParam(action);

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Filter the projects that should be visible to everyone (%s), or only specific user/groups (%s).<br/>" +
        "If no visibility is specified, the default project visibility of the organization will be used.",
        Visibility.PUBLIC.getLabel(), Visibility.PRIVATE.getLabel())
      .setRequired(false)
      .setInternal(true)
      .setSince("6.4")
      .setPossibleValues(Visibility.getLabels());
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return SearchWsRequest.builder()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .build();
  }

  private SearchWsResponse doHandle(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = support.getOrganization(dbSession, ofNullable(request.getOrganization()).orElseGet(defaultOrganizationProvider.get()::getKey));
      userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);

      ComponentQuery query = buildQuery(request);
      Paging paging = buildPaging(dbSession, request, organization, query);
      List<ComponentDto> components = dbClient.componentDao().selectByQuery(dbSession, organization.getUuid(), query, paging.offset(), paging.pageSize());
      return buildResponse(components, organization, paging);
    }
  }

  private static ComponentQuery buildQuery(SearchWsRequest request) {
    List<String> qualifiers = request.getQualifiers();
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setNameOrKeyQuery(request.getQuery())
      .setQualifiers(qualifiers.toArray(new String[qualifiers.size()]));

    setNullable(request.getVisibility(), v -> query.setPrivate(Visibility.isPrivate(v)));

    return query.build();
  }

  private Paging buildPaging(DbSession dbSession, SearchWsRequest request, OrganizationDto organization, ComponentQuery query) {
    int total = dbClient.componentDao().countByQuery(dbSession, organization.getUuid(), query);
    return Paging.forPageIndex(request.getPage())
      .withPageSize(request.getPageSize())
      .andTotal(total);
  }

  private static SearchWsResponse buildResponse(List<ComponentDto> components, OrganizationDto organization, Paging paging) {
    SearchWsResponse.Builder responseBuilder = newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    components.stream()
      .map(dto -> dtoToProject(organization, dto))
      .forEach(responseBuilder::addComponents);
    return responseBuilder.build();
  }

  private static Component dtoToProject(OrganizationDto organization, ComponentDto dto) {
    checkArgument(
      organization.getUuid().equals(dto.getOrganizationUuid()),
      "No Organization found for uuid '%s'",
      dto.getOrganizationUuid());

    Component.Builder builder = Component.newBuilder()
      .setOrganization(organization.getKey())
      .setId(dto.uuid())
      .setKey(dto.key())
      .setName(dto.name())
      .setQualifier(dto.qualifier())
      .setVisibility(dto.isPrivate() ? PRIVATE.getLabel() : PUBLIC.getLabel());
    return builder.build();
  }

}
