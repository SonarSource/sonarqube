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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.SearchClient;
import org.sonar.server.search.StickyFacetBuilder;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class IssueIndex extends BaseIndex<Issue, FakeIssueDto, String> {

  private static final String FACET_SUFFIX_MISSING = "_missing";

  private static final int DEFAULT_ISSUE_FACET_SIZE = 5;

  private ListMultimap<String, IndexField> sortColumns = ArrayListMultimap.create();

  public IssueIndex(SearchClient client) {
    super(IndexDefinition.ISSUES, null, client);

    sortColumns.put(IssueQuery.SORT_BY_ASSIGNEE, IssueNormalizer.IssueField.ASSIGNEE);
    sortColumns.put(IssueQuery.SORT_BY_STATUS, IssueNormalizer.IssueField.STATUS);
    sortColumns.put(IssueQuery.SORT_BY_SEVERITY, IssueNormalizer.IssueField.SEVERITY_VALUE);
    sortColumns.put(IssueQuery.SORT_BY_CREATION_DATE, IssueNormalizer.IssueField.ISSUE_CREATED_AT);
    sortColumns.put(IssueQuery.SORT_BY_UPDATE_DATE, IssueNormalizer.IssueField.ISSUE_UPDATED_AT);
    sortColumns.put(IssueQuery.SORT_BY_CLOSE_DATE, IssueNormalizer.IssueField.ISSUE_CLOSE_DATE);
    sortColumns.put(IssueQuery.SORT_BY_FILE_LINE, IssueNormalizer.IssueField.PROJECT);
    sortColumns.put(IssueQuery.SORT_BY_FILE_LINE, IssueNormalizer.IssueField.FILE_PATH);
    sortColumns.put(IssueQuery.SORT_BY_FILE_LINE, IssueNormalizer.IssueField.LINE);
    sortColumns.put(IssueQuery.SORT_BY_FILE_LINE, IssueNormalizer.IssueField.KEY);
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
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

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
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

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
    return new Result<Issue>(this, response);
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
    FilterBuilder dateFilter = FilterBuilders.rangeFilter(IssueNormalizer.IssueField.ISSUE_CLOSE_DATE.field()).lt(beforeDate.getTime());
    QueryBuilder queryBuilder = QueryBuilders.filteredQuery(
      QueryBuilders.matchAllQuery(),
      FilterBuilders.andFilter(projectFilter, dateFilter)
      );

    getClient().prepareDeleteByQuery(getIndexName()).setQuery(queryBuilder).get();
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
    filters.put(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, matchFilter(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, query.componentRootUuids()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, matchFilter(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, query.componentUuids()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, matchFilter(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, query.projectUuids()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, matchFilter(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_TAGS, matchFilter(IssueIndexDefinition.FIELD_ISSUE_TAGS, query.tags()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, matchFilter(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_REPORTER, matchFilter(IssueIndexDefinition.FIELD_ISSUE_REPORTER, query.reporters()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, matchFilter(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, query.rules()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, matchFilter(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, query.severities()));
    filters.put(IssueIndexDefinition.FIELD_ISSUE_STATUS, matchFilter(IssueIndexDefinition.FIELD_ISSUE_STATUS, query.statuses()));

    addDatesFilter(filters, query);

    return filters;
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
        IssueFilterParameters.SEVERITIES, IssueNormalizer.IssueField.SEVERITY.field(), Severity.ALL.toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.STATUSES, IssueNormalizer.IssueField.STATUS.field(), Issue.STATUSES.toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.COMPONENT_UUIDS, IssueNormalizer.IssueField.COMPONENT.field(), query.componentUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.PROJECT_UUIDS, IssueNormalizer.IssueField.PROJECT.field(), query.projectUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.LANGUAGES, IssueNormalizer.IssueField.LANGUAGE.field(), query.languages().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.TAGS, IssueNormalizer.IssueField.TAGS.field(), query.tags().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.RULES, IssueNormalizer.IssueField.RULE_KEY.field(), query.rules().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch,
        IssueFilterParameters.REPORTERS, IssueNormalizer.IssueField.REPORTER.field());

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
    String fieldName = IssueNormalizer.IssueField.ASSIGNEE.field();
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
    String fieldName = IssueNormalizer.IssueField.RESOLUTION.field();
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
    String fieldName = IssueNormalizer.IssueField.ACTION_PLAN.field();
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

  private void setSorting(IssueQuery query, SearchRequestBuilder esSearch) {
    String sortField = query.sort();
    if (sortField != null) {
      Boolean asc = query.asc();
      List<IndexField> fields = toIndexFields(sortField);
      for (IndexField field : fields) {
        FieldSortBuilder sortBuilder = SortBuilders.fieldSort(field.sortField());
        // line is optional. When missing, it means zero.
        if (asc != null && asc) {
          sortBuilder.missing("_first");
          sortBuilder.order(SortOrder.ASC);
        } else {
          sortBuilder.missing("_last");
          sortBuilder.order(SortOrder.DESC);
        }
        esSearch.addSort(sortBuilder);
      }
    } else {
      esSearch.addSort(IssueNormalizer.IssueField.ISSUE_UPDATED_AT.sortField(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(IssueNormalizer.IssueField.KEY.sortField(), SortOrder.ASC);
    }
  }

  private List<IndexField> toIndexFields(String sort) {
    List<IndexField> fields = sortColumns.get(sort);
    if (fields != null) {
      return fields;
    }
    throw new IllegalStateException("Unknown sort field : " + sort);
  }

  protected void setPagination(QueryContext options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  @CheckForNull
  private FilterBuilder matchFilter(String field, @Nullable Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      return FilterBuilders.termsFilter(field, values);
    } else {
      return null;
    }
  }

  public Collection<String> listTagsMatching(String query, int pageSize) {
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
}
