/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.TokenType;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier;
import org.sonar.server.component.ws.FilterParser.Criterion;
import org.sonar.server.component.ws.SearchProjectsAction.SearchResults.SearchResultsBuilder;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.TokenUserSession;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.SearchProjectsWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.measure.ProjectMeasuresIndexerIterator.METRIC_KEYS;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.IS_FAVORITE_CRITERION;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.component.ws.ProjectMeasuresQueryValidator.NON_METRIC_SORT_KEYS;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_CREATION_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_LAST_ANALYSIS_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH_PROJECTS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

public class SearchProjectsAction implements ComponentsWsAction {
  public static final int MAX_PAGE_SIZE = 500;
  public static final int DEFAULT_PAGE_SIZE = 100;
  private static final String ALL = "_all";
  private static final String ANALYSIS_DATE = "analysisDate";
  private static final String LEAK_PERIOD_DATE = "leakPeriodDate";
  private static final String METRIC_LEAK_PROJECTS_KEY = "leak_projects";
  private static final String HTML_POSSIBLE_VALUES_TEXT = "The possible values are:";
  private static final String HTML_UL_START_TAG = "<ul>";
  private static final String HTML_UL_END_TAG = "</ul>";
  private static final Set<String> POSSIBLE_FIELDS = newHashSet(ALL, ANALYSIS_DATE, LEAK_PERIOD_DATE);

  private final DbClient dbClient;
  private final ProjectMeasuresIndex index;
  private final UserSession userSession;
  private final PlatformEditionProvider editionProvider;
  private final AiCodeAssuranceVerifier aiCodeAssuranceVerifier;

