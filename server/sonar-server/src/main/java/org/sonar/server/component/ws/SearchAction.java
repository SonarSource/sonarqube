/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.client.component.SearchWsRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.Collectors.uniqueIndex;
import static org.sonar.server.util.LanguageParamUtils.getExampleValue;
import static org.sonar.server.util.LanguageParamUtils.getLanguageKeys;
import static org.sonar.server.ws.WsParameterBuilder.createQualifiersParameter;
import static org.sonar.server.ws.WsParameterBuilder.QualifierParameterContext.newQualifierParameterContext;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ResourceTypes resourceTypes;
  private final I18n i18n;
  private final UserSession userSession;
  private final Languages languages;

  public SearchAction(DbClient dbClient, ResourceTypes resourceTypes, I18n i18n, UserSession userSession, Languages languages) {
    this.dbClient = dbClient;
    this.resourceTypes = resourceTypes;
    this.i18n = i18n;
    this.userSession = userSession;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setSince("6.3")
      .setDescription("Search for components")
      .addPagingParams(100)
      .addSearchQuery("sona", "component names", "component keys")
      .setResponseExample(getClass().getResource("search-components-example.json"))
      .setHandler(this);
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

  private SearchWsResponse doHandle(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentQuery query = buildQuery(request);
      Paging paging = buildPaging(dbSession, request, query);
      List<ComponentDto> components = searchComponents(dbSession, query, paging);

      Set<String> organizationUuids = components.stream()
        .map(ComponentDto::getOrganizationUuid)
        .collect(Collectors.toSet());
      Map<String, OrganizationDto> organizationsByUuid = dbClient.organizationDao().selectByUuids(dbSession, organizationUuids)
        .stream()
        .collect(Collectors.uniqueIndex(OrganizationDto::getUuid));
      return buildResponse(components, organizationsByUuid, paging);
    }
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setLanguage(request.param(PARAM_LANGUAGE))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));
  }

  private List<ComponentDto> searchComponents(DbSession dbSession, ComponentQuery query, Paging paging) {
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByQuery(dbSession, query, paging.offset(), paging.pageSize());
    return filterAuthorizedComponents(dbSession, componentDtos);
  }

  private List<ComponentDto> filterAuthorizedComponents(DbSession dbSession, List<ComponentDto> componentDtos) {
    Set<String> projectUuids = componentDtos.stream().map(ComponentDto::projectUuid).collect(Collectors.toSet());
    List<ComponentDto> projects = dbClient.componentDao().selectByUuids(dbSession, projectUuids);
    Map<String, Long> projectIdsByUuids = projects.stream().collect(uniqueIndex(ComponentDto::uuid, ComponentDto::getId));
    Collection<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(dbSession, projectIdsByUuids.values(), userSession.getUserId(), USER);
    return componentDtos.stream()
      .filter(component -> authorizedProjectIds.contains(projectIdsByUuids.get(component.projectUuid())))
      .collect(Collectors.toList());
  }

  private static SearchWsResponse buildResponse(List<ComponentDto> components, Map<String, OrganizationDto> organizationsByUuid, Paging paging) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    components.stream()
      .map(new ComponentDToComponentResponseFunction(organizationsByUuid)::apply)
      .forEach(responseBuilder::addComponents);

    return responseBuilder.build();
  }

  private Paging buildPaging(DbSession dbSession, SearchWsRequest request, ComponentQuery query) {
    int total = dbClient.componentDao().countByQuery(dbSession, query);
    return Paging.forPageIndex(request.getPage())
      .withPageSize(request.getPageSize())
      .andTotal(total);
  }

  private static ComponentQuery buildQuery(SearchWsRequest request) {
    List<String> qualifiers = request.getQualifiers();
    return ComponentQuery.builder()
      .setNameOrKeyQuery(request.getQuery())
      .setLanguage(request.getLanguage())
      .setQualifiers(qualifiers.toArray(new String[qualifiers.size()]))
      .build();
  }

  private static class ComponentDToComponentResponseFunction implements Function<ComponentDto, WsComponents.Component> {
    private final Map<String, OrganizationDto> organizationsByUuid;

    private ComponentDToComponentResponseFunction(Map<String, OrganizationDto> organizationsByUuid) {
      this.organizationsByUuid = organizationsByUuid;
    }

    @Override
    public WsComponents.Component apply(@Nonnull ComponentDto dto) {
      OrganizationDto organization = checkNotNull(
        organizationsByUuid.get(dto.getOrganizationUuid()),
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
}
