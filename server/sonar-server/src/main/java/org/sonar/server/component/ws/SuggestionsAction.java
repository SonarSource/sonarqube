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

import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Category;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.es.DefaultIndexSettings.MINIMUM_NGRAM_LENGTH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Organization;
import static org.sonarqube.ws.WsComponents.SuggestionsWsResponse.newBuilder;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  static final String PARAM_QUERY = "s";
  static final String PARAM_MORE = "more";
  static final String SHORT_INPUT_WARNING = "short_input";

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
      .setDescription("Search query with a minimum of two characters. Can contain several search tokens, separated by spaces. " +
        "Search tokens with only one character will be ignored.")
      .setExampleValue("sonar");

    action.createParam(PARAM_MORE)
      .setDescription("Category, for which to display " + EXTENDED_LIMIT + " instead of " + DEFAULT_LIMIT + " results")
      .setPossibleValues(stream(SuggestionCategory.values()).map(SuggestionCategory::getName).toArray(String[]::new))
      .setSince("6.4");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String query = wsRequest.param(PARAM_QUERY);
    String more = wsRequest.param(PARAM_MORE);

    ComponentIndexQuery.Builder queryBuilder = ComponentIndexQuery.builder().setQuery(query);

    List<ComponentsPerQualifier> componentsPerQualifiers = getComponentsPerQualifiers(more, queryBuilder);
    String warning = getWarning(query);

    SuggestionsWsResponse searchWsResponse = toResponse(componentsPerQualifiers, warning);
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static String getWarning(String query) {
    List<String> tokens = ComponentTextSearchFeature.split(query).collect(Collectors.toList());
    if (tokens.stream().anyMatch(token -> token.length() < MINIMUM_NGRAM_LENGTH)) {
      return SHORT_INPUT_WARNING;
    }
    return null;
  }

  private List<ComponentsPerQualifier> getComponentsPerQualifiers(String more, ComponentIndexQuery.Builder queryBuilder) {
    List<ComponentsPerQualifier> componentsPerQualifiers;
    if (more == null) {
      queryBuilder.setQualifiers(stream(SuggestionCategory.values()).map(SuggestionCategory::getQualifier).collect(Collectors.toList()))
        .setLimit(DEFAULT_LIMIT);
    } else {
      queryBuilder.setQualifiers(singletonList(SuggestionCategory.getByName(more).getQualifier()))
        .setLimit(EXTENDED_LIMIT);
    }
    componentsPerQualifiers = searchInIndex(queryBuilder.build());
    return componentsPerQualifiers;
  }

  private List<ComponentsPerQualifier> searchInIndex(ComponentIndexQuery componentIndexQuery) {
    return index.search(componentIndexQuery);
  }

  private SuggestionsWsResponse toResponse(List<ComponentsPerQualifier> componentsPerQualifiers, @Nullable String warning) {
    SuggestionsWsResponse.Builder builder = newBuilder();
    if (!componentsPerQualifiers.isEmpty()) {
      Map<String, OrganizationDto> organizationsByUuids;
      Map<String, ComponentDto> componentsByUuids;
      try (DbSession dbSession = dbClient.openSession(false)) {
        Set<String> componentUuids = componentsPerQualifiers.stream()
          .map(ComponentsPerQualifier::getComponentUuids)
          .flatMap(Collection::stream)
          .collect(toSet());
        componentsByUuids = dbClient.componentDao().selectByUuids(dbSession, componentUuids).stream()
          .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
        Set<String> organizationUuids = componentsByUuids.values().stream()
          .map(ComponentDto::getOrganizationUuid)
          .collect(toSet());
        organizationsByUuids = dbClient.organizationDao().selectByUuids(dbSession, organizationUuids).stream()
          .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
      }
      builder
        .addAllSuggestions(toCategoryResponses(componentsPerQualifiers, componentsByUuids, organizationsByUuids))
        .addAllOrganizations(toOrganizationResponses(organizationsByUuids));
    }
    ofNullable(warning).ifPresent(builder::setWarning);
    return builder.build();
  }

  private static List<Category> toCategoryResponses(List<ComponentsPerQualifier> componentsPerQualifiers, Map<String, ComponentDto> componentsByUuids,
    Map<String, OrganizationDto> organizationByUuids) {
    return componentsPerQualifiers.stream().map(qualifier -> {

      List<Component> results = qualifier.getComponentUuids().stream()
        .map(componentsByUuids::get)
        .map(dto -> dtoToComponent(dto, organizationByUuids))
        .collect(toList());

      return Category.newBuilder()
        .setCategory(qualifier.getQualifier())
        .setMore(qualifier.getNumberOfFurtherResults())
        .addAllItems(results)
        .build();
    }).collect(toList());
  }

  private static Component dtoToComponent(ComponentDto result, Map<String, OrganizationDto> organizationByUuid) {
    String organizationKey = organizationByUuid.get(result.getOrganizationUuid()).getKey();
    checkState(organizationKey != null, "Organization with uuid '%s' not found", result.getOrganizationUuid());
    return Component.newBuilder()
      .setOrganization(organizationKey)
      .setKey(result.getKey())
      .setName(result.longName())
      .build();
  }

  private static List<Organization> toOrganizationResponses(Map<String, OrganizationDto> organizationByUuids) {
    return organizationByUuids.values().stream()
      .map(o -> Organization.newBuilder()
        .setKey(o.getKey())
        .setName(o.getName())
        .build())
      .collect(Collectors.toList());
  }
}
