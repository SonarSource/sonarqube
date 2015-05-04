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
package org.sonar.server.issue.index;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.global.GlobalBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.joda.time.Duration;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.es.*;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.StickyFacetBuilder;
import org.sonar.server.user.UserSession;
import org.sonar.server.view.index.ViewIndexDefinition;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

/**
 * The unique entry-point to interact with Elasticsearch index "issues".
 * All the requests are listed here.
 */
public class IssueIndex extends BaseIndex {


  private static final int SCROLL_TIME_IN_MINUTES = 3;

  private static final String SUBSTRING_MATCH_REGEXP = ".*%s.*";

  public static final List<String> SUPPORTED_FACETS = ImmutableList.of(
    IssueFilterParameters.SEVERITIES,
    IssueFilterParameters.STATUSES,
    IssueFilterParameters.RESOLUTIONS,
    IssueFilterParameters.ACTION_PLANS,
    IssueFilterParameters.PROJECT_UUIDS,
    IssueFilterParameters.RULES,
    IssueFilterParameters.ASSIGNEES,
    IssueFilterParameters.FACET_ASSIGNED_TO_ME,
    IssueFilterParameters.REPORTERS,
    IssueFilterParameters.AUTHORS,
    IssueFilterParameters.MODULE_UUIDS,
    IssueFilterParameters.FILE_UUIDS,
    IssueFilterParameters.DIRECTORIES,
    IssueFilterParameters.LANGUAGES,
    IssueFilterParameters.TAGS,
    IssueFilterParameters.CREATED_AT);

  // TODO to be documented
  // TODO move to Facets ?
  private static final String FACET_SUFFIX_MISSING = "_missing";

  private static final String IS_ASSIGNED_FILTER = "__isAssigned";

  private static final int DEFAULT_FACET_SIZE = 15;
  private static final Duration TWENTY_DAYS = Duration.standardDays(20L);
  private static final Duration TWENTY_WEEKS = Duration.standardDays(20L * 7L);
  private static final Duration TWENTY_MONTHS = Duration.standardDays(20L * 30L);

  /**
   * Convert an Elasticsearch result (a map) to an {@link org.sonar.server.issue.index.IssueDoc}. It's
   * used for {@link org.sonar.server.es.SearchResult}.
   */
  private static final Function<Map<String, Object>, IssueDoc> DOC_CONVERTER = new NonNullInputFunction<Map<String, Object>, IssueDoc>() {
    @Override
    protected IssueDoc doApply(Map<String, Object> input) {
      return new IssueDoc(input);
    }
  };

  private final Sorting sorting;
  private final System2 system;

  public IssueIndex(EsClient client, System2 system) {
    super(client);

    this.system = system;
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

    // by default order by updated date and issue key (in order to be deterministic when same ms)
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT).reverse();
    this.sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  /**
   * Warning, this method is not efficient as routing (the project uuid) is not known.
   * All the ES cluster nodes are involved.
   */
  @CheckForNull
  public IssueDoc getNullableByKey(String key) {
    SearchResult<IssueDoc> result = search(IssueQuery.builder().issueKeys(newArrayList(key)).build(), new SearchOptions());
    if (result.getTotal() == 1) {
      return result.getDocs().get(0);
    }
    return null;
  }

  /**
   * Warning, see {@link #getNullableByKey(String)}.
   * A {@link org.sonar.server.exceptions.NotFoundException} is thrown if key does not exist.
   */
  public IssueDoc getByKey(String key) {
    IssueDoc value = getNullableByKey(key);
    if (value == null) {
      throw new NotFoundException(String.format("Issue with key '%s' does not exist", key));
    }
    return value;
  }

