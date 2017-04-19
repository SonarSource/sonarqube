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

import com.google.common.html.HtmlEscapers;
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
import org.sonar.server.component.index.ComponentHit;
import org.sonar.server.component.index.ComponentHitsPerQualifier;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexQuery;
import org.sonar.server.component.index.ComponentIndexResults;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Category;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Project;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Suggestion;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
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
  static final String PARAM_RECENTLY_BROWSED = "recentlyBrowsed";
  static final String SHORT_INPUT_WARNING = "short_input";
  private static final long MAXIMUM_RECENTLY_BROWSED = 50;

  static final int EXTENDED_LIMIT = 20;

  private final ComponentIndex index;
  private final FavoriteFinder favoriteFinder;

  private DbClient dbClient;

  public SuggestionsAction(DbClient dbClient, ComponentIndex index, FavoriteFinder favoriteFinder) {
    this.dbClient = dbClient;
    this.index = index;
    this.favoriteFinder = favoriteFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    NewAction action = context.createAction(ACTION_SUGGESTIONS)
      .setDescription(
        "Internal WS for the top-right search engine. The result will contain component search results, grouped by their qualifiers.<p>"
          + "Each result contains:"
          + "<ul>"
          + "<li>the organization key</li>"
          + "<li>the component key</li>"
          + "<li>the component's name (unescaped)</li>"
          + "<li>optionally a display name, which puts emphasis to matching characters (this text contains html tags and parts of the html-escaped name)</li>"
          + "</ul>")
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
      .setDescription("Category, for which to display " + EXTENDED_LIMIT + " instead of " + ComponentIndexQuery.DEFAULT_LIMIT + " results")
      .setPossibleValues(stream(SuggestionCategory.values()).map(SuggestionCategory::getName).toArray(String[]::new))
      .setSince("6.4");

    action.createParam(PARAM_RECENTLY_BROWSED)
      .setDescription("Comma separated list of component keys, that have recently been browsed by the user. Only the first " + MAXIMUM_RECENTLY_BROWSED
        + " items will be used. Order is not taken into account.")
      .setSince("6.4")
      .setExampleValue("org.sonarsource:sonarqube,some.other:project")
      .setRequired(false);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String query = wsRequest.param(PARAM_QUERY);
    String more = wsRequest.param(PARAM_MORE);
    List<String> recentlyBrowsedParam = wsRequest.paramAsStrings(PARAM_RECENTLY_BROWSED);
    Set<String> recentlyBrowsedKeys;
    if (recentlyBrowsedParam == null) {
      recentlyBrowsedKeys = emptySet();
    } else {
      recentlyBrowsedKeys = recentlyBrowsedParam.stream().limit(MAXIMUM_RECENTLY_BROWSED).collect(Collectors.toSet());
    }
    Set<String> favoriteKeys = favoriteFinder.list().stream().map(ComponentDto::getKey).collect(Collectors.toSet());

    ComponentIndexQuery.Builder queryBuilder = ComponentIndexQuery.builder()
      .setQuery(query)
      .setRecentlyBrowsedKeys(recentlyBrowsedKeys)
      .setFavoriteKeys(favoriteKeys);

    ComponentIndexResults componentsPerQualifiers = getComponentsPerQualifiers(more, queryBuilder);
    String warning = getWarning(query);

    SuggestionsWsResponse searchWsResponse = toResponse(componentsPerQualifiers, recentlyBrowsedKeys, favoriteKeys, warning);
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static String getWarning(String query) {
    List<String> tokens = ComponentTextSearchFeature.split(query).collect(Collectors.toList());
    if (tokens.stream().anyMatch(token -> token.length() < MINIMUM_NGRAM_LENGTH)) {
      return SHORT_INPUT_WARNING;
    }
    return null;
  }

  private ComponentIndexResults getComponentsPerQualifiers(@Nullable String more, ComponentIndexQuery.Builder queryBuilder) {
    List<String> qualifiers;
    if (more == null) {
      qualifiers = stream(SuggestionCategory.values()).map(SuggestionCategory::getQualifier).collect(Collectors.toList());
    } else {
      qualifiers = singletonList(SuggestionCategory.getByName(more).getQualifier());
      queryBuilder.setLimit(EXTENDED_LIMIT);
    }
    queryBuilder.setQualifiers(qualifiers);
    return searchInIndex(queryBuilder.build());
  }

  private ComponentIndexResults searchInIndex(ComponentIndexQuery componentIndexQuery) {
    return index.search(componentIndexQuery);
  }

  private SuggestionsWsResponse toResponse(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteKeys, @Nullable String warning) {
    SuggestionsWsResponse.Builder builder = newBuilder();
    if (!componentsPerQualifiers.isEmpty()) {
      Map<String, OrganizationDto> organizationsByUuids;
      Map<String, ComponentDto> componentsByUuids;
      Map<String, ComponentDto> projectsByUuids;
      try (DbSession dbSession = dbClient.openSession(false)) {
        Set<String> componentUuids = componentsPerQualifiers.getQualifiers()
          .map(ComponentHitsPerQualifier::getHits)
          .flatMap(Collection::stream)
          .map(ComponentHit::getUuid)
          .collect(toSet());
        componentsByUuids = dbClient.componentDao().selectByUuids(dbSession, componentUuids).stream()
          .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
        Set<String> organizationUuids = componentsByUuids.values().stream()
          .map(ComponentDto::getOrganizationUuid)
          .collect(toSet());
        organizationsByUuids = dbClient.organizationDao().selectByUuids(dbSession, organizationUuids).stream()
          .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
        Set<String> projectUuids = componentsByUuids.values().stream()
          .filter(c -> !c.projectUuid().equals(c.uuid()))
          .map(ComponentDto::projectUuid)
          .collect(toSet());
        projectsByUuids = dbClient.componentDao().selectByUuids(dbSession, projectUuids).stream()
          .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
      }
      builder
        .addAllResults(toCategories(componentsPerQualifiers, recentlyBrowsedKeys, favoriteKeys, componentsByUuids, organizationsByUuids, projectsByUuids))
        .addAllOrganizations(toOrganizations(organizationsByUuids))
        .addAllProjects(toProjects(projectsByUuids));
    }
    ofNullable(warning).ifPresent(builder::setWarning);
    return builder.build();
  }

  private static List<Category> toCategories(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteKeys,
    Map<String, ComponentDto> componentsByUuids, Map<String, OrganizationDto> organizationByUuids, Map<String, ComponentDto> projectsByUuids) {
    return componentsPerQualifiers.getQualifiers().map(qualifier -> {

      List<Suggestion> suggestions = qualifier.getHits().stream()
        .map(hit -> toSuggestion(hit, recentlyBrowsedKeys, favoriteKeys, componentsByUuids, organizationByUuids, projectsByUuids))
        .collect(toList());

      return Category.newBuilder()
        .setQ(qualifier.getQualifier())
        .setMore(qualifier.getNumberOfFurtherResults())
        .addAllItems(suggestions)
        .build();
    }).collect(toList());
  }

  private static Suggestion toSuggestion(ComponentHit hit, Set<String> recentlyBrowsedKeys, Set<String> favoriteKeys, Map<String, ComponentDto> componentsByUuids,
    Map<String, OrganizationDto> organizationByUuids, Map<String, ComponentDto> projectsByUuids) {
    ComponentDto result = componentsByUuids.get(hit.getUuid());
    String organizationKey = organizationByUuids.get(result.getOrganizationUuid()).getKey();
    checkState(organizationKey != null, "Organization with uuid '%s' not found", result.getOrganizationUuid());
    String projectKey = ofNullable(result.projectUuid()).map(projectsByUuids::get).map(ComponentDto::getKey).orElse("");
    return Suggestion.newBuilder()
      .setOrganization(organizationKey)
      .setProject(projectKey)
      .setKey(result.getKey())
      .setName(result.longName())
      .setMatch(hit.getHighlightedText().orElse(HtmlEscapers.htmlEscaper().escape(result.longName())))
      .setIsRecentlyBrowsed(recentlyBrowsedKeys.contains(result.getKey()))
      .setIsFavorite(favoriteKeys.contains(result.getKey()))
      .build();
  }

  private static List<Organization> toOrganizations(Map<String, OrganizationDto> organizationByUuids) {
    return organizationByUuids.values().stream()
      .map(o -> Organization.newBuilder()
        .setKey(o.getKey())
        .setName(o.getName())
        .build())
      .collect(Collectors.toList());
  }

  private static List<Project> toProjects(Map<String, ComponentDto> projectsByUuids) {
    return projectsByUuids.values().stream()
      .map(p -> Project.newBuilder()
        .setKey(p.key())
        .setName(p.longName())
        .build())
      .collect(Collectors.toList());
  }
}
