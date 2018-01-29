/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.issue.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.TermsLookup;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.Sorting;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQuery.PeriodStart;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.user.UserSession;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.es.BaseDoc.epochMillisToEpochSeconds;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.INDEX_TYPE_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_FACET_MODE_DEBT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_PARAM_ACTION_PLANS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_ASSIGNED_TO_ME;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHORS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_REPORTERS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

/**
 * The unique entry-point to interact with Elasticsearch index "issues".
 * All the requests are listed here.
 */
public class IssueIndex {

  public static final List<String> SUPPORTED_FACETS = ImmutableList.of(
    PARAM_SEVERITIES,
    PARAM_STATUSES,
    PARAM_RESOLUTIONS,
    DEPRECATED_PARAM_ACTION_PLANS,
    PARAM_PROJECT_UUIDS,
    PARAM_RULES,
    PARAM_ASSIGNEES,
    FACET_ASSIGNED_TO_ME,
    PARAM_REPORTERS,
    PARAM_AUTHORS,
    PARAM_MODULE_UUIDS,
    PARAM_FILE_UUIDS,
    PARAM_DIRECTORIES,
    PARAM_LANGUAGES,
    PARAM_TAGS,
    PARAM_TYPES,
    PARAM_CREATED_AT);
  public static final String AGGREGATION_NAME_FOR_TAGS = "tags__issues";
  private static final String SUBSTRING_MATCH_REGEXP = ".*%s.*";
  // TODO to be documented
  // TODO move to Facets ?
  private static final String FACET_SUFFIX_MISSING = "_missing";
  private static final String IS_ASSIGNED_FILTER = "__isAssigned";
  private static final SumAggregationBuilder EFFORT_AGGREGATION = AggregationBuilders.sum(FACET_MODE_EFFORT).field(IssueIndexDefinition.FIELD_ISSUE_EFFORT);
  private static final Order EFFORT_AGGREGATION_ORDER = Order.aggregation(FACET_MODE_EFFORT, false);
  private static final int DEFAULT_FACET_SIZE = 15;
  private static final Duration TWENTY_DAYS = Duration.standardDays(20L);
  private static final Duration TWENTY_WEEKS = Duration.standardDays(20L * 7L);
  private static final Duration TWENTY_MONTHS = Duration.standardDays(20L * 30L);
  private final Sorting sorting;
  private final EsClient client;
  private final System2 system;
  private final UserSession userSession;
  private final AuthorizationTypeSupport authorizationTypeSupport;

