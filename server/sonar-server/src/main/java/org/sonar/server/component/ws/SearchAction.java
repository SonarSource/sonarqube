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
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.LanguageParamUtils;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.client.component.SearchWsRequest;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.core.util.Protobuf.setNullable;
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
      .setSince("5.2")
      .setInternal(true)
      .setDescription("Search for components")
      .addPagingParams(100)
      .addSearchQuery("sona", "component names", "component keys")
      .setResponseExample(getClass().getResource("search-components-example.json"))
      .setHandler(this);

    createQualifiersParameter(action, newQualifierParameterContext(i18n, resourceTypes))
      .setRequired(true);

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription("Language key. If provided, only components for the given language are returned.")
      .setExampleValue(LanguageParamUtils.getExampleValue(languages))
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setSince("5.4");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private SearchWsResponse doHandle(SearchWsRequest request) {
    userSession.checkLoggedIn().checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentQuery query = buildQuery(request);
      Paging paging = buildPaging(dbSession, request, query);
      List<ComponentDto> components = searchComponents(dbSession, query, paging);
      return buildResponse(components, paging);
    } finally {
      dbClient.closeSession(dbSession);
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
    return dbClient.componentDao().selectByQuery(
      dbSession,
      query,
      paging.offset(),
      paging.pageSize());
  }

  private static SearchWsResponse buildResponse(List<ComponentDto> components, Paging paging) {
    WsComponents.SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    responseBuilder.addAllComponents(
      from(components)
        .transform(ComponentDToComponentResponseFunction.INSTANCE));

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

  private enum ComponentDToComponentResponseFunction implements Function<ComponentDto, WsComponents.Component> {
    INSTANCE;

    @Override
    public WsComponents.Component apply(@Nonnull ComponentDto dto) {
      WsComponents.Component.Builder builder = WsComponents.Component.newBuilder()
        .setId(dto.uuid())
        .setKey(dto.key())
        .setName(dto.name())
        .setQualifier(dto.qualifier());
      setNullable(dto.language(), builder::setLanguage);
      return builder.build();
    }
  }
}
