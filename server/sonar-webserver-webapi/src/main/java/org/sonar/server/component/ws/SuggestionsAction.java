/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ListMultimap;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.component.ComponentType;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.component.index.ComponentHit;
import org.sonar.server.component.index.ComponentHitsPerQualifier;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexResults;
import org.sonar.server.component.index.SuggestionQuery;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components.SuggestionsWsResponse;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Category;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Suggestion;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.component.index.SuggestionQuery.DEFAULT_LIMIT;
import static org.sonar.server.es.newindex.DefaultIndexSettings.MINIMUM_NGRAM_LENGTH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Components.SuggestionsWsResponse.newBuilder;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {
  static final String PARAM_QUERY = "s";
  static final String PARAM_MORE = "more";
  static final String PARAM_RECENTLY_BROWSED = "recentlyBrowsed";
  static final String SHORT_INPUT_WARNING = "short_input";
  private static final int MAXIMUM_RECENTLY_BROWSED = 50;
  private static final int EXTENDED_LIMIT = 20;

  private final ComponentIndex index;
  private final FavoriteFinder favoriteFinder;
  private final UserSession userSession;
  private final ComponentTypes componentTypes;

  private final DbClient dbClient;

  public SuggestionsAction(DbClient dbClient, ComponentIndex index, FavoriteFinder favoriteFinder, UserSession userSession, ComponentTypes componentTypes) {
    this.dbClient = dbClient;
    this.index = index;
    this.favoriteFinder = favoriteFinder;
    this.userSession = userSession;
    this.componentTypes = componentTypes;
  }

  @Override
  public void define(WebService.NewController context) {
    NewAction action = context.createAction(ACTION_SUGGESTIONS)
      .setDescription(
        "Internal WS for the top-right search engine. The result will contain component search results, grouped by their qualifiers.<p>"
          + "Each result contains:"
          + "<ul>"
          + "<li>the component key</li>"
          + "<li>the component's name (unescaped)</li>"
          + "<li>optionally a display name, which puts emphasis to matching characters (this text contains html tags and parts of the html-escaped name)</li>"
          + "</ul>")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "suggestions-example.json"))
      .setChangelog(
        new Change("10.0", String.format("The use of 'BRC' as value for parameter '%s' is no longer supported", PARAM_MORE)),
        new Change("8.4", String.format("The use of 'DIR', 'FIL','UTS' as values for parameter '%s' is no longer supported", PARAM_MORE)),
        new Change("7.6", String.format("The use of 'BRC' as value for parameter '%s' is deprecated", PARAM_MORE)));

    action.createParam(PARAM_QUERY)
      .setRequired(false)
      .setMinimumLength(2)
      .setDescription("Search query: can contain several search tokens separated by spaces.")
      .setExampleValue("sonar");

    action.createParam(PARAM_MORE)
      .setDescription("Category, for which to display the next " + EXTENDED_LIMIT + " results (skipping the first " + DEFAULT_LIMIT + " results)")
      .setPossibleValues(stream(SuggestionCategory.values()).map(SuggestionCategory::getName).toArray(String[]::new))
      .setSince("6.4");

    action.createParam(PARAM_RECENTLY_BROWSED)
      .setDescription("Comma separated list of component keys, that have recently been browsed by the user. Only the first " + MAXIMUM_RECENTLY_BROWSED
        + " items will be used. Order is not taken into account.")
      .setSince("6.4")
      .setExampleValue("org.sonarsource:sonarqube,some.other:project")
      .setRequired(false)
      .setMaxValuesAllowed(MAXIMUM_RECENTLY_BROWSED);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String query = wsRequest.param(PARAM_QUERY);
    String more = wsRequest.param(PARAM_MORE);
    Set<String> recentlyBrowsedKeys = getRecentlyBrowsedKeys(wsRequest);
    List<String> qualifiers = getQualifiers(more);
    int skip = more == null ? 0 : DEFAULT_LIMIT;
    int limit = more == null ? DEFAULT_LIMIT : EXTENDED_LIMIT;
    SuggestionsWsResponse searchWsResponse = loadSuggestions(query, skip, limit, recentlyBrowsedKeys, qualifiers);
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static Set<String> getRecentlyBrowsedKeys(Request wsRequest) {
    List<String> recentlyBrowsedParam = wsRequest.paramAsStrings(PARAM_RECENTLY_BROWSED);
    if (recentlyBrowsedParam == null) {
      return emptySet();
    }
    return new HashSet<>(recentlyBrowsedParam);
  }

  private SuggestionsWsResponse loadSuggestions(@Nullable String query, int skip, int limit, Set<String> recentlyBrowsedKeys, List<String> qualifiers) {
    if (query == null) {
      return loadSuggestionsWithoutSearch(skip, limit, recentlyBrowsedKeys, qualifiers);
    }
    return loadSuggestionsWithSearch(query, skip, limit, recentlyBrowsedKeys, qualifiers);
  }

  /**
   * we are generating suggestions, by using (1) favorites and (2) recently browsed components (without searching in Elasticsearch)
   */
  private SuggestionsWsResponse loadSuggestionsWithoutSearch(int skip, int limit, Set<String> recentlyBrowsedKeys, List<String> qualifiers) {
    List<EntityDto> favorites = favoriteFinder.list();
    if (favorites.isEmpty() && recentlyBrowsedKeys.isEmpty()) {
      return newBuilder().build();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<EntityDto> entities = new HashSet<>(favorites);
      if (!recentlyBrowsedKeys.isEmpty()) {
        entities.addAll(dbClient.entityDao().selectByKeys(dbSession, recentlyBrowsedKeys));
      }
      List<EntityDto> authorizedEntities = userSession.keepAuthorizedEntities(USER, entities);
      ListMultimap<String, EntityDto> entityPerQualifier = authorizedEntities.stream()
        .collect(MoreCollectors.index(EntityDto::getQualifier));
      if (entityPerQualifier.isEmpty()) {
        return newBuilder().build();
      }

      Set<String> favoriteUuids = favorites.stream().map(EntityDto::getUuid).collect(Collectors.toSet());
      Comparator<EntityDto> favoriteComparator = Comparator.comparing(c -> favoriteUuids.contains(c.getUuid()) ? -1 : +1);
      Comparator<EntityDto> comparator = favoriteComparator.thenComparing(EntityDto::getName);

      ComponentIndexResults componentsPerQualifiers = ComponentIndexResults.newBuilder().setQualifiers(
        qualifiers.stream().map(q -> {
          List<EntityDto> componentsOfThisQualifier = entityPerQualifier.get(q);
          List<ComponentHit> hits = componentsOfThisQualifier
            .stream()
            .sorted(comparator)
            .skip(skip)
            .limit(limit)
            .map(EntityDto::getUuid)
            .map(ComponentHit::new)
            .toList();
          int totalHits = componentsOfThisQualifier.size();
          return new ComponentHitsPerQualifier(q, hits, totalHits);
        })).build();
      return buildResponse(recentlyBrowsedKeys, favoriteUuids, componentsPerQualifiers, authorizedEntities, skip + limit).build();
    }
  }

  private SuggestionsWsResponse loadSuggestionsWithSearch(String query, int skip, int limit, Set<String> recentlyBrowsedKeys, List<String> qualifiers) {
    if (split(query).noneMatch(token -> token.length() >= MINIMUM_NGRAM_LENGTH)) {
      SuggestionsWsResponse.Builder queryBuilder = newBuilder();
      getWarning(query).ifPresent(queryBuilder::setWarning);
      return queryBuilder.build();
    }

    List<EntityDto> favorites = favoriteFinder.list();
    Set<String> favoriteKeys = favorites.stream().map(EntityDto::getKey).collect(Collectors.toSet());
    SuggestionQuery.Builder queryBuilder = SuggestionQuery.builder()
      .setQuery(query)
      .setRecentlyBrowsedKeys(recentlyBrowsedKeys)
      .setFavoriteKeys(favoriteKeys)
      .setQualifiers(qualifiers)
      .setSkip(skip)
      .setLimit(limit);
    ComponentIndexResults componentsPerQualifiers = searchInIndex(queryBuilder.build());
    if (componentsPerQualifiers.isEmpty()) {
      return newBuilder().build();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<String> entityUuids = componentsPerQualifiers.getQualifiers()
        .map(ComponentHitsPerQualifier::getHits)
        .flatMap(Collection::stream)
        .map(ComponentHit::uuid)
        .collect(Collectors.toSet());
      List<EntityDto> entities = dbClient.entityDao().selectByUuids(dbSession, entityUuids);
      Set<String> favoriteUuids = favorites.stream().map(EntityDto::getUuid).collect(Collectors.toSet());
      SuggestionsWsResponse.Builder searchWsResponse = buildResponse(recentlyBrowsedKeys, favoriteUuids, componentsPerQualifiers, entities, skip + limit);
      getWarning(query).ifPresent(searchWsResponse::setWarning);
      return searchWsResponse.build();
    }
  }

  private static Optional<String> getWarning(String query) {
    return split(query)
      .filter(token -> token.length() < MINIMUM_NGRAM_LENGTH)
      .findAny()
      .map(x -> SHORT_INPUT_WARNING);
  }

  private static Stream<String> split(String query) {
    return Arrays.stream(query.split(DefaultIndexSettings.SEARCH_TERM_TOKENIZER_PATTERN));
  }

  private List<String> getQualifiers(@Nullable String more) {
    Set<String> availableQualifiers = componentTypes.getAll().stream()
      .map(ComponentType::getQualifier)
      .collect(Collectors.toSet());
    if (more == null) {
      return stream(SuggestionCategory.values())
        .map(SuggestionCategory::getQualifier)
        .filter(availableQualifiers::contains)
        .toList();
    }

    String qualifier = SuggestionCategory.getByName(more).getQualifier();
    return availableQualifiers.contains(qualifier) ? singletonList(qualifier) : emptyList();
  }

  private static SuggestionsWsResponse.Builder buildResponse(Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids, ComponentIndexResults componentsPerQualifiers,
    List<EntityDto> entities, int coveredItems) {

    Map<String, EntityDto> entitiesByUuids = entities.stream().collect(Collectors.toMap(EntityDto::getUuid, Function.identity()));
    return toResponse(componentsPerQualifiers, recentlyBrowsedKeys, favoriteUuids, entitiesByUuids, coveredItems);
  }

  private ComponentIndexResults searchInIndex(SuggestionQuery suggestionQuery) {
    return index.searchSuggestionsV2(suggestionQuery);
  }

  private static SuggestionsWsResponse.Builder toResponse(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids,
    Map<String, EntityDto> entitiesByUuids, int coveredItems) {
    if (componentsPerQualifiers.isEmpty()) {
      return newBuilder();
    }
    return newBuilder()
      .addAllResults(toCategories(componentsPerQualifiers, recentlyBrowsedKeys, favoriteUuids, entitiesByUuids, coveredItems));
  }

  private static List<Category> toCategories(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids,
    Map<String, EntityDto> entitiesByUuids, int coveredItems) {
    return componentsPerQualifiers.getQualifiers().map(qualifier -> {

      List<Suggestion> suggestions = qualifier.getHits().stream()
        .map(hit -> toSuggestion(hit, recentlyBrowsedKeys, favoriteUuids, entitiesByUuids))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

      return Category.newBuilder()
        .setQ(qualifier.getQualifier())
        .setMore(Math.max(0, qualifier.getTotalHits() - coveredItems))
        .addAllItems(suggestions)
        .build();
    }).toList();
  }

  /**
   * @return null when the component exists in Elasticsearch but not in database. That
   * occurs when failed indexing requests are been recovering.
   */
  private static Optional<Suggestion> toSuggestion(ComponentHit hit, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids, Map<String, EntityDto> entitiesByUuids) {
    return Optional.ofNullable(entitiesByUuids.get(hit.uuid()))
      .map(result -> Suggestion.newBuilder()
        .setKey(result.getKey())
        .setName(result.getName())
        .setMatch(Optional.ofNullable(hit.highlightedText()).orElse(HtmlEscapers.htmlEscaper().escape(result.getName())))
        .setIsRecentlyBrowsed(recentlyBrowsedKeys.contains(result.getKey()))
        .setIsFavorite(favoriteUuids.contains(result.getUuid()))
        .build());
  }
}