  public IssueIndex(EsClient client, System2 system, UserSession userSession, AuthorizationTypeSupport authorizationTypeSupport) {
    this.client = client;
    this.system = system;
    this.userSession = userSession;
    this.authorizationTypeSupport = authorizationTypeSupport;

    this.sorting = new Sorting();
    this.sorting.add(IssueQuery.SORT_BY_ASSIGNEE, IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE);
    this.sorting.add(IssueQuery.SORT_BY_STATUS, IssueIndexDefinition.FIELD_ISSUE_STATUS);
    this.sorting.add(IssueQuery.SORT_BY_SEVERITY, IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE);
    this.sorting.add(IssueQuery.SORT_BY_CREATION_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_UPDATE_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_CLOSE_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_FILE_PATH);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_LINE);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE).reverse();
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_KEY);

    // by default order by created date, project, file, line and issue key (in order to be deterministic when same ms)
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT).reverse();
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH);
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_LINE);
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  /**
   * Optimization - do not send ES request to all shards when scope is restricted
   * to a set of projects. Because project UUID is used for routing, the request
   * can be sent to only the shards containing the specified projects.
   * Note that sticky facets may involve all projects, so this optimization must be
   * disabled when facets are enabled.
   */
  private static void configureRouting(IssueQuery query, SearchOptions options, SearchRequestBuilder requestBuilder) {
    Collection<String> uuids = query.projectUuids();
    if (!uuids.isEmpty() && options.getFacets().isEmpty()) {
      requestBuilder.setRouting(uuids.toArray(new String[uuids.size()]));
    }
  }

  private static void configurePagination(SearchOptions options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset()).setSize(options.getLimit());
  }

  private static void addComponentRelatedFilters(IssueQuery query, Map<String, QueryBuilder> filters) {
    QueryBuilder viewFilter = createViewFilter(query.viewUuids());
    QueryBuilder componentFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.componentUuids());
    QueryBuilder projectFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids());
    QueryBuilder moduleRootFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, query.moduleRootUuids());
    QueryBuilder moduleFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, query.moduleUuids());
    QueryBuilder directoryFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, query.directories());
    QueryBuilder fileFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.fileUuids());
    QueryBuilder branchFilter = createTermFilter(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID, query.branchUuid());
    filters.put("__is_main_branch", createTermFilter(IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(query.isMainBranch())));

    if (BooleanUtils.isTrue(query.onComponentOnly())) {
      filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, componentFilter);
    } else {
      filters.put("__view", viewFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID, branchFilter);
      filters.put("__module", moduleRootFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
      if (fileFilter != null) {
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      } else {
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, componentFilter);
      }
    }
  }

  @CheckForNull
  private static QueryBuilder createViewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }

    BoolQueryBuilder viewsFilter = boolQuery();
    for (String viewUuid : viewUuids) {
      viewsFilter.should(QueryBuilders.termsLookupQuery(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID,
        new TermsLookup(
          ViewIndexDefinition.INDEX_TYPE_VIEW.getIndex(),
          ViewIndexDefinition.INDEX_TYPE_VIEW.getType(),
          viewUuid,
          ViewIndexDefinition.FIELD_PROJECTS)));
    }
    return viewsFilter;
  }

  private static StickyFacetBuilder newStickyFacetBuilder(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    if (hasQueryEffortFacet(query)) {
      return new StickyFacetBuilder(esQuery, filters, EFFORT_AGGREGATION, EFFORT_AGGREGATION_ORDER);
    }
    return new StickyFacetBuilder(esQuery, filters);
  }

  private static void addSimpleStickyFacetIfNeeded(SearchOptions options, StickyFacetBuilder stickyFacetBuilder, SearchRequestBuilder esSearch,
    String facetName, String fieldName, Object... selectedValues) {
    if (options.getFacets().contains(facetName)) {
      esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(fieldName, facetName, DEFAULT_FACET_SIZE, selectedValues));
    }
  }

  private static AggregationBuilder addEffortAggregationIfNeeded(IssueQuery query, AggregationBuilder aggregation) {
    if (hasQueryEffortFacet(query)) {
      aggregation.subAggregation(EFFORT_AGGREGATION);
    }
    return aggregation;
  }

  private static boolean hasQueryEffortFacet(IssueQuery query) {
    return FACET_MODE_EFFORT.equals(query.facetMode()) || DEPRECATED_FACET_MODE_DEBT.equals(query.facetMode());
  }

  private static AggregationBuilder createAssigneesFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder queryBuilder) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE;
    String facetName = PARAM_ASSIGNEES;

    // Same as in super.stickyFacetBuilder
    Map<String, QueryBuilder> assigneeFilters = Maps.newHashMap(filters);
    assigneeFilters.remove(IS_ASSIGNED_FILTER);
    assigneeFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = newStickyFacetBuilder(query, assigneeFilters, queryBuilder);
    BoolQueryBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_FACET_SIZE);

    Collection<String> assigneesEscaped = escapeValuesForFacetInclusion(query.assignees());
    if (!assigneesEscaped.isEmpty()) {
      facetTopAggregation = assigneeFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, t -> t, assigneesEscaped.toArray());
    }

    // Add missing facet for unassigned issues
    facetTopAggregation.subAggregation(
      addEffortAggregationIfNeeded(query, AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)));

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  private static Collection<String> escapeValuesForFacetInclusion(@Nullable Collection<String> values) {
    if (values == null) {
      return Collections.emptyList();
    }
    return values.stream().map(Pattern::quote).collect(MoreCollectors.toArrayList(values.size()));
  }

  private static AggregationBuilder createResolutionFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
    String facetName = PARAM_RESOLUTIONS;

    // Same as in super.stickyFacetBuilder
    Map<String, QueryBuilder> resolutionFilters = Maps.newHashMap(filters);
    resolutionFilters.remove("__isResolved");
    resolutionFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = newStickyFacetBuilder(query, resolutionFilters, esQuery);
    BoolQueryBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_FACET_SIZE);
    facetTopAggregation = assigneeFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, t -> t);

    // Add missing facet for unresolved issues
    facetTopAggregation.subAggregation(
      addEffortAggregationIfNeeded(query, AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)));

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  @CheckForNull
  private static QueryBuilder createTermsFilter(String field, Collection<?> values) {
    return values.isEmpty() ? null : termsQuery(field, values);
  }

  @CheckForNull
  private static QueryBuilder createTermFilter(String field, @Nullable String value) {
    return value == null ? null : termQuery(field, value);
  }

  public SearchResponse search(IssueQuery query, SearchOptions options) {
    SearchRequestBuilder requestBuilder = client.prepareSearch(INDEX_TYPE_ISSUE);

    configureSorting(query, requestBuilder);
    configurePagination(options, requestBuilder);
    configureRouting(query, options, requestBuilder);

    QueryBuilder esQuery = matchAllQuery();
    BoolQueryBuilder esFilter = boolQuery();
    Map<String, QueryBuilder> filters = createFilters(query);
    for (QueryBuilder filter : filters.values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }
    if (esFilter.hasClauses()) {
      requestBuilder.setQuery(boolQuery().must(esQuery).filter(esFilter));
    } else {
      requestBuilder.setQuery(esQuery);
    }

    configureStickyFacets(query, options, filters, esQuery, requestBuilder);
    requestBuilder.setFetchSource(false);
    return requestBuilder.get();
  }

  private void configureSorting(IssueQuery query, SearchRequestBuilder esRequest) {
    createSortBuilders(query).forEach(esRequest::addSort);
  }

  private List<FieldSortBuilder> createSortBuilders(IssueQuery query) {
    String sortField = query.sort();
    if (sortField != null) {
      boolean asc = BooleanUtils.isTrue(query.asc());
      return sorting.fill(sortField, asc);
    }
    return sorting.fillDefault();
  }

  private Map<String, QueryBuilder> createFilters(IssueQuery query) {
    Map<String, QueryBuilder> filters = new HashMap<>();
    filters.put("__authorization", createAuthorizationFilter(query.checkAuthorization()));

    // Issue is assigned Filter
    if (BooleanUtils.isTrue(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, existsQuery(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
    } else if (BooleanUtils.isFalse(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, boolQuery().mustNot(existsQuery(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE)));
    }

    // Issue is Resolved Filter
    String isResolved = "__isResolved";
    if (BooleanUtils.isTrue(query.resolved())) {
      filters.put(isResolved, existsQuery(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION));
    } else if (BooleanUtils.isFalse(query.resolved())) {
      filters.put(isResolved, boolQuery().mustNot(existsQuery(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION)));
    }

    // Field Filters
    filters.put(IssueIndexDefinition.FIELD_ISSUE_KEY, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_KEY, query.issueKeys()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, query.assignees()));

    addComponentRelatedFilters(query, filters);

    filters.put(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_TAGS, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_TAGS, query.tags()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_TYPE, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_TYPE, query.types()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RULE_ID, createTermsFilter(
      IssueIndexDefinition.FIELD_ISSUE_RULE_ID,
      query.rules().stream().map(RuleDefinitionDto::getId).collect(Collectors.toList())));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, query.severities()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_STATUS, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_STATUS, query.statuses()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID, createTermFilter(IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID, query.organizationUuid()));

    addDatesFilter(filters, query);
    addCreatedAfterByProjectsFilter(filters, query);
    return filters;
  }

  private QueryBuilder createAuthorizationFilter(boolean checkAuthorization) {
    if (checkAuthorization) {
      return authorizationTypeSupport.createQueryFilter();
    }
    return matchAllQuery();
  }

  private void addDatesFilter(Map<String, QueryBuilder> filters, IssueQuery query) {
    PeriodStart createdAfter = query.createdAfter();
    Date createdBefore = query.createdBefore();

    validateCreationDateBounds(createdBefore, createdAfter != null ? createdAfter.date() : null);

    if (createdAfter != null) {
      filters.put("__createdAfter", QueryBuilders
        .rangeQuery(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .from(BaseDoc.dateToEpochSeconds(createdAfter.date()), createdAfter.inclusive()));
    }
    if (createdBefore != null) {
      filters.put("__createdBefore", QueryBuilders
        .rangeQuery(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .lt(BaseDoc.dateToEpochSeconds(createdBefore)));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.put("__createdAt", termQuery(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, BaseDoc.dateToEpochSeconds(createdAt)));
    }
  }

  private static void addCreatedAfterByProjectsFilter(Map<String, QueryBuilder> filters, IssueQuery query) {
    Map<String, PeriodStart> createdAfterByProjectUuids = query.createdAfterByProjectUuids();
    BoolQueryBuilder boolQueryBuilder = boolQuery();
    createdAfterByProjectUuids.forEach((projectUuid, createdAfterDate) -> boolQueryBuilder.should(boolQuery()
      .filter(termQuery(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectUuid))
      .filter(rangeQuery(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT).from(BaseDoc.dateToEpochSeconds(createdAfterDate.date()), createdAfterDate.inclusive()))));
    filters.put("createdAfterByProjectUuids", boolQueryBuilder);
  }

  private void validateCreationDateBounds(@Nullable Date createdBefore, @Nullable Date createdAfter) {
    Preconditions.checkArgument(createdAfter == null || createdAfter.before(new Date(system.now())),
      "Start bound cannot be in the future");
    Preconditions.checkArgument(createdAfter == null || createdBefore == null || createdAfter.before(createdBefore),
      "Start bound cannot be larger or equal to end bound");
  }

  private void configureStickyFacets(IssueQuery query, SearchOptions options, Map<String, QueryBuilder> filters, QueryBuilder esQuery, SearchRequestBuilder esSearch) {
    if (!options.getFacets().isEmpty()) {
      StickyFacetBuilder stickyFacetBuilder = newStickyFacetBuilder(query, filters, esQuery);
      // Execute Term aggregations
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_SEVERITIES, IssueIndexDefinition.FIELD_ISSUE_SEVERITY);
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_STATUSES, IssueIndexDefinition.FIELD_ISSUE_STATUS);
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_PROJECT_UUIDS, IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_MODULE_UUIDS, IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, query.moduleUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_DIRECTORIES, IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, query.directories().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_FILE_UUIDS, IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.fileUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_LANGUAGES, IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_RULES, IssueIndexDefinition.FIELD_ISSUE_RULE_ID, query.rules().stream().map(RuleDefinitionDto::getId).toArray());

      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        PARAM_AUTHORS, IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query.authors().toArray());

      if (options.getFacets().contains(PARAM_TAGS)) {
        esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(IssueIndexDefinition.FIELD_ISSUE_TAGS, PARAM_TAGS, query.tags().toArray()));
      }
      if (options.getFacets().contains(PARAM_TYPES)) {
        esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(IssueIndexDefinition.FIELD_ISSUE_TYPE, PARAM_TYPES, query.types().toArray()));
      }
      if (options.getFacets().contains(PARAM_RESOLUTIONS)) {
        esSearch.addAggregation(createResolutionFacet(query, filters, esQuery));
      }
      if (options.getFacets().contains(PARAM_ASSIGNEES)) {
        esSearch.addAggregation(createAssigneesFacet(query, filters, esQuery));
      }
      addAssignedToMeFacetIfNeeded(esSearch, options, query, filters, esQuery);
      if (options.getFacets().contains(PARAM_CREATED_AT)) {
        getCreatedAtFacet(query, filters, esQuery).ifPresent(esSearch::addAggregation);
      }
    }

    if (hasQueryEffortFacet(query)) {
      esSearch.addAggregation(EFFORT_AGGREGATION);
    }
  }

  private Optional<AggregationBuilder> getCreatedAtFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    long startTime;
    boolean startInclusive;
    PeriodStart createdAfter = query.createdAfter();
    if (createdAfter == null) {
      Optional<Long> minDate = getMinCreatedAt(filters, esQuery);
      if (!minDate.isPresent()) {
        return Optional.empty();
      }
      startTime = minDate.get();
      startInclusive = true;
    } else {
      startTime = createdAfter.date().getTime();
      startInclusive = createdAfter.inclusive();
    }
    Date createdBefore = query.createdBefore();
    long endTime = createdBefore == null ? system.now() : createdBefore.getTime();

    Duration timeSpan = new Duration(startTime, endTime);
    DateHistogramInterval bucketSize = DateHistogramInterval.YEAR;
    if (timeSpan.isShorterThan(TWENTY_DAYS)) {
      bucketSize = DateHistogramInterval.DAY;
    } else if (timeSpan.isShorterThan(TWENTY_WEEKS)) {
      bucketSize = DateHistogramInterval.WEEK;
    } else if (timeSpan.isShorterThan(TWENTY_MONTHS)) {
      bucketSize = DateHistogramInterval.MONTH;
    }

    AggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(PARAM_CREATED_AT)
      .field(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
      .dateHistogramInterval(bucketSize)
      .minDocCount(0L)
      .format(DateUtils.DATETIME_FORMAT)
      .timeZone(DateTimeZone.forOffsetMillis(system.getDefaultTimeZone().getRawOffset()))
      // ES dateHistogram bounds are inclusive while createdBefore parameter is exclusive
      .extendedBounds(new ExtendedBounds(startInclusive ? startTime : (startTime + 1), endTime - 1L));
    addEffortAggregationIfNeeded(query, dateHistogram);
    return Optional.of(dateHistogram);
  }

  private Optional<Long> getMinCreatedAt(Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    String facetNameAndField = IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT;
    SearchRequestBuilder esRequest = client
      .prepareSearch(INDEX_TYPE_ISSUE)
      .setSize(0);
    BoolQueryBuilder esFilter = boolQuery();
    filters.values().stream().filter(Objects::nonNull).forEach(esFilter::must);
    if (esFilter.hasClauses()) {
      esRequest.setQuery(QueryBuilders.boolQuery().must(esQuery).filter(esFilter));
    } else {
      esRequest.setQuery(esQuery);
    }
    esRequest.addAggregation(AggregationBuilders.min(facetNameAndField).field(facetNameAndField));
    Min minValue = esRequest.get().getAggregations().get(facetNameAndField);

    Double actualValue = minValue.getValue();
    if (actualValue.isInfinite()) {
      return Optional.empty();
    }
    return Optional.of(actualValue.longValue());
  }

  private void addAssignedToMeFacetIfNeeded(SearchRequestBuilder builder, SearchOptions options, IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder queryBuilder) {
    String login = userSession.getLogin();

    if (!options.getFacets().contains(FACET_ASSIGNED_TO_ME) || StringUtils.isEmpty(login)) {
      return;
    }

    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE;
    String facetName = FACET_ASSIGNED_TO_ME;

    // Same as in super.stickyFacetBuilder
    StickyFacetBuilder assignedToMeFacetBuilder = newStickyFacetBuilder(query, filters, queryBuilder);
    BoolQueryBuilder facetFilter = assignedToMeFacetBuilder.getStickyFacetFilter(IS_ASSIGNED_FILTER, fieldName);

    FilterAggregationBuilder facetTopAggregation = AggregationBuilders
      .filter(facetName + "__filter", facetFilter)
      .subAggregation(addEffortAggregationIfNeeded(query, AggregationBuilders.terms(facetName + "__terms")
        .field(fieldName)
        .includeExclude(new IncludeExclude(escapeSpecialRegexChars(login), null))));

    builder.addAggregation(
      AggregationBuilders.global(facetName)
        .subAggregation(facetTopAggregation));
  }

  public List<String> listTags(@Nullable OrganizationDto organization, @Nullable String textQuery, int size) {
    int maxPageSize = 500;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    if (size <= 0) {
      return emptyList();
    }

    BoolQueryBuilder esQuery = boolQuery()
      .filter(createAuthorizationFilter(true));
    if (organization != null) {
      esQuery.filter(termQuery(FIELD_ISSUE_ORGANIZATION_UUID, organization.getUuid()));
    }

    SearchRequestBuilder requestBuilder = client
      .prepareSearch(INDEX_TYPE_ISSUE)
      .setQuery(esQuery)
      .setSize(0);

    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(AGGREGATION_NAME_FOR_TAGS)
      .field(IssueIndexDefinition.FIELD_ISSUE_TAGS)
      .size(size)
      .order(Terms.Order.term(true))
      .minDocCount(1L);
    if (textQuery != null) {
      String escapedTextQuery = escapeSpecialRegexChars(textQuery);
      termsAggregation.includeExclude(new IncludeExclude(format(SUBSTRING_MATCH_REGEXP, escapedTextQuery), null));
    }
    requestBuilder.addAggregation(termsAggregation);

    SearchResponse searchResponse = requestBuilder.get();
    Terms issuesResult = searchResponse.getAggregations().get(AGGREGATION_NAME_FOR_TAGS);
    return EsUtils.termsKeys(issuesResult);
  }

  public Map<String, Long> countTags(IssueQuery query, int maxNumberOfTags) {
    Terms terms = listTermsMatching(IssueIndexDefinition.FIELD_ISSUE_TAGS, query, null, Terms.Order.count(false), maxNumberOfTags);
    return EsUtils.termsToMap(terms);
  }

  public List<String> listAuthors(IssueQuery query, @Nullable String textQuery, int maxNumberOfAuthors) {
    Terms terms = listTermsMatching(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query, textQuery, Terms.Order.term(true), maxNumberOfAuthors);
    return EsUtils.termsKeys(terms);
  }

  private Terms listTermsMatching(String fieldName, IssueQuery query, @Nullable String textQuery, Terms.Order termsOrder, int maxNumberOfTags) {
    SearchRequestBuilder requestBuilder = client
      .prepareSearch(INDEX_TYPE_ISSUE)
      // Avoids returning search hits
      .setSize(0);

    requestBuilder.setQuery(boolQuery().must(QueryBuilders.matchAllQuery()).filter(createBoolFilter(query)));

    TermsAggregationBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(fieldName)
      .size(maxNumberOfTags)
      .order(termsOrder)
      .minDocCount(1L);
    if (textQuery != null) {
      aggreg.includeExclude(new IncludeExclude(format(SUBSTRING_MATCH_REGEXP, escapeSpecialRegexChars(textQuery)), null));
    }

    SearchResponse searchResponse = requestBuilder.addAggregation(aggreg).get();
    return searchResponse.getAggregations().get("_ref");
  }

  private BoolQueryBuilder createBoolFilter(IssueQuery query) {
    BoolQueryBuilder boolQuery = boolQuery();
    for (QueryBuilder filter : createFilters(query).values()) {
      if (filter != null) {
        boolQuery.must(filter);
      }
    }
    return boolQuery;
  }

  public List<ProjectStatistics> searchProjectStatistics(List<String> projectUuids, List<Long> froms, String assignee) {
    checkState(projectUuids.size() == froms.size(),
      "Expected same size for projectUuids (had size %s) and froms (had size %s)", projectUuids.size(), froms.size());
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }
    SearchRequestBuilder request = client.prepareSearch(IssueIndexDefinition.INDEX_TYPE_ISSUE)
      .setQuery(
        boolQuery()
          .mustNot(existsQuery(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION))
          .filter(termQuery(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, assignee)))
      .setSize(0);
    IntStream.range(0, projectUuids.size()).forEach(i -> {
      String projectUuid = projectUuids.get(i);
      long from = froms.get(i);
      request
        .addAggregation(AggregationBuilders
          .filter(projectUuid, boolQuery()
            .filter(termQuery(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectUuid))
            .filter(rangeQuery(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT).gte(epochMillisToEpochSeconds(from))))
          .subAggregation(
            AggregationBuilders.terms("branchUuid").field(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID)
              .subAggregation(
                AggregationBuilders.count("count").field(IssueIndexDefinition.FIELD_ISSUE_KEY))
              .subAggregation(
                AggregationBuilders.max("maxFuncCreatedAt").field(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT))));
    });
    SearchResponse response = request.get();
    return response.getAggregations().asList().stream()
      .map(x -> (InternalFilter) x)
      .flatMap(projectBucket -> ((StringTerms) projectBucket.getAggregations().get("branchUuid")).getBuckets().stream()
        .flatMap(branchBucket -> {
          long count = ((InternalValueCount) branchBucket.getAggregations().get("count")).getValue();
          if (count < 1L) {
            return Stream.empty();
          }
          long lastIssueDate = (long) ((InternalMax) branchBucket.getAggregations().get("maxFuncCreatedAt")).getValue();
          return Stream.of(new ProjectStatistics(branchBucket.getKeyAsString(), count, lastIssueDate));
        }))
      .collect(MoreCollectors.toList(projectUuids.size()));
  }

  public List<BranchStatistics> searchBranchStatistics(String projectUuid, List<String> branchUuids) {
    if (branchUuids.isEmpty()) {
      return Collections.emptyList();
    }

    SearchRequestBuilder request = client.prepareSearch(IssueIndexDefinition.INDEX_TYPE_ISSUE)
      .setRouting(projectUuid)
      .setQuery(
        boolQuery()
          .must(termsQuery(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID, branchUuids))
          .mustNot(existsQuery(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION))
          .must(termQuery(IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(false))))
      .setSize(0)
      .addAggregation(AggregationBuilders.terms("branchUuids")
        .field(IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID)
        .size(branchUuids.size())
        .subAggregation(AggregationBuilders.terms("types")
          .field(IssueIndexDefinition.FIELD_ISSUE_TYPE)));
    SearchResponse response = request.get();
    return ((StringTerms) response.getAggregations().get("branchUuids")).getBuckets().stream()
      .map(bucket -> new BranchStatistics(bucket.getKeyAsString(),
        ((StringTerms) bucket.getAggregations().get("types")).getBuckets()
          .stream()
          .collect(uniqueIndex(StringTerms.Bucket::getKeyAsString, InternalTerms.Bucket::getDocCount))))
      .collect(MoreCollectors.toList(branchUuids.size()));
  }

}
