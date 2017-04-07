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

import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexQuery;
import org.sonar.server.component.index.ComponentsPerQualifier;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Qualifier;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  static final String PARAM_QUERY = "s";
  static final String PARAM_MORE = "more";

  private static final String[] QUALIFIERS = {
    Qualifiers.VIEW,
    Qualifiers.SUBVIEW,
    Qualifiers.PROJECT,
    Qualifiers.MODULE,
    Qualifiers.FILE,
    Qualifiers.UNIT_TEST_FILE
  };

  static final int DEFAULT_LIMIT = 6;
  static final int EXTENDED_LIMIT = 20;

  private final ComponentIndex index;

  private DbClient dbClient;

  public SuggestionsAction(DbClient dbClient, ComponentIndex index) {
    this.dbClient = dbClient;
    this.index = index;
  }

  @Override
  public void define(WebService.NewController context) {
    NewAction action = context.createAction(ACTION_SUGGESTIONS)
      .setDescription("Internal WS for the top-right search engine")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-suggestions.json"));

    action.createParam(PARAM_QUERY)
      .setRequired(true)
      .setDescription("Substring of project key (minimum 2 characters)")
      .setExampleValue("sonar");

    action.createParam(PARAM_MORE)
      .setDescription("Qualifier, for which to display " + EXTENDED_LIMIT + " instead of " + DEFAULT_LIMIT + " results")
      .setPossibleValues(QUALIFIERS)
      .setSince("6.4");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String query = wsRequest.param(PARAM_QUERY);
    String more = wsRequest.param(PARAM_MORE);

    ComponentIndexQuery.Builder queryBuilder = ComponentIndexQuery.builder().setQuery(query);
    if (more == null) {
      queryBuilder.setQualifiers(asList(QUALIFIERS))
        .setLimit(DEFAULT_LIMIT);
    } else {
      queryBuilder.setQualifiers(Collections.singletonList(more))
        .setLimit(EXTENDED_LIMIT);
    }
    List<ComponentsPerQualifier> componentsPerQualifiers = searchInIndex(queryBuilder.build());
    SuggestionsWsResponse searchWsResponse = toResponse(componentsPerQualifiers);
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private List<ComponentsPerQualifier> searchInIndex(ComponentIndexQuery componentIndexQuery) {
    return index.search(componentIndexQuery);
  }

  private SuggestionsWsResponse toResponse(List<ComponentsPerQualifier> componentsPerQualifiers) {
    return SuggestionsWsResponse.newBuilder()
      .addAllResults(getResultsOfAllQualifiers(componentsPerQualifiers))
      .build();
  }

  private List<Qualifier> getResultsOfAllQualifiers(List<ComponentsPerQualifier> componentsPerQualifiers) {
    if (componentsPerQualifiers.isEmpty()) {
      return Collections.emptyList();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      return componentsPerQualifiers.stream().map(qualifier -> {

        List<String> uuids = qualifier.getComponentUuids();
        List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, uuids);
        List<ComponentDto> sortedComponentDtos = Ordering.explicit(uuids)
          .onResultOf(ComponentDto::uuid)
          .immutableSortedCopy(componentDtos);

        Map<String, String> organizationKeyByUuids = getOrganizationKeys(dbSession, componentDtos);

        List<Component> results = sortedComponentDtos
          .stream()
          .map(dto -> dtoToComponent(dto, organizationKeyByUuids))
          .collect(toList());

        return Qualifier.newBuilder()
          .setQ(qualifier.getQualifier())
          .setMore(qualifier.getNumberOfFurtherResults())
          .addAllItems(results)
          .build();
      }).collect(toList());
    }

  }

  private Map<String, String> getOrganizationKeys(DbSession dbSession, List<ComponentDto> componentDtos) {
    return dbClient.organizationDao().selectByUuids(
      dbSession,
      componentDtos.stream().map(ComponentDto::getOrganizationUuid).collect(MoreCollectors.toSet()))
      .stream()
      .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid, OrganizationDto::getKey));
  }

  private static Component dtoToComponent(ComponentDto result, Map<String, String> organizationKeysByUuid) {
    String organizationKey = organizationKeysByUuid.get(result.getOrganizationUuid());
    checkState(organizationKey != null, "Organization with uuid '%s' not found", result.getOrganizationUuid());
    return Component.newBuilder()
      .setOrganization(organizationKey)
      .setKey(result.getKey())
      .setName(result.longName())
      .build();
  }

}
