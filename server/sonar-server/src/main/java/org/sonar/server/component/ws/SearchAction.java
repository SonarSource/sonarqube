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
package org.sonar.server.component.ws;

import java.util.List;
import java.util.Optional;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.client.component.SearchWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.util.LanguageParamUtils.getExampleValue;
import static org.sonar.server.util.LanguageParamUtils.getLanguageKeys;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ResourceTypes resourceTypes;
  private final I18n i18n;
  private final UserSession userSession;
  private final Languages languages;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public SearchAction(DbClient dbClient, ResourceTypes resourceTypes, I18n i18n, UserSession userSession,
    Languages languages, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.resourceTypes = resourceTypes;
    this.i18n = i18n;
    this.userSession = userSession;
    this.languages = languages;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setSince("6.3")
      .setDescription("Search for components.<br>" +
        "Returns the components with the 'Browse' permission.")
      .addPagingParams(100)
      .addSearchQuery("sona", "component names", "component keys")
      .setResponseExample(getClass().getResource("search-components-example.json"))
      .setHandler(this);
    action
      .createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.3");
    action
      .createParam(PARAM_LANGUAGE)
      .setDescription("Language key. If provided, only components for the given language are returned.")
      .setExampleValue(getExampleValue(languages))
      .setPossibleValues(getLanguageKeys(languages));
    createQualifiersParameter(action, newQualifierParameterContext(i18n, resourceTypes))
      .setRequired(true);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setLanguage(request.param(PARAM_LANGUAGE))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
  }

  private SearchWsResponse doHandle(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentQuery query = buildQuery(request);
      OrganizationDto organization = getOrganization(dbSession, request);
      Paging paging = buildPaging(dbSession, request, organization, query);
      List<ComponentDto> components = searchComponents(dbSession, organization, query, paging);

      return buildResponse(components, organization, paging);
    }
  }

  private static ComponentQuery buildQuery(SearchWsRequest request) {
    List<String> qualifiers = request.getQualifiers();
    return ComponentQuery.builder()
      .setNameOrKeyQuery(request.getQuery())
      .setLanguage(request.getLanguage())
      .setQualifiers(qualifiers.toArray(new String[qualifiers.size()]))
      .build();
  }

  private OrganizationDto getOrganization(DbSession dbSession, SearchWsRequest request) {
    String organizationKey = Optional.ofNullable(request.getOrganization())
      .orElseGet(defaultOrganizationProvider.get()::getKey);
    return WsUtils.checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationKey),
      "No organizationDto with key '%s'", organizationKey);
  }

  private Paging buildPaging(DbSession dbSession, SearchWsRequest request, OrganizationDto organization, ComponentQuery query) {
    int total = dbClient.componentDao().countByQuery(dbSession, organization.getUuid(), query);
    return Paging.forPageIndex(request.getPage())
      .withPageSize(request.getPageSize())
      .andTotal(total);
  }

  private List<ComponentDto> searchComponents(DbSession dbSession, OrganizationDto organization, ComponentQuery query, Paging paging) {
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByQuery(dbSession, organization.getUuid(), query, paging.offset(), paging.pageSize());
    return userSession.keepAuthorizedComponents(UserRole.USER, componentDtos);
  }

  private static SearchWsResponse buildResponse(List<ComponentDto> components, OrganizationDto organization, Paging paging) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    components.stream()
      .map(dto -> dtoToComponent(organization, dto))
      .forEach(responseBuilder::addComponents);

    return responseBuilder.build();
  }

  private static WsComponents.Component dtoToComponent(OrganizationDto organization, ComponentDto dto) {
    checkArgument(
      organization.getUuid().equals(dto.getOrganizationUuid()),
      "No Organization found for uuid '%s'",
      dto.getOrganizationUuid());

    WsComponents.Component.Builder builder = WsComponents.Component.newBuilder()
      .setOrganization(organization.getKey())
      .setId(dto.uuid())
      .setKey(dto.key())
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    setNullable(dto.language(), builder::setLanguage);
    return builder.build();
  }

}
