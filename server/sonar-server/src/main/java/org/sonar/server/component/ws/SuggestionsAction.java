/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
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
import org.sonar.server.component.index.ComponentIndexResults;
import org.sonar.server.component.index.SuggestionQuery;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Components.SuggestionsWsResponse;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Category;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Project;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Suggestion;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.component.index.SuggestionQuery.DEFAULT_LIMIT;
import static org.sonar.server.es.newindex.DefaultIndexSettings.MINIMUM_NGRAM_LENGTH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Common.Organization;
import static org.sonarqube.ws.Components.SuggestionsWsResponse.newBuilder;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  static final String PARAM_QUERY = "s";
  static final String PARAM_MORE = "more";
  static final String PARAM_RECENTLY_BROWSED = "recentlyBrowsed";
  static final String SHORT_INPUT_WARNING = "short_input";
  private static final int MAXIMUM_RECENTLY_BROWSED = 50;

  private static final int EXTENDED_LIMIT = 20;
  private static final Set<String> QUALIFIERS_FOR_WHICH_TO_RETURN_PROJECT = Stream.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE).collect(Collectors.toSet());

  private final ComponentIndex index;
  private final FavoriteFinder favoriteFinder;
  private final UserSession userSession;
  private final ResourceTypes resourceTypes;

  private DbClient dbClient;

  public SuggestionsAction(DbClient dbClient, ComponentIndex index, FavoriteFinder favoriteFinder, UserSession userSession, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.index = index;
    this.favoriteFinder = favoriteFinder;
    this.userSession = userSession;
    this.resourceTypes = resourceTypes;
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
      .setResponseExample(Resources.getResource(this.getClass(), "suggestions-example.json"))
      .setChangelog(
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
   * we are generating suggestions, by using (1) favorites and (2) recently browsed components (without searchin in Elasticsearch)
   */
  private SuggestionsWsResponse loadSuggestionsWithoutSearch(int skip, int limit, Set<String> recentlyBrowsedKeys, List<String> qualifiers) {
    List<ComponentDto> favoriteDtos = favoriteFinder.list();
    if (favoriteDtos.isEmpty() && recentlyBrowsedKeys.isEmpty()) {
      return newBuilder().build();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<ComponentDto> componentDtos = new HashSet<>(favoriteDtos);
      if (!recentlyBrowsedKeys.isEmpty()) {
        componentDtos.addAll(dbClient.componentDao().selectByKeys(dbSession, recentlyBrowsedKeys));
      }
      List<ComponentDto> authorizedComponents = userSession.keepAuthorizedComponents(USER, componentDtos);
      ListMultimap<String, ComponentDto> componentsPerQualifier = authorizedComponents.stream()
        .collect(MoreCollectors.index(ComponentDto::qualifier));
      if (componentsPerQualifier.isEmpty()) {
        return newBuilder().build();
      }

      Set<String> favoriteUuids = favoriteDtos.stream().map(ComponentDto::uuid).collect(MoreCollectors.toSet(favoriteDtos.size()));
      Comparator<ComponentDto> favoriteComparator = Comparator.comparing(c -> favoriteUuids.contains(c.uuid()) ? -1 : +1);
      Comparator<ComponentDto> comparator = favoriteComparator.thenComparing(ComponentDto::name);

      ComponentIndexResults componentsPerQualifiers = ComponentIndexResults.newBuilder().setQualifiers(
        qualifiers.stream().map(q -> {
          List<ComponentDto> componentsOfThisQualifier = componentsPerQualifier.get(q);
          List<ComponentHit> hits = componentsOfThisQualifier
            .stream()
            .sorted(comparator)
            .skip(skip)
            .limit(limit)
            .map(ComponentDto::uuid)
            .map(ComponentHit::new)
            .collect(MoreCollectors.toList(limit));
          int totalHits = componentsOfThisQualifier.size();
          return new ComponentHitsPerQualifier(q, hits, totalHits);
        })).build();
      return buildResponse(recentlyBrowsedKeys, favoriteUuids, componentsPerQualifiers, dbSession, authorizedComponents, skip + limit).build();
    }
  }

  private SuggestionsWsResponse loadSuggestionsWithSearch(String query, int skip, int limit, Set<String> recentlyBrowsedKeys, List<String> qualifiers) {
    if (split(query).noneMatch(token -> token.length() >= MINIMUM_NGRAM_LENGTH)) {
      SuggestionsWsResponse.Builder queryBuilder = newBuilder();
      getWarning(query).ifPresent(queryBuilder::setWarning);
      return queryBuilder.build();
    }

    List<ComponentDto> favorites = favoriteFinder.list();
    Set<String> favoriteKeys = favorites.stream().map(ComponentDto::getDbKey).collect(MoreCollectors.toSet(favorites.size()));
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
      Set<String> componentUuids = componentsPerQualifiers.getQualifiers()
        .map(ComponentHitsPerQualifier::getHits)
        .flatMap(Collection::stream)
        .map(ComponentHit::getUuid)
        .collect(toSet());
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
      Set<String> favoriteUuids = favorites.stream().map(ComponentDto::uuid).collect(MoreCollectors.toSet(favorites.size()));
      SuggestionsWsResponse.Builder searchWsResponse = buildResponse(recentlyBrowsedKeys, favoriteUuids, componentsPerQualifiers, dbSession, componentDtos, skip + limit);
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
    Set<String> availableQualifiers = resourceTypes.getAll().stream()
      .map(ResourceType::getQualifier)
      .filter(q -> !q.equals(Qualifiers.MODULE))
      .collect(MoreCollectors.toSet());
    if (more == null) {
      return stream(SuggestionCategory.values())
        .map(SuggestionCategory::getQualifier)
        .filter(availableQualifiers::contains)
        .collect(Collectors.toList());
    }

    String qualifier = SuggestionCategory.getByName(more).getQualifier();
    return availableQualifiers.contains(qualifier) ? singletonList(qualifier) : emptyList();
  }

  private SuggestionsWsResponse.Builder buildResponse(Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids, ComponentIndexResults componentsPerQualifiers,
    DbSession dbSession, List<ComponentDto> componentDtos, int coveredItems) {

    Map<String, ComponentDto> componentsByUuids = componentDtos.stream()
      .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
    Map<String, OrganizationDto> organizationsByUuids = loadOrganizations(dbSession, componentsByUuids.values());
    Map<String, ComponentDto> projectsByUuids = loadProjects(dbSession, componentsByUuids.values());
    return toResponse(componentsPerQualifiers, recentlyBrowsedKeys, favoriteUuids, organizationsByUuids, componentsByUuids, projectsByUuids, coveredItems);
  }

  private Map<String, ComponentDto> loadProjects(DbSession dbSession, Collection<ComponentDto> components) {
    Set<String> projectUuids = components.stream()
      .filter(c -> QUALIFIERS_FOR_WHICH_TO_RETURN_PROJECT.contains(c.qualifier()))
      .map(ComponentDto::projectUuid)
      .collect(MoreCollectors.toSet());
    return dbClient.componentDao().selectByUuids(dbSession, projectUuids).stream()
      .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
  }

  private Map<String, OrganizationDto> loadOrganizations(DbSession dbSession, Collection<ComponentDto> components) {
    Set<String> organizationUuids = components.stream()
      .map(ComponentDto::getOrganizationUuid)
      .collect(MoreCollectors.toSet());
    return dbClient.organizationDao().selectByUuids(dbSession, organizationUuids).stream()
      .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
  }

  private ComponentIndexResults searchInIndex(SuggestionQuery suggestionQuery) {
    return index.searchSuggestions(suggestionQuery);
  }

  private static SuggestionsWsResponse.Builder toResponse(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids,
    Map<String, OrganizationDto> organizationsByUuids, Map<String, ComponentDto> componentsByUuids, Map<String, ComponentDto> projectsByUuids, int coveredItems) {
    if (componentsPerQualifiers.isEmpty()) {
      return newBuilder();
    }
    return newBuilder()
      .addAllResults(toCategories(componentsPerQualifiers, recentlyBrowsedKeys, favoriteUuids, componentsByUuids, organizationsByUuids, projectsByUuids, coveredItems))
      .addAllOrganizations(toOrganizations(organizationsByUuids))
      .addAllProjects(toProjects(projectsByUuids));
  }

  private static List<Category> toCategories(ComponentIndexResults componentsPerQualifiers, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids,
    Map<String, ComponentDto> componentsByUuids, Map<String, OrganizationDto> organizationByUuids, Map<String, ComponentDto> projectsByUuids, int coveredItems) {
    return componentsPerQualifiers.getQualifiers().map(qualifier -> {

      List<Suggestion> suggestions = qualifier.getHits().stream()
        .map(hit -> toSuggestion(hit, recentlyBrowsedKeys, favoriteUuids, componentsByUuids, organizationByUuids, projectsByUuids))
        .filter(Objects::nonNull)
        .collect(toList());

      return Category.newBuilder()
        .setQ(qualifier.getQualifier())
        .setMore(Math.max(0, qualifier.getTotalHits() - coveredItems))
        .addAllItems(suggestions)
        .build();
    }).collect(toList());
  }

  /**
   * @return null when the component exists in Elasticsearch but not in database. That
   * occurs when failed indexing requests are been recovering.
   */
  @CheckForNull
  private static Suggestion toSuggestion(ComponentHit hit, Set<String> recentlyBrowsedKeys, Set<String> favoriteUuids, Map<String, ComponentDto> componentsByUuids,
    Map<String, OrganizationDto> organizationByUuids, Map<String, ComponentDto> projectsByUuids) {
    ComponentDto result = componentsByUuids.get(hit.getUuid());
    if (result == null
      // SONAR-11419 this has happened in production while code does not really allow it. An inconsistency in DB may be the cause.
      || (QUALIFIERS_FOR_WHICH_TO_RETURN_PROJECT.contains(result.qualifier()) && projectsByUuids.get(result.projectUuid()) == null)) {
      return null;
    }
    String organizationKey = organizationByUuids.get(result.getOrganizationUuid()).getKey();
    checkState(organizationKey != null, "Organization with uuid '%s' not found", result.getOrganizationUuid());
    Suggestion.Builder builder = Suggestion.newBuilder()
      .setOrganization(organizationKey)
      .setKey(result.getDbKey())
      .setName(result.name())
      .setMatch(hit.getHighlightedText().orElse(HtmlEscapers.htmlEscaper().escape(result.name())))
      .setIsRecentlyBrowsed(recentlyBrowsedKeys.contains(result.getDbKey()))
      .setIsFavorite(favoriteUuids.contains(result.uuid()));
    if (QUALIFIERS_FOR_WHICH_TO_RETURN_PROJECT.contains(result.qualifier())) {
      builder.setProject(projectsByUuids.get(result.projectUuid()).getDbKey());
    }
    return builder.build();
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
        .setKey(p.getDbKey())
        .setName(p.longName())
        .build())
      .collect(Collectors.toList());
  }
}
