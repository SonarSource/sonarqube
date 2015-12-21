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

import com.google.common.base.Function;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Qualifiers;
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
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.client.component.SearchWsRequest;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.natural;
import static java.lang.String.format;
import static org.sonar.server.component.ResourceTypeFunctions.RESOURCE_TYPE_TO_QUALIFIER;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class SearchAction implements ComponentsWsAction {
  private static final String QUALIFIER_PROPERTY_PREFIX = "qualifiers.";

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
    WebService.NewAction action = context.createAction("search")
      .setSince("5.2")
      .setInternal(true)
      .setDescription("Search for components")
      .addPagingParams(100)
      .addSearchQuery("sona", "component names", "component keys")
      .setResponseExample(getClass().getResource("search-components-example.json"))
      .setHandler(this);

    action.createParam(PARAM_QUALIFIERS)
      .setRequired(true)
      .setExampleValue(format("%s,%s", Qualifiers.PROJECT, Qualifiers.MODULE))
      .setDescription("Comma-separated list of component qualifiers. Possible values are " + buildQualifiersDescription());

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
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    List<String> qualifiers = request.getQualifiers();
    validateQualifiers(qualifiers);

    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentQuery query = buildQuery(request, qualifiers);
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

  private SearchWsResponse buildResponse(List<ComponentDto> components, Paging paging) {
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

  private ComponentQuery buildQuery(SearchWsRequest request, List<String> qualifiers) {
    return new ComponentQuery(
      request.getQuery(),
      request.getLanguage(),
      qualifiers.toArray(new String[qualifiers.size()]));
  }

  private void validateQualifiers(List<String> qualifiers) {
    Set<String> possibleQualifiers = allQualifiers();
    for (String qualifier : qualifiers) {
      checkRequest(possibleQualifiers.contains(qualifier),
        format("The '%s' parameter must be one of %s. '%s' was passed.", PARAM_QUALIFIER, possibleQualifiers, qualifier));
    }
  }

  private Set<String> allQualifiers() {
    return from(resourceTypes.getAll())
      .transform(RESOURCE_TYPE_TO_QUALIFIER)
      .toSortedSet(natural());
  }

  private String buildQualifiersDescription() {
    StringBuilder description = new StringBuilder();
    description.append("<ul>");
    String qualifierPattern = "<li>%s - %s</li>";
    for (String qualifier : allQualifiers()) {
      description.append(format(qualifierPattern, qualifier, i18n(qualifier)));
    }
    description.append("</ul>");

    return description.toString();
  }

  private String i18n(String qualifier) {
    return i18n.message(userSession.locale(), QUALIFIER_PROPERTY_PREFIX + qualifier, "");
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
      if (dto.language() != null) {
        builder.setLanguage(dto.language());
      }

      return builder.build();
    }
  }
}