  public SearchProjectsAction(DbClient dbClient, ProjectMeasuresIndex index, UserSession userSession,
    PlatformEditionProvider editionProvider, AiCodeAssuranceVerifier aiCodeAssuranceVerifier) {
    this.dbClient = dbClient;
    this.index = index;
    this.userSession = userSession;
    this.editionProvider = editionProvider;
    this.aiCodeAssuranceVerifier = aiCodeAssuranceVerifier;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH_PROJECTS)
      .setSince("6.2")
      .setDescription("Search for projects")
      .addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE)
      .setInternal(true)
      .setChangelog(
        new Change("10.7", "Add 'isAiCodeAssured' response field"),
        new Change("10.3", "Add 'creationDate' sort parameter."),
        new Change("10.2", "Field 'needIssueSync' removed from response"),
        new Change("8.3", "Add 'qualifier' filter and facet"),
        new Change("8.0", "Field 'id' removed from response"))
      .setResponseExample(getClass().getResource("search_projects-example.json"))
      .setHandler(this);

    action.createFieldsParam(POSSIBLE_FIELDS)
      .setDescription("Comma-separated list of the fields to be returned in response")
      .setSince("6.4");
    action.createParam(FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(Arrays.stream(ProjectMeasuresIndex.Facet.values())
        .map(ProjectMeasuresIndex.Facet::getName)
        .sorted()
        .toList());
    action
      .createParam(PARAM_FILTER)
      .setMinimumLength(2)
      .setDescription("Filter of projects on name, key, measure value, quality gate, language, tag or whether a project is a favorite or not.<br>" +
        "The filter must be encoded to form a valid URL (for example '=' must be replaced by '%3D').<br>" +
        "Examples of use:" +
        HTML_UL_START_TAG +
        " <li>to filter my favorite projects with a failed quality gate and a coverage greater than or equals to 60% and a coverage strictly lower than 80%:<br>" +
        "   <code>filter=\"alert_status = ERROR and isFavorite and coverage >= 60 and coverage < 80\"</code></li>" +
        " <li>to filter projects with a reliability, security and maintainability rating equals or worse than B:<br>" +
        "   <code>filter=\"reliability_rating>=2 and security_rating>=2 and sqale_rating>=2\"</code></li>" +
        " <li>to filter projects without duplication data:<br>" +
        "   <code>filter=\"duplicated_lines_density = NO_DATA\"</code></li>" +
        HTML_UL_END_TAG +
        "To filter on project name or key, use the 'query' keyword, for instance : <code>filter='query = \"Sonar\"'</code>.<br>" +
        "<br>" +
        "To filter on a numeric metric, provide the metric key.<br>" +
        "These are the supported metric keys:<br>" +
        HTML_UL_START_TAG +
        METRIC_KEYS.stream().sorted().map(key -> "<li>" + key + "</li>").collect(Collectors.joining()) +
        HTML_UL_END_TAG +
        "<br>" +
        "To filter on a rating, provide the corresponding metric key (ex: reliability_rating for reliability rating).<br>" +
        HTML_POSSIBLE_VALUES_TEXT +
        HTML_UL_START_TAG +
        " <li>'1' for rating A</li>" +
        " <li>'2' for rating B</li>" +
        " <li>'3' for rating C</li>" +
        " <li>'4' for rating D</li>" +
        " <li>'5' for rating E</li>" +
        HTML_UL_END_TAG +
        "To filter on a Quality Gate status use the metric key 'alert_status'. Only the '=' operator can be used.<br>" +
        HTML_POSSIBLE_VALUES_TEXT +
        HTML_UL_START_TAG +
        " <li>'OK' for Passed</li>" +
        " <li>'WARN' for Warning</li>" +
        " <li>'ERROR' for Failed</li>" +
        HTML_UL_END_TAG +
        "To filter on languages use the 'languages' keyword: " +
        HTML_UL_START_TAG +
        " <li>to filter on a single language you can use 'languages = java'</li>" +
        " <li>to filter on several languages you must use 'languages IN (java, js)'</li>" +
        HTML_UL_END_TAG +
        "Use the WS api/languages/list to find the key of a language.<br> " +
        "To filter on tags use the 'tags' keyword:" +
        HTML_UL_START_TAG +
        " <li>to filter on one tag you can use <code>tags = finance</code></li>" +
        " <li>to filter on several tags you must use <code>tags in (offshore, java)</code></li>" +
        HTML_UL_END_TAG +
        "To filter on a qualifier use key 'qualifier'. Only the '=' operator can be used.<br>" +
        HTML_POSSIBLE_VALUES_TEXT +
        HTML_UL_START_TAG +
        " <li>TRK - for projects</li>" +
        " <li>APP - for applications</li>" +
        HTML_UL_END_TAG);
    action.createParam(Param.SORT)
      .setDescription("Sort projects by numeric metric key, quality gate status (using '%s'), last analysis date (using '%s'), project name or creationDate (using '%s').",
        ALERT_STATUS_KEY, SORT_BY_LAST_ANALYSIS_DATE, PARAM_FILTER, SORT_BY_CREATION_DATE)
      .setDefaultValue(SORT_BY_NAME)
      .setPossibleValues(
        Stream.concat(METRIC_KEYS.stream(), NON_METRIC_SORT_KEYS.stream()).sorted().toList())
      .setSince("6.4");
    action.createParam(Param.ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue(true);
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    SearchProjectsWsResponse response = doHandle(toRequest(httpRequest));

    writeProtobuf(response, httpRequest, httpResponse);
  }

  private SearchProjectsWsResponse doHandle(SearchProjectsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResults searchResults = searchData(dbSession, request);
      return buildResponse(request, searchResults);
    }
  }

  private SearchResults searchData(DbSession dbSession, SearchProjectsRequest request) {
    Set<String> favoriteProjectUuids = loadFavoriteProjectUuids(dbSession);
    List<Criterion> criteria = FilterParser.parse(firstNonNull(request.getFilter(), ""));
    ProjectMeasuresQuery query = newProjectMeasuresQuery(criteria, hasFavoriteFilter(criteria) ? favoriteProjectUuids : null)
      .setSort(request.getSort())
      .setAsc(request.getAsc());

    Set<String> qualifiersBasedOnEdition = getQualifiersBasedOnEdition(query);
    query.setQualifiers(qualifiersBasedOnEdition);

    ProjectMeasuresQueryValidator.validate(query);

    SearchIdResult<String> esResults = index.search(query, new SearchOptions()
      // skip facets for project token authorization, avoid exposing unauthorized projects count
      .addFacets(isProjectAnalysisToken() ? emptyList() : request.getFacets())
      .setPage(request.getPage(), request.getPageSize()));

    List<String> projectUuids = esResults.getUuids();
    Ordering<ProjectDto> ordering = Ordering.explicit(projectUuids).onResultOf(ProjectDto::getUuid);
    List<ProjectDto> projects = ordering.immutableSortedCopy(dbClient.projectDao().selectByUuids(dbSession, new HashSet<>(projectUuids)));
    projects = userSession.keepAuthorizedEntities(UserRole.USER, projects);

    Map<String, BranchDto> mainBranchByUuid = dbClient.branchDao().selectMainBranchesByProjectUuids(dbSession, projectUuids)
      .stream()
      .collect(Collectors.toMap(BranchDto::getUuid, b -> b));

    List<SnapshotDto> snapshots = getSnapshots(dbSession, request, mainBranchByUuid.keySet());
    Map<String, SnapshotDto> analysisByProjectUuid = snapshots.stream()
      .collect(Collectors.toMap(s -> mainBranchByUuid.get(s.getRootComponentUuid()).getProjectUuid(), s -> s));
    Map<String, Long> applicationsBranchLeakPeriod = getApplicationsLeakPeriod(dbSession, request, qualifiersBasedOnEdition, mainBranchByUuid.keySet());
    Map<String, Long> applicationsLeakPeriod = applicationsBranchLeakPeriod.entrySet()
      .stream()
      .collect(Collectors.toMap(e -> mainBranchByUuid.get(e.getKey()).getProjectUuid(), Entry::getValue));

    return SearchResultsBuilder.builder()
      .projects(projects)
      .favoriteProjectUuids(favoriteProjectUuids)
      .searchResults(esResults)
      .analysisByProjectUuid(analysisByProjectUuid)
      .applicationsLeakPeriods(applicationsLeakPeriod)
      .query(query)
      .build();
  }

  private Set<String> getQualifiersBasedOnEdition(ProjectMeasuresQuery query) {
    Set<String> availableQualifiers = getQualifiersFromEdition();
    Set<String> requestQualifiers = query.getQualifiers().orElse(availableQualifiers);

    Set<String> resolvedQualifiers = requestQualifiers.stream()
      .filter(availableQualifiers::contains)
      .collect(Collectors.toSet());
    if (!resolvedQualifiers.isEmpty()) {
      return resolvedQualifiers;
    } else {
      throw new IllegalArgumentException("Invalid qualifier, available are: " + String.join(",", availableQualifiers));
    }
  }

  private Set<String> getQualifiersFromEdition() {
    Optional<Edition> edition = editionProvider.get();

    if (edition.isEmpty()) {
      return Sets.newHashSet(ComponentQualifiers.PROJECT);
    }

    return switch (edition.get()) {
      case ENTERPRISE, DATACENTER, DEVELOPER -> Sets.newHashSet(ComponentQualifiers.PROJECT, ComponentQualifiers.APP);
      default -> Sets.newHashSet(ComponentQualifiers.PROJECT);
    };
  }

  private static boolean hasFavoriteFilter(List<Criterion> criteria) {
    return criteria.stream()
      .map(Criterion::getKey)
      .anyMatch(IS_FAVORITE_CRITERION::equalsIgnoreCase);
  }

  private Set<String> loadFavoriteProjectUuids(DbSession dbSession) {
    if (!userSession.isLoggedIn()) {
      return Collections.emptySet();
    }

    List<PropertyDto> props = dbClient.propertiesDao().selectByQuery(
      PropertyQuery.builder()
        .setUserUuid(userSession.getUuid())
        .setKey("favourite")
        .build(),
      dbSession);

    Set<String> favoriteDbUuids = props.stream()
      .map(PropertyDto::getEntityUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    return dbClient.projectDao().selectByUuids(dbSession, favoriteDbUuids).stream()
      .map(ProjectDto::getUuid)
      .collect(Collectors.toSet());
  }

  private List<SnapshotDto> getSnapshots(DbSession dbSession, SearchProjectsRequest request, Collection<String> mainBranchUuids) {
    if (request.getAdditionalFields().contains(ANALYSIS_DATE) || request.getAdditionalFields().contains(LEAK_PERIOD_DATE)) {
      return dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, mainBranchUuids);
    }
    return emptyList();
  }

  private Map<String, Long> getApplicationsLeakPeriod(DbSession dbSession, SearchProjectsRequest request, Set<String> qualifiers, Collection<String> mainBranchUuids) {
    if (qualifiers.contains(ComponentQualifiers.APP) && request.getAdditionalFields().contains(LEAK_PERIOD_DATE)) {
      return dbClient.measureDao().selectByComponentUuidsAndMetricKeys(dbSession, mainBranchUuids, Collections.singleton(METRIC_LEAK_PROJECTS_KEY))
        .stream()
        .filter(m -> !Objects.isNull(m.getString(METRIC_LEAK_PROJECTS_KEY)))
        .map(m -> Maps.immutableEntry(m.getComponentUuid(), ApplicationLeakProjects.parse(m.getString(METRIC_LEAK_PROJECTS_KEY)).getOldestLeak()))
        .filter(entry -> entry.getValue().isPresent())
        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().get().getLeak()));
    }

    return emptyMap();
  }

  private static SearchProjectsRequest toRequest(Request httpRequest) {
    RequestBuilder request = new RequestBuilder()
      .setFilter(httpRequest.param(PARAM_FILTER))
      .setSort(httpRequest.mandatoryParam(Param.SORT))
      .setAsc(httpRequest.mandatoryParamAsBoolean(Param.ASCENDING))
      .setPage(httpRequest.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(httpRequest.mandatoryParamAsInt(Param.PAGE_SIZE));
    if (httpRequest.hasParam(FACETS)) {
      request.setFacets(httpRequest.mandatoryParamAsStrings(FACETS));
    }
    if (httpRequest.hasParam(FIELDS)) {
      List<String> paramsAsString = httpRequest.mandatoryParamAsStrings(FIELDS);
      if (paramsAsString.contains(ALL)) {
        request.setAdditionalFields(List.of(ANALYSIS_DATE, LEAK_PERIOD_DATE));
      } else {
        request.setAdditionalFields(paramsAsString);
      }
    }
    return request.build();
  }

  private SearchProjectsWsResponse buildResponse(SearchProjectsRequest request, SearchResults searchResults) {
    Function<ProjectDto, Component> dbToWsComponent = new DbToWsComponent(request, searchResults, userSession.isLoggedIn());

    return Stream.of(SearchProjectsWsResponse.newBuilder())
      .map(response -> response.setPaging(Common.Paging.newBuilder()
        .setPageIndex(request.getPage())
        .setPageSize(request.getPageSize())
        // skip total for project token authorization, avoid exposing unauthorized projects count
        .setTotal(isProjectAnalysisToken() ? searchResults.projects.size() : searchResults.total)))
      .map(response -> {
        searchResults.projects.stream()
          .map(dbToWsComponent)
          .forEach(response::addComponents);
        return response;
      })
      .map(response -> addFacets(searchResults, response))
      .map(SearchProjectsWsResponse.Builder::build)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("SearchProjectsWsResponse not built"));
  }

  private static SearchProjectsWsResponse.Builder addFacets(SearchResults searchResults, SearchProjectsWsResponse.Builder wsResponse) {
    Facets esFacets = searchResults.facets;
    EsToWsFacet esToWsFacet = new EsToWsFacet();

    searchResults.query.getLanguages().ifPresent(languages -> addMandatoryValuesToFacet(esFacets, FILTER_LANGUAGES, languages));
    searchResults.query.getTags().ifPresent(tags -> addMandatoryValuesToFacet(esFacets, FILTER_TAGS, tags));
    Common.Facets wsFacets = esFacets.getAll().entrySet().stream()
      .map(esToWsFacet)
      .collect(Collector.of(
        Common.Facets::newBuilder,
        Common.Facets.Builder::addFacets,
        (result1, result2) -> {
          throw new IllegalStateException("Parallel execution forbidden");
        },
        Common.Facets.Builder::build));

    wsResponse.setFacets(wsFacets);

    return wsResponse;
  }

  private static void addMandatoryValuesToFacet(Facets facets, String facetName, Iterable<String> mandatoryValues) {
    Map<String, Long> buckets = facets.get(facetName);
    if (buckets == null) {
      return;
    }
    for (String mandatoryValue : mandatoryValues) {
      if (!buckets.containsKey(mandatoryValue)) {
        buckets.put(mandatoryValue, 0L);
      }
    }
  }

  private static class EsToWsFacet implements Function<Entry<String, LinkedHashMap<String, Long>>, Common.Facet> {
    private final BucketToFacetValue bucketToFacetValue = new BucketToFacetValue();
    private final Common.Facet.Builder wsFacet = Common.Facet.newBuilder();

    @Override
    public Common.Facet apply(Entry<String, LinkedHashMap<String, Long>> esFacet) {
      wsFacet
        .clear()
        .setProperty(esFacet.getKey());
      LinkedHashMap<String, Long> buckets = esFacet.getValue();
      if (buckets != null) {
        buckets.entrySet()
          .stream()
          .map(bucketToFacetValue)
          .forEach(wsFacet::addValues);
      } else {
        wsFacet.addAllValues(Collections.emptyList());
      }

      return wsFacet.build();
    }
  }

  private static class BucketToFacetValue implements Function<Entry<String, Long>, Common.FacetValue> {
    private final Common.FacetValue.Builder facetValue;

    private BucketToFacetValue() {
      this.facetValue = Common.FacetValue.newBuilder();
    }

    @Override
    public Common.FacetValue apply(Entry<String, Long> bucket) {
      return facetValue
        .clear()
        .setVal(bucket.getKey())
        .setCount(bucket.getValue())
        .build();
    }
  }

  private class DbToWsComponent implements Function<ProjectDto, Component> {
    private final SearchProjectsRequest request;
    private final Component.Builder wsComponent;
    private final Set<String> favoriteProjectUuids;
    private final boolean isUserLoggedIn;
    private final Map<String, SnapshotDto> analysisByProjectUuid;
    private final Map<String, Long> applicationsLeakPeriod;

    private DbToWsComponent(SearchProjectsRequest request, SearchResults searchResults, boolean isUserLoggedIn) {
      this.request = request;
      this.analysisByProjectUuid = searchResults.analysisByProjectUuid;
      this.applicationsLeakPeriod = searchResults.applicationsLeakPeriods;
      this.wsComponent = Component.newBuilder();
      this.favoriteProjectUuids = searchResults.favoriteProjectUuids;
      this.isUserLoggedIn = isUserLoggedIn;
    }

    @Override
    public Component apply(ProjectDto dbProject) {
      wsComponent
        .clear()
        .setKey(dbProject.getKey())
        .setName(dbProject.getName())
        .setQualifier(dbProject.getQualifier())
        .setVisibility(Visibility.getLabel(dbProject.isPrivate()))
        .setIsAiCodeAssured(aiCodeAssuranceVerifier.isAiCodeAssured(dbProject))
        .setIsAiCodeFixEnabled(dbProject.getAiCodeFixEnabled());
      wsComponent.getTagsBuilder().addAllTags(dbProject.getTags());

      SnapshotDto snapshotDto = analysisByProjectUuid.get(dbProject.getUuid());
      if (snapshotDto != null) {
        if (request.getAdditionalFields().contains(ANALYSIS_DATE)) {
          wsComponent.setAnalysisDate(formatDateTime(snapshotDto.getCreatedAt()));
        }
        if (request.getAdditionalFields().contains(LEAK_PERIOD_DATE)) {
          if (ComponentQualifiers.APP.equals(dbProject.getQualifier())) {
            ofNullable(applicationsLeakPeriod.get(dbProject.getUuid())).ifPresent(leakPeriodDate -> wsComponent.setLeakPeriodDate(formatDateTime(leakPeriodDate)));
          } else {
            ofNullable(snapshotDto.getPeriodDate()).ifPresent(leakPeriodDate -> wsComponent.setLeakPeriodDate(formatDateTime(leakPeriodDate)));
          }
        }
      }

      if (isUserLoggedIn) {
        wsComponent.setIsFavorite(favoriteProjectUuids.contains(dbProject.getUuid()));
      }

      return wsComponent.build();
    }

  }

  public static class SearchResults {
    private final List<ProjectDto> projects;
    private final Set<String> favoriteProjectUuids;
    private final Facets facets;
    private final Map<String, SnapshotDto> analysisByProjectUuid;
    private final Map<String, Long> applicationsLeakPeriods;
    private final ProjectMeasuresQuery query;
    private final int total;

    private SearchResults(List<ProjectDto> projects, Set<String> favoriteProjectUuids, SearchIdResult<String> searchResults, Map<String, SnapshotDto> analysisByProjectUuid,
      Map<String, Long> applicationsLeakPeriods, ProjectMeasuresQuery query) {
      this.projects = projects;
      this.favoriteProjectUuids = favoriteProjectUuids;
      this.total = (int) searchResults.getTotal();
      this.facets = searchResults.getFacets();
      this.analysisByProjectUuid = analysisByProjectUuid;
      this.applicationsLeakPeriods = applicationsLeakPeriods;
      this.query = query;
    }

    public static final class SearchResultsBuilder {

      private List<ProjectDto> projects;
      private Set<String> favoriteProjectUuids;
      private Map<String, SnapshotDto> analysisByProjectUuid;
      private Map<String, Long> applicationsLeakPeriods;
      private ProjectMeasuresQuery query;
      private SearchIdResult<String> searchResults;

      private SearchResultsBuilder() {
      }

      public static SearchResultsBuilder builder() {
        return new SearchResultsBuilder();
      }

      public SearchResultsBuilder projects(List<ProjectDto> projects) {
        this.projects = projects;
        return this;
      }

      public SearchResultsBuilder favoriteProjectUuids(Set<String> favoriteProjectUuids) {
        this.favoriteProjectUuids = favoriteProjectUuids;
        return this;
      }

      public SearchResultsBuilder analysisByProjectUuid(Map<String, SnapshotDto> analysisByProjectUuid) {
        this.analysisByProjectUuid = analysisByProjectUuid;
        return this;
      }

      public SearchResultsBuilder applicationsLeakPeriods(Map<String, Long> applicationsLeakPeriods) {
        this.applicationsLeakPeriods = applicationsLeakPeriods;
        return this;
      }

      public SearchResultsBuilder query(ProjectMeasuresQuery query) {
        this.query = query;
        return this;
      }

      public SearchResultsBuilder searchResults(SearchIdResult<String> searchResults) {
        this.searchResults = searchResults;
        return this;
      }

      public SearchResults build() {
        return new SearchResults(projects, favoriteProjectUuids, searchResults, analysisByProjectUuid, applicationsLeakPeriods, query);
      }
    }
  }

  static class SearchProjectsRequest {

    private final int page;
    private final int pageSize;
    private final String filter;
    private final List<String> facets;
    private final String sort;
    private final Boolean asc;
    private final List<String> additionalFields;

    private SearchProjectsRequest(RequestBuilder builder) {
      this.page = builder.page;
      this.pageSize = builder.pageSize;
      this.filter = builder.filter;
      this.facets = builder.facets;
      this.sort = builder.sort;
      this.asc = builder.asc;
      this.additionalFields = builder.additionalFields;
    }

    @CheckForNull
    public String getFilter() {
      return filter;
    }

    public List<String> getFacets() {
      return facets;
    }

    @CheckForNull
    public String getSort() {
      return sort;
    }

    public int getPageSize() {
      return pageSize;
    }

    public int getPage() {
      return page;
    }

    @CheckForNull
    public Boolean getAsc() {
      return asc;
    }

    public List<String> getAdditionalFields() {
      return additionalFields;
    }

    public static RequestBuilder builder() {
      return new RequestBuilder();
    }

  }

  static class RequestBuilder {
    private Integer page;
    private Integer pageSize;
    private String filter;
    private List<String> facets = new ArrayList<>();
    private String sort;
    private Boolean asc;
    private List<String> additionalFields = new ArrayList<>();

    private RequestBuilder() {
      // enforce static factory method
    }

    public RequestBuilder setFilter(@Nullable String filter) {
      this.filter = filter;
      return this;
    }

    public RequestBuilder setFacets(List<String> facets) {
      this.facets = requireNonNull(facets);
      return this;
    }

    public RequestBuilder setPage(int page) {
      this.page = page;
      return this;
    }

    public RequestBuilder setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public RequestBuilder setSort(@Nullable String sort) {
      this.sort = sort;
      return this;
    }

    public RequestBuilder setAsc(boolean asc) {
      this.asc = asc;
      return this;
    }

    public RequestBuilder setAdditionalFields(List<String> additionalFields) {
      this.additionalFields = requireNonNull(additionalFields, "additional fields cannot be null");
      return this;
    }

    public SearchProjectsRequest build() {
      if (page == null) {
        page = 1;
      }
      if (pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
      checkArgument(pageSize <= MAX_PAGE_SIZE, "Page size must not be greater than %s", MAX_PAGE_SIZE);
      return new SearchProjectsRequest(this);
    }
  }

  private boolean isProjectAnalysisToken() {
    if (userSession instanceof TokenUserSession tokenUserSession) {
      return TokenType.PROJECT_ANALYSIS_TOKEN.equals(tokenUserSession.getTokenType());
    }
    return false;
  }
}
