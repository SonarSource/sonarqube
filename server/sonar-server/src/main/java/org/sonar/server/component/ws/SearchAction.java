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
import org.sonarqube.ws.WsComponents.WsSearchResponse;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.natural;
import static java.lang.String.format;
import static org.sonar.server.component.ResourceTypeFunctions.RESOURCE_TYPE_TO_QUALIFIER;
import static org.sonar.server.component.ws.WsComponentsParameters.PARAM_QUALIFIERS;
import static org.sonar.server.permission.ws.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements ComponentsWsAction {
  private static final String QUALIFIER_PROPERTY_PREFIX = "qualifiers.";

  private final DbClient dbClient;
  private final ResourceTypes resourceTypes;
  private final I18n i18n;
  private final UserSession userSession;

  public SearchAction(DbClient dbClient, ResourceTypes resourceTypes, I18n i18n, UserSession userSession) {
    this.dbClient = dbClient;
    this.resourceTypes = resourceTypes;
    this.i18n = i18n;
    this.userSession = userSession;
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
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    List<String> qualifiers = wsRequest.mandatoryParamAsStrings(PARAM_QUALIFIERS);
    validateQualifiers(qualifiers);

    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentQuery query = buildQuery(wsRequest, qualifiers);
      Paging paging = buildPaging(dbSession, wsRequest, query);
      List<ComponentDto> components = searchComponents(dbSession, query, paging);
      WsSearchResponse response = buildResponse(components, paging);
      writeProtobuf(response, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private List<ComponentDto> searchComponents(DbSession dbSession, ComponentQuery query, Paging paging) {
    return dbClient.componentDao().selectByQuery(
      dbSession,
      query,
      paging.offset(),
      paging.pageSize());
  }

  private WsSearchResponse buildResponse(List<ComponentDto> components, Paging paging) {
    WsSearchResponse.Builder responseBuilder = WsSearchResponse.newBuilder();
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

  private Paging buildPaging(DbSession dbSession, Request wsRequest, ComponentQuery query) {
    int total = dbClient.componentDao().countByQuery(dbSession, query);
    return Paging.forPageIndex(wsRequest.mandatoryParamAsInt(Param.PAGE))
      .withPageSize(wsRequest.mandatoryParamAsInt(Param.PAGE_SIZE))
      .andTotal(total);
  }

  private ComponentQuery buildQuery(Request wsRequest, List<String> qualifiers) {
    return new ComponentQuery(
      wsRequest.param(Param.TEXT_QUERY),
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

  private enum ComponentDToComponentResponseFunction implements Function<ComponentDto, WsSearchResponse.Component> {
    INSTANCE;

    @Override
    public WsSearchResponse.Component apply(@Nonnull ComponentDto dto) {
      return WsSearchResponse.Component.newBuilder()
        .setId(dto.uuid())
        .setKey(dto.key())
        .setName(dto.name())
        .setQualifier(dto.qualifier())
        .build();
    }
  }
}
