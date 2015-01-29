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
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.server.es.Sorting;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.search.*;
import org.sonar.server.user.UserSession;
import org.sonar.server.view.index.ViewIndexDefinition;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class IssueIndex extends BaseIndex<Issue, FakeIssueDto, String> {

  private static final String FILTER_COMPONENT_ROOT = "__componentRoot";

  private static final String FACET_SUFFIX_MISSING = "_missing";

  private static final int DEFAULT_ISSUE_FACET_SIZE = 5;
  private static final int TAGS_FACET_SIZE = 10;

  private final Sorting sorting;

  public IssueIndex(SearchClient client) {
    super(IndexDefinition.ISSUES, null, client);

    sorting = new Sorting();
    sorting.add(IssueQuery.SORT_BY_ASSIGNEE, IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE);
    sorting.add(IssueQuery.SORT_BY_STATUS, IssueIndexDefinition.FIELD_ISSUE_STATUS);
    sorting.add(IssueQuery.SORT_BY_SEVERITY, IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE);
    sorting.add(IssueQuery.SORT_BY_CREATION_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT);
    sorting.add(IssueQuery.SORT_BY_UPDATE_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT);
    sorting.add(IssueQuery.SORT_BY_CLOSE_DATE, IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT);
    sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
    sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_FILE_PATH);
    sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_LINE);
    sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE).reverse();
    sorting.add(IssueQuery.SORT_BY_FILE_LINE, IssueIndexDefinition.FIELD_ISSUE_KEY);

    // by default order by updated date and issue key (in order to be deterministic when same ms)
    sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT).reverse();
    sorting.addDefault(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  @Override
  protected void initializeIndex() {
    // replaced by IssueIndexDefinition
  }

  @Override
  protected String getKeyValue(String keyString) {
    return keyString;
  }

  @Override
  protected Map mapProperties() {
    throw new UnsupportedOperationException("Being refactored");
  }

  @Override
  protected Map mapKey() {
    throw new UnsupportedOperationException("Being refactored");
  }

  @Override
  protected IssueDoc toDoc(Map<String, Object> fields) {
    Preconditions.checkNotNull(fields, "Cannot construct Issue with null response");
    return new IssueDoc(fields);
  }

  @Override
  public Issue getNullableByKey(String key) {
    Result<Issue> result = search(IssueQuery.builder().issueKeys(newArrayList(key)).build(), new QueryContext());
    if (result.getTotal() == 1) {
      return result.getHits().get(0);
    }
    return null;
  }

  public List<FacetValue> listAssignees(IssueQuery query) {
    QueryContext queryContext = new QueryContext().setPage(1, 0);

    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE);

    QueryBuilder esQuery = QueryBuilders.matchAllQuery();
    BoolFilterBuilder esFilter = getFilter(query, queryContext);
    if (esFilter.hasClauses()) {
      esSearch.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      esSearch.setQuery(esQuery);
    }
    esSearch.addAggregation(AggregationBuilders.terms(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE)
      .size(Integer.MAX_VALUE)
      .field(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
    esSearch.addAggregation(AggregationBuilders.missing("notAssigned")
      .field(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));

    SearchResponse response = esSearch.get();
    Terms aggregation = (Terms) response.getAggregations().getAsMap().get(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE);
    List<FacetValue> facetValues = newArrayList();
    for (Terms.Bucket value : aggregation.getBuckets()) {
      facetValues.add(new FacetValue(value.getKey(), value.getDocCount()));
    }
    facetValues.add(new FacetValue("_notAssigned_", ((InternalMissing) response.getAggregations().get("notAssigned")).getDocCount()));

    return facetValues;
  }

  public Result<Issue> search(IssueQuery query, QueryContext options) {
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE);

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    setSorting(query, esSearch);
    setPagination(options, esSearch);

    QueryBuilder esQuery = QueryBuilders.matchAllQuery();
    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();
    Map<String, FilterBuilder> filters = getFilters(query, options);
    for (FilterBuilder filter : filters.values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }

    if (esFilter.hasClauses()) {
      esSearch.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      esSearch.setQuery(esQuery);
    }

    setFacets(query, options, filters, esQuery, esSearch);

    SearchResponse response = esSearch.get();
    return new Result<>(this, response);
  }

  private BoolFilterBuilder getFilter(IssueQuery query, QueryContext options) {
    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();
    for (FilterBuilder filter : getFilters(query, options).values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }
    return esFilter;
  }

  public void deleteClosedIssuesOfProjectBefore(String uuid, Date beforeDate) {
    FilterBuilder projectFilter = FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, uuid));
    FilterBuilder dateFilter = FilterBuilders.rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT).lt(beforeDate.getTime());
    QueryBuilder queryBuilder = QueryBuilders.filteredQuery(
      QueryBuilders.matchAllQuery(),
      FilterBuilders.andFilter(projectFilter, dateFilter)
      );

    getClient().prepareDeleteByQuery(IssueIndexDefinition.INDEX).setQuery(queryBuilder).get();
  }

  /* Build main filter (match based) */
  protected Map<String, FilterBuilder> getFilters(IssueQuery query, QueryContext options) {

    Map<String, FilterBuilder> filters = Maps.newHashMap();

    filters.put("__authorization", getAuthorizationFilter(options));

    // Issue is assigned Filter
    String isAssigned = "__isAssigned";
    if (BooleanUtils.isTrue(query.assigned())) {
      filters.put(isAssigned, FilterBuilders.existsFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
    } else if (BooleanUtils.isFalse(query.assigned())) {
      filters.put(isAssigned, FilterBuilders.missingFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE));
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
    filters.put(IssueIndexDefinition.FIELD_ISSUE_KEY, matchFilter(IssueIndexDefinition.FIELD_ISSUE_KEY, query.issueKeys()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, matchFilter(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, query.actionPlans()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, matchFilter(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, query.assignees()));

    addComponentRelatedFilters(query, filters);

    filters.put(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, matchFilter(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_TAGS, matchFilter(IssueIndexDefinition.FIELD_ISSUE_TAGS, query.tags()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, matchFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_REPORTER, matchFilter(IssueIndexDefinition.FIELD_ISSUE_REPORTER, query.reporters()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, matchFilter(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, matchFilter(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, query.rules()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, matchFilter(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, query.severities()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_STATUS, matchFilter(IssueIndexDefinition.FIELD_ISSUE_STATUS, query.statuses()));

    addDatesFilter(filters, query);

    return filters;
  }

  private void addComponentRelatedFilters(IssueQuery query, Map<String, FilterBuilder> filters) {
    FilterBuilder viewFilter = viewFilter(query.viewUuids());
    FilterBuilder componentFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.componentUuids());
    FilterBuilder projectFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids());
    FilterBuilder moduleRootFilter = moduleRootFilter(query.moduleRootUuids());
    FilterBuilder moduleFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, query.moduleUuids());
    FilterBuilder directoryRootFilter = directoryRootFilter(query.moduleUuids(), query.directories());
    FilterBuilder directoryFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, query.directories());
    FilterBuilder fileFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.fileUuids());

    if (BooleanUtils.isTrue(query.isContextualized())) {
      if (viewFilter != null) {
        filters.put(FILTER_COMPONENT_ROOT, viewFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      } else if (projectFilter != null) {
        filters.put(FILTER_COMPONENT_ROOT, projectFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      } else if (moduleRootFilter != null) {
        filters.put(FILTER_COMPONENT_ROOT, moduleRootFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      } else if (directoryRootFilter != null) {
        filters.put(FILTER_COMPONENT_ROOT, directoryRootFilter);
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      } else if (fileFilter != null) {
        filters.put(FILTER_COMPONENT_ROOT, fileFilter);
      } else if (componentFilter != null) {
        // Last resort, when component type is unknown
        filters.put(FILTER_COMPONENT_ROOT, componentFilter);
      }
    } else {
      if (query.onComponentOnly()) {
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, componentFilter);
      } else {
        filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, fileFilter);
      }
      filters.put(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, projectFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleFilter);
      filters.put(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
      filters.put("view", viewFilter);
    }
  }

  @CheckForNull
  private FilterBuilder moduleRootFilter(Collection<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      return null;
    }
    FilterBuilder componentFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, componentUuids);
    FilterBuilder modulePathFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, componentUuids);
    FilterBuilder compositeFilter = null;
    if (componentFilter != null) {
      if (modulePathFilter != null) {
        compositeFilter = FilterBuilders.orFilter(componentFilter, modulePathFilter);
      } else {
        compositeFilter = componentFilter;
      }
    } else if (modulePathFilter != null) {
      compositeFilter = modulePathFilter;
    }
    return compositeFilter;
  }

  @CheckForNull
  private FilterBuilder directoryRootFilter(Collection<String> moduleUuids, Collection<String> directoryPaths) {
    BoolFilterBuilder directoryTop = null;
    FilterBuilder moduleFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, moduleUuids);
    FilterBuilder directoryFilter = matchFilter(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, directoryPaths);
    if (moduleFilter != null) {
      directoryTop = FilterBuilders.boolFilter();
      directoryTop.must(moduleFilter);
    }
    if (directoryFilter != null) {
      if (directoryTop == null) {
        directoryTop = FilterBuilders.boolFilter();
      }
      directoryTop.must(directoryFilter);
    }
    return directoryTop;
  }

  @CheckForNull
  private FilterBuilder viewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }

    OrFilterBuilder viewsFilter = FilterBuilders.orFilter();
    for (String viewUuid : viewUuids) {
      viewsFilter.add(FilterBuilders.termsLookupFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID)
        .lookupIndex(ViewIndexDefinition.INDEX)
        .lookupType(ViewIndexDefinition.TYPE_VIEW)
        .lookupId(viewUuid)
        .lookupPath(ViewIndexDefinition.FIELD_PROJECTS));
    }
    return viewsFilter;
  }

  private FilterBuilder getAuthorizationFilter(QueryContext options) {
    String user = options.getUserLogin();
    Set<String> groups = options.getUserGroups();
    OrFilterBuilder groupsAndUser = FilterBuilders.orFilter();
    if (user != null) {
      groupsAndUser.add(FilterBuilders.termFilter(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS, user));
    }
    for (String group : groups) {
      groupsAndUser.add(FilterBuilders.termFilter(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS, group));
    }
    return FilterBuilders.hasParentFilter(IssueIndexDefinition.TYPE_AUTHORIZATION,
      QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter()
          .must(groupsAndUser)
          .cache(true))
      );
  }

  private void addDatesFilter(Map<String, FilterBuilder> filters, IssueQuery query) {
    Date createdAfter = query.createdAfter();
    if (createdAfter != null) {
      filters.put("__createdAfter", FilterBuilders
        .rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .gte(createdAfter));
    }
    Date createdBefore = query.createdBefore();
    if (createdBefore != null) {
      filters.put("__createdBefore", FilterBuilders
        .rangeFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT)
        .lte(createdBefore));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.put("__createdAt", FilterBuilders.termFilter(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, createdAt));
    }
  }

  private void setFacets(IssueQuery query, QueryContext options, Map<String, FilterBuilder> filters, QueryBuilder esQuery, SearchRequestBuilder esSearch) {
    if (options.isFacet()) {
      StickyFacetBuilder stickyFacetBuilder = stickyFacetBuilder(esQuery, filters);
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

      if (options.facets().contains(IssueFilterParameters.TAGS)) {
        esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(IssueIndexDefinition.FIELD_ISSUE_TAGS, IssueFilterParameters.TAGS, TAGS_FACET_SIZE, query.tags().toArray()));
      }

      if (options.facets().contains(IssueFilterParameters.RESOLUTIONS)) {
        esSearch.addAggregation(getResolutionFacet(filters, esQuery));
      }
      if (options.facets().contains(IssueFilterParameters.ASSIGNEES)) {
        esSearch.addAggregation(getAssigneesFacet(query, filters, esQuery));
      }
      if (options.facets().contains(IssueFilterParameters.ACTION_PLANS)) {
        esSearch.addAggregation(getActionPlansFacet(query, filters, esQuery));
      }
    }
  }

  private void addSimpleStickyFacetIfNeeded(QueryContext options, StickyFacetBuilder stickyFacetBuilder, SearchRequestBuilder esSearch,
    String facetName, String fieldName, Object... selectedValues) {
    if (options.facets().contains(facetName)) {
      esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(fieldName, facetName, DEFAULT_ISSUE_FACET_SIZE, selectedValues));
    }
  }

  private AggregationBuilder getAssigneesFacet(IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE;
    String facetName = IssueFilterParameters.ASSIGNEES;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> assigneeFilters = Maps.newHashMap(filters);
    assigneeFilters.remove("__isAssigned");
    assigneeFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = new StickyFacetBuilder(esQuery, assigneeFilters);
    BoolFilterBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_ISSUE_FACET_SIZE);
    List<String> assignees = Lists.newArrayList(query.assignees());

    UserSession session = UserSession.get();
    if (session.isLoggedIn()) {
      assignees.add(session.login());
    }
    facetTopAggregation = assigneeFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, assignees.toArray());

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

  private AggregationBuilder getResolutionFacet(Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
    String facetName = IssueFilterParameters.RESOLUTIONS;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> resolutionFilters = Maps.newHashMap(filters);
    resolutionFilters.remove("__isResolved");
    resolutionFilters.remove(fieldName);
    StickyFacetBuilder assigneeFacetBuilder = new StickyFacetBuilder(esQuery, resolutionFilters);
    BoolFilterBuilder facetFilter = assigneeFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = assigneeFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_ISSUE_FACET_SIZE);
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

  private AggregationBuilder getActionPlansFacet(IssueQuery query, Map<String, FilterBuilder> filters, QueryBuilder esQuery) {
    String fieldName = IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN;
    String facetName = IssueFilterParameters.ACTION_PLANS;

    // Same as in super.stickyFacetBuilder
    Map<String, FilterBuilder> actionPlanFilters = Maps.newHashMap(filters);
    actionPlanFilters.remove("__isPlanned");
    actionPlanFilters.remove(fieldName);
    StickyFacetBuilder actionPlanFacetBuilder = new StickyFacetBuilder(esQuery, actionPlanFilters);
    BoolFilterBuilder facetFilter = actionPlanFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = actionPlanFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, DEFAULT_ISSUE_FACET_SIZE);
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

  private void setSorting(IssueQuery query, SearchRequestBuilder esRequest) {
    String sortField = query.sort();
    if (sortField != null) {
      boolean asc = BooleanUtils.isTrue(query.asc());
      sorting.fill(esRequest, sortField, asc);
    } else {
      sorting.fillDefault(esRequest);
    }
  }

  protected void setPagination(QueryContext options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  @CheckForNull
  private FilterBuilder matchFilter(String field, Collection<?> values) {
    if (!values.isEmpty()) {
      return FilterBuilders.termsFilter(field, values);
    } else {
      return null;
    }
  }

  public Collection<String> listTagsMatching(@Nullable String query, int pageSize) {
    SearchRequestBuilder count = getClient().prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE)
      .setQuery(QueryBuilders.matchAllQuery());
    TermsBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(IssueIndexDefinition.FIELD_ISSUE_TAGS)
      .size(pageSize)
      .order(Order.term(true))
      .minDocCount(1L);
    if (query != null) {
      aggreg.include(".*" + query + ".*");
    }
    Terms result = count.addAggregation(aggreg).get().getAggregations().get("_ref");

    return Collections2.transform(result.getBuckets(), new Function<Bucket, String>() {
      @Override
      public String apply(Bucket bucket) {
        return bucket.getKey();
      }
    });
  }

  public Map<String, Long> listTagsForComponent(String componentUuid, int pageSize) {
    SearchRequestBuilder count = getClient().prepareSearch(IssueIndexDefinition.INDEX)
      .setTypes(IssueIndexDefinition.TYPE_ISSUE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter()
          .must(getAuthorizationFilter(new QueryContext()))
          .must(FilterBuilders.missingFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION))
          .must(moduleRootFilter(Arrays.asList(componentUuid)))));
    TermsBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(IssueIndexDefinition.FIELD_ISSUE_TAGS)
      .size(pageSize)
      .order(Order.count(false))
      .minDocCount(1L);
    Terms result = count.addAggregation(aggreg).get().getAggregations().get("_ref");

    Map<String, Long> map = Maps.newHashMap();
    for (Bucket bucket : result.getBuckets()) {
      map.put(bucket.getKey(), bucket.getDocCount());
    }
    return map;
  }
}