  public SearchResult<IssueDoc> search(IssueQuery query, SearchOptions options) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE);

    configureSorting(query, requestBuilder);
    configurePagination(options, requestBuilder);

    QueryBuilder esQuery = QueryBuilders.matchAllQuery();
    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();
    Map<String, FilterBuilder> filters = createFilters(query);
    for (FilterBuilder filter : filters.values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }
    if (esFilter.hasClauses()) {
      requestBuilder.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      requestBuilder.setQuery(esQuery);
    }

    configureStickyFacets(query, options, filters, esQuery, requestBuilder);
    return new SearchResult<>(requestBuilder.get(), DOC_CONVERTER);
  }

  private void configureSorting(IssueQuery query, SearchRequestBuilder esRequest) {
    String sortField = query.sort();
    if (sortField != null) {
      boolean asc = BooleanUtils.isTrue(query.asc());
      sorting.fill(esRequest, sortField, asc);
    } else {
      sorting.fillDefault(esRequest);
    }
  }

  protected void configurePagination(SearchOptions options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset()).setSize(options.getLimit());
  }

  private Map<String, FilterBuilder> createFilters(IssueQuery query) {
    Map<String, FilterBuilder> filters = new HashMap<>();
    filters.put("__authorization", createAuthorizationFilter(query.checkAuthorization(), query.userLogin(), query.userGroups()));

    // Issue is assigned Filter
    if (BooleanUtils.isTrue(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, FilterBuilders.existsFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
    } else if (BooleanUtils.isFalse(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, FilterBuilders.missingFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
    }

    // Issue is planned Filter
    String isPlanned = "__isPlanned";
    if (BooleanUtils.isTrue(query.planned())) {
      filters.put(isPlanned, FilterBuilders.existsFilter(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN));
    } else if (BooleanUtils.isFalse(query.planned())) {
      filters.put(isPlanned, FilterBuilders.missingFilter(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN));
    }

    // Issue is Resolved Filter
    String isResolved = "__isResolved";
    if (BooleanUtils.isTrue(query.resolved())) {
      filters.put(isResolved, FilterBuilders.existsFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION));
    } else if (BooleanUtils.isFalse(query.resolved())) {
      filters.put(isResolved, FilterBuilders.missingFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION));
    }

    // Field Filters
    filters.put(IssueIndexDefinition.FIELD_ISSUE_KEY, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_KEY, query.issueKeys()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, query.actionPlans()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, query.assignees()));

    addComponentRelatedFilters(query, filters);

    filters.put(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_TAGS, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_TAGS, query.tags()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_REPORTER, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_REPORTER, query.reporters()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, query.rules()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, query.severities()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_STATUS, createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_STATUS, query.statuses()));

    addDatesFilter(filters, query);

    return filters;
  }

  private void addComponentRelatedFilters(IssueQuery query, Map<String, FilterBuilder> filters) {
    FilterBuilder viewFilter = createViewFilter(query.viewUuids());
    FilterBuilder componentFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.componentUuids());
    FilterBuilder projectFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids());
    FilterBuilder moduleRootFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, query.moduleRootUuids());
    FilterBuilder moduleFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, query.moduleUuids());
    FilterBuilder directoryFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, query.directories());
    FilterBuilder fileFilter = createTermsFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.fileUuids());

    if (BooleanUtils.isTrue(query.onComponentOnly())) {
      filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, componentFilter);
    } else {
      filters.put("__view", viewFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectFilter);
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
  private FilterBuilder createViewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }

    OrFilterBuilder viewsFilter = FilterBuilders.orFilter();
    for (String viewUuid : viewUuids) {
      viewsFilter.add(FilterBuilders.termsLookupFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID)
        .lookupIndex(ViewIndexDefinition.INDEX)
        .lookupType(ViewIndexDefinition.TYPE_VIEW)
        .lookupId(viewUuid)
        .lookupPath(ViewIndexDefinition.FIELD_PROJECTS))
        .cacheKey(viewsLookupCacheKey(viewUuid));
    }
    return viewsFilter;
  }

  public static String viewsLookupCacheKey(String viewUuid) {
    return String.format("%s%s%s", IssueIndexDefinition.TYPE_ISSUE, viewUuid, ViewIndexDefinition.TYPE_VIEW);
  }

  private FilterBuilder createAuthorizationFilter(boolean checkAuthorization, @Nullable String userLogin, Set<String> userGroups) {
    if (checkAuthorization) {
      OrFilterBuilder groupsAndUser = FilterBuilders.orFilter();
      if (userLogin != null) {
        groupsAndUser.add(FilterBuilders.termFilter(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, userLogin));
      }
      for (String group : userGroups) {
        groupsAndUser.add(FilterBuilders.termFilter(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, group));
      }
      return FilterBuilders.hasParentFilter(IssueIndexDefinition.TYPE_AUTHORIZATION,
        QueryBuilders.filteredQuery(
          QueryBuilders.matchAllQuery(),
          FilterBuilders.boolFilter()
            .must(groupsAndUser)
            .cache(true))
        );
    } else {
      return FilterBuilders.matchAllFilter();
    }
  }

  private void addDatesFilter(Map<String, FilterBuilder> filters, IssueQuery query) {
    Date createdAfter = query.createdAfter();
    Date createdBefore = query.createdBefore();

    validateCreationDateBounds(createdBefore, createdAfter);

    if (createdAfter != null) {
      filters.put("__createdAfter", FilterBuilders
        .rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .gt(createdAfter)
        .cache(false));
    }
    if (createdBefore != null) {
      filters.put("__createdBefore", FilterBuilders
        .rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .lt(createdBefore)
        .cache(false));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.put("__createdAt", FilterBuilders.termFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, createdAt).cache(false));
    }
  }

  private void validateCreationDateBounds(Date createdBefore, Date createdAfter) {
    Preconditions.checkArgument(createdAfter == null || createdAfter.before(new Date(system.now())),
      "Start bound cannot be in the future");
    Preconditions.checkArgument(createdAfter == null || createdAfter.equals(createdBefore) || createdBefore == null || createdAfter.before(createdBefore),
      "Start bound cannot be larger than end bound");
  }

  private void configureStickyFacets(IssueQuery query, SearchOptions options, Map<String, FilterBuilder> filters, QueryBuilder esQuery, SearchRequestBuilder esSearch) {
    if (!options.getFacets().isEmpty()) {
      StickyFacetBuilder stickyFacetBuilder = new StickyFacetBuilder(esQuery, filters);
      // Execute Term aggregations
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.SEVERITIES, IssueIndexDefinition.FIELD_ISSUE_SEVERITY, Severity.ALL.toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.STATUSES, IssueIndexDefinition.FIELD_ISSUE_STATUS, Issue.STATUSES.toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.PROJECT_UUIDS, IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.MODULE_UUIDS, IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, query.moduleUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.DIRECTORIES, IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, query.directories().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.FILE_UUIDS, IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.fileUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.LANGUAGES, IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.RULES, IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, query.rules().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.REPORTERS, IssueIndexDefinition.FIELD_ISSUE_REPORTER);

      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.AUTHORS, IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query.authors().toArray());

      if (options.getFacets().contains(IssueFilterParameters.TAGS)) {
        esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(IssueIndexDefinition.FIELD_ISSUE_TAGS, IssueFilterParameters.TAGS, query.tags().toArray()));
      }

      if (options.getFacets().contains(IssueFilterParameters.RESOLUTIONS)) {
        esSearch.addAggregation(createResolutionFacet(filters, esQuery));
      }
      if (options.getFacets().contains(IssueFilterParameters.ASSIGNEES)) {
        esSearch.addAggregation(createAssigneesFacet(query, filters, esQuery));
      }
      addAssignedToMeFacetIfNeeded(esSearch, options, query, filters, esQuery);
      if (options.getFacets().contains(IssueFilterParameters.ACTION_PLANS)) {
        esSearch.addAggregation(createActionPlansFacet(query, filters, esQuery));
      }
      if (options.getFacets().contains(IssueFilterParameters.CREATED_AT)) {
        esSearch.addAggregation(getCreatedAtFacet(query, filters, esQuery));
      }
    }
  }

  private void addSimpleStickyFacetIfNeeded(SearchOptions options, StickyFacetBuilder stickyFacetBuilder, SearchRequestBuilder esSearch,
    String facetName, String fieldName, Object... selectedValues) {
    if (options.getFacets().contains(facetName)) {
      esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(fieldName, facetName, DEFAULT_FACET_SIZE, selectedValues));
    }
  }

  private AggregationBuilder getCreatedAtFacet(IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    long now = system.now();

    String timeZoneString = system.getDefaultTimeZone().getID();

    DateHistogram.Interval bucketSize = DateHistogram.Interval.YEAR;
    Date createdAfter = query.createdAfter();
    long startTime = createdAfter == null ? getMinCreatedAt(filters, esQuery) : createdAfter.getTime();
    Date createdBefore = query.createdBefore();
    long endTime = createdBefore == null ? now : createdBefore.getTime();

    Duration timeSpan = new Duration(startTime, endTime);

    if (timeSpan.isShorterThan(TWENTY_DAYS)) {
      bucketSize = DateHistogram.Interval.DAY;
    } else if (timeSpan.isShorterThan(TWENTY_WEEKS)) {
      bucketSize = DateHistogram.Interval.WEEK;
    } else if (timeSpan.isShorterThan(TWENTY_MONTHS)) {
      bucketSize = DateHistogram.Interval.MONTH;
    }

    return AggregationBuilders.dateHistogram(IssueFilterParameters.CREATED_AT)
      .field(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
      .interval(bucketSize)
      .minDocCount(0L)
      .format(DateUtils.DATETIME_FORMAT)
      .preZone(timeZoneString)
      .postZone(timeZoneString)
      .extendedBounds(startTime, endTime);
  }

  private long getMinCreatedAt(Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String facetNameAndField = IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT;
    SearchRequestBuilder esRequest = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE)
      .setSearchType(SearchType.COUNT);
    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();
    for (FilterBuilder filter : filters.values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }
    if (esFilter.hasClauses()) {
      esRequest.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      esRequest.setQuery(esQuery);
    }
    esRequest.addAggregation(AggregationBuilders.min(facetNameAndField).field(facetNameAndField));
    Min minValue = esRequest.get().getAggregations().get(facetNameAndField);
    Double actualValue = minValue.getValue();
    if (actualValue.isInfinite()) {
      return Long.MIN_VALUE;
    } else {
      return actualValue.longValue();
    }
  }

  private AggregationBuilder createAssigneesFacet(IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder queryBuilder) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE;
    String facetName = IssueFilterParameters.ASSIGNEES;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> assigneeFilters = Maps.newHashMap(filters);
    assigneeFilters.remove(IS_ASSIGNED_FILTER);
    assigneeFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = new StickyFacetBuilder(queryBuilder, assigneeFilters);
    BoolFilterBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_FACET_SIZE);

    Collection<String> assigneesEscaped = escapeValuesForFacetInclusion(query.assignees());
    if (!assigneesEscaped.isEmpty()) {
      facetTopAggregation = assigneeFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, assigneesEscaped.toArray());
    }

    // Add missing facet for unassigned issues
    facetTopAggregation.subAggregation(
      AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)
      );

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  private Collection<String> escapeValuesForFacetInclusion(@Nullable Collection<String> values) {
    return values == null ? Arrays.<String>asList() : Collections2.transform(values, new Function<String, String>() {
        @Override
        public String apply(String input) {
          return Pattern.quote(input);
        }
      });
  }

  private void addAssignedToMeFacetIfNeeded(SearchRequestBuilder builder, SearchOptions options, IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder queryBuilder) {
    String login = UserSession.get().login();

    if (!options.getFacets().contains(IssueFilterParameters.FACET_ASSIGNED_TO_ME) || StringUtils.isEmpty(login)) {
      return;
    }

    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE;
    String facetName = IssueFilterParameters.FACET_ASSIGNED_TO_ME;

    // Same as in super.stickyFacetBuilder
    StickyFacetBuilder assignedToMeFacetBuilder = new StickyFacetBuilder(queryBuilder, filters);
    BoolFilterBuilder facetFilter = assignedToMeFacetBuilder.getStickyFacetFilter(IS_ASSIGNED_FILTER, fieldName);

    FilterAggregationBuilder facetTopAggregation = AggregationBuilders
      .filter(facetName + "__filter")
      .filter(facetFilter)
      .subAggregation(AggregationBuilders.terms(facetName + "__terms").field(fieldName).include(login));

    builder.addAggregation(
      AggregationBuilders.global(facetName)
        .subAggregation(facetTopAggregation));
  }

  private AggregationBuilder createResolutionFacet(Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
    String facetName = IssueFilterParameters.RESOLUTIONS;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> resolutionFilters = Maps.newHashMap(filters);
    resolutionFilters.remove("__isResolved");
    resolutionFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = new StickyFacetBuilder(esQuery, resolutionFilters);
    BoolFilterBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_FACET_SIZE);
    facetTopAggregation = assigneeFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, Issue.RESOLUTIONS.toArray());

    // Add missing facet for unresolved issues
    facetTopAggregation.subAggregation(
      AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)
      );

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  private AggregationBuilder createActionPlansFacet(IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN;
    String facetName = IssueFilterParameters.ACTION_PLANS;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> actionPlanFilters = Maps.newHashMap(filters);
    actionPlanFilters.remove("__isPlanned");
    actionPlanFilters.remove(fieldName);
    StickyFacetBuilder actionPlanFacetBuilder = new StickyFacetBuilder(esQuery, actionPlanFilters);
    BoolFilterBuilder facetFilter = actionPlanFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = actionPlanFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_FACET_SIZE);
    facetTopAggregation = actionPlanFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, query.actionPlans().toArray());

    // Add missing facet for unresolved issues
    facetTopAggregation.subAggregation(
      AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)
      );

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  @CheckForNull
  private FilterBuilder createTermsFilter(String field, Collection<?> values) {
    if (!values.isEmpty()) {
      return FilterBuilders.termsFilter(field, values);
    } else {
      return null;
    }
  }

  public List<String> listTags(IssueQuery query, @Nullable String textQuery, int maxNumberOfTags) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX, IndexDefinition.RULE.getIndexName())
      .setTypes(IssueIndexDefinition.TYPE_ISSUE, IndexDefinition.RULE.getIndexType());

    requestBuilder.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
      createBoolFilter(query)));

    GlobalBuilder topAggreg = AggregationBuilders.global("tags");
    String tagsOnIssuesSubAggregation = "tags__issues";
    String tagsOnRulesSubAggregation = "tags__rules";

    TermsBuilder issueTags = AggregationBuilders.terms(tagsOnIssuesSubAggregation)
      .field(IssueIndexDefinition.FIELD_ISSUE_TAGS)
      .size(maxNumberOfTags)
      .order(Terms.Order.term(true))
      .minDocCount(1L);
    if (textQuery != null) {
      issueTags.include(String.format(SUBSTRING_MATCH_REGEXP, textQuery));
    }
    TermsBuilder ruleTags = AggregationBuilders.terms(tagsOnRulesSubAggregation)
      .field(RuleNormalizer.RuleField.ALL_TAGS.field())
      .size(maxNumberOfTags)
      .order(Terms.Order.term(true))
      .minDocCount(1L);
    if (textQuery != null) {
      ruleTags.include(String.format(SUBSTRING_MATCH_REGEXP, textQuery));
    }

    SearchResponse searchResponse = requestBuilder.addAggregation(topAggreg.subAggregation(issueTags).subAggregation(ruleTags)).get();
    Global allTags = searchResponse.getAggregations().get("tags");
    SortedSet<String> result = Sets.newTreeSet();
    Terms issuesResult = allTags.getAggregations().get(tagsOnIssuesSubAggregation);
    Terms rulesResult = allTags.getAggregations().get(tagsOnRulesSubAggregation);
    result.addAll(EsUtils.termsKeys(issuesResult));
    result.addAll(EsUtils.termsKeys(rulesResult));
    List<String> resultAsList = Lists.newArrayList(result);
    return resultAsList.size() > maxNumberOfTags && maxNumberOfTags > 0 ? resultAsList.subList(0, maxNumberOfTags) : resultAsList;
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
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      // Avoids returning search hits
      .setSearchType(SearchType.COUNT)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE);

    requestBuilder.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
      createBoolFilter(query)));

    TermsBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(fieldName)
      .size(maxNumberOfTags)
      .order(termsOrder)
      .minDocCount(1L);
    if (textQuery != null) {
      aggreg.include(String.format(SUBSTRING_MATCH_REGEXP, textQuery));
    }

    SearchResponse searchResponse = requestBuilder.addAggregation(aggreg).get();
    return searchResponse.getAggregations().get("_ref");
  }

  public void deleteClosedIssuesOfProjectBefore(String projectUuid, Date beforeDate) {
    FilterBuilder projectFilter = FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectUuid));
    FilterBuilder dateFilter = FilterBuilders.rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT).lt(beforeDate.getTime());
    QueryBuilder queryBuilder = QueryBuilders.filteredQuery(
      QueryBuilders.matchAllQuery(),
      FilterBuilders.andFilter(projectFilter, dateFilter)
      );

    getClient().prepareDeleteByQuery(IssueIndexDefinition.INDEX).setQuery(queryBuilder).get();
  }

  private BoolFilterBuilder createBoolFilter(IssueQuery query) {
    BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
    for (FilterBuilder filter : createFilters(query).values()) {
      // TODO Can it be null ?
      if (filter != null) {
        boolFilter.must(filter);
      }
    }
    return boolFilter;
  }

  /**
   * TODO used only by tests, so must be replaced by EsTester#countDocuments()
   */
  public long countAll() {
    return getClient().prepareCount(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE)
      .get().getCount();
  }

  /**
   * Return non closed issues for a given project, module, or file. Other kind of components are not allowed.
   * Only fields needed for the batch are returned.
   */
  public Iterator<IssueDoc> selectIssuesForBatch(ComponentDto component) {
    BoolFilterBuilder filter = FilterBuilders.boolFilter()
      .must(createAuthorizationFilter(true, UserSession.get().login(), UserSession.get().userGroups()))
      .mustNot(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_STATUS, Issue.STATUS_CLOSED));

    switch (component.scope()) {
      case Scopes.PROJECT:
        filter.must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, component.uuid()));
        break;
      case Scopes.FILE:
        filter.must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, component.uuid()));
        break;
      default:
        throw new IllegalStateException(String.format("Component of scope '%s' is not allowed", component.scope()));
    }

    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE)
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .setSize(10000)
      .setFetchSource(
        new String[] {IssueIndexDefinition.FIELD_ISSUE_KEY, IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID,
          IssueIndexDefinition.FIELD_ISSUE_FILE_PATH, IssueIndexDefinition.FIELD_ISSUE_SEVERITY, IssueIndexDefinition.FIELD_ISSUE_MANUAL_SEVERITY,
          IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, IssueIndexDefinition.FIELD_ISSUE_STATUS, IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE,
          IssueIndexDefinition.FIELD_ISSUE_LINE, IssueIndexDefinition.FIELD_ISSUE_MESSAGE, IssueIndexDefinition.FIELD_ISSUE_CHECKSUM,
          IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT},
        null)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));
    SearchResponse response = requestBuilder.get();

    return scroll(response.getScrollId());
  }

  // Scrolling within the index
  private Iterator<IssueDoc> scroll(final String scrollId) {
    return new Iterator<IssueDoc>() {
      private final Queue<SearchHit> hits = new ArrayDeque<>();

      @Override
      public boolean hasNext() {
        if (hits.isEmpty()) {
          SearchScrollRequestBuilder esRequest = getClient()
            .prepareSearchScroll(scrollId)
            .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));
          Collections.addAll(hits, esRequest.get().getHits().getHits());
        }
        return !hits.isEmpty();
      }

      @Override
      public IssueDoc next() {
        if(!hasNext()){
          throw new NoSuchElementException();
        }
        return new IssueDoc(hits.poll().getSource());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Cannot remove item when scrolling");
      }
    };
  }
}
