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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.BooleanUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.issue.Issue;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.search.*;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class IssueIndex extends BaseIndex<Issue, IssueDto, String> {

  private Map<String, IndexField> sortColumns = newHashMap();

  public IssueIndex(IssueNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.ISSUES, normalizer, client);

    sortColumns.put(IssueQuery.SORT_BY_ASSIGNEE, IssueNormalizer.IssueField.ASSIGNEE);
    sortColumns.put(IssueQuery.SORT_BY_STATUS, IssueNormalizer.IssueField.STATUS);
    sortColumns.put(IssueQuery.SORT_BY_SEVERITY, IssueNormalizer.IssueField.SEVERITY_VALUE);
    sortColumns.put(IssueQuery.SORT_BY_CREATION_DATE, IssueNormalizer.IssueField.ISSUE_CREATED_AT);
    sortColumns.put(IssueQuery.SORT_BY_UPDATE_DATE, IssueNormalizer.IssueField.ISSUE_UPDATED_AT);
    sortColumns.put(IssueQuery.SORT_BY_CLOSE_DATE, IssueNormalizer.IssueField.ISSUE_CLOSE_DATE);
  }

  @Override
  protected ImmutableSettings.Builder addCustomIndexSettings(ImmutableSettings.Builder baseIndexSettings) {
    return baseIndexSettings.put("index.number_of_shards", 4);
  }

  @Override
  protected String getKeyValue(String keyString) {
    return keyString;
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : IssueNormalizer.IssueField.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  @Override
  protected Map mapDomain() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("dynamic", false);
    mapping.put("_all", ImmutableMap.of("enabled", false));
    mapping.put("_id", mapKey());
    mapping.put("_parent", mapParent());
    mapping.put("_routing", mapRouting());
    mapping.put("properties", mapProperties());
    return mapping;
  }

  private Object mapParent() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("type", getParentType());
    return mapping;
  }

  private String getParentType() {
    return IndexDefinition.ISSUES_AUTHORIZATION.getIndexType();
  }

  private Map mapRouting() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("required", true);
    mapping.put("path", IssueNormalizer.IssueField.PROJECT.field());
    return mapping;
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", IssueNormalizer.IssueField.KEY.field());
    return mapping;
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
    esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.ASSIGNEE.field())
      .size(Integer.MAX_VALUE)
      .field(IssueNormalizer.IssueField.ASSIGNEE.field()));
    esSearch.addAggregation(AggregationBuilders.missing("notAssigned")
      .field(IssueNormalizer.IssueField.ASSIGNEE.field()));

    SearchResponse response = getClient().execute(esSearch);
    Terms aggregation = (Terms) response.getAggregations().getAsMap().get(IssueNormalizer.IssueField.ASSIGNEE.field());
    List<FacetValue> facetValues = newArrayList();
    for (Terms.Bucket value : aggregation.getBuckets()) {
      facetValues.add(new FacetValue(value.getKey(), (int) value.getDocCount()));
    }
    facetValues.add(new FacetValue("_notAssigned_", (int) ((InternalMissing) response.getAggregations().get("notAssigned")).getDocCount()));

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

    setFacets(options, esSearch);
    setSorting(query, esSearch);
    setPagination(options, esSearch);

    QueryBuilder esQuery = QueryBuilders.matchAllQuery();
    BoolFilterBuilder esFilter = getFilter(query, options);
    if (esFilter.hasClauses()) {
      esSearch.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      esSearch.setQuery(esQuery);
    }

    SearchResponse response = getClient().execute(esSearch);
    return new Result<Issue>(this, response);
  }

  /* Build main filter (match based) */
  protected BoolFilterBuilder getFilter(IssueQuery query, QueryContext options) {
    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();

    addAuthorizationFilter(esFilter, options);

    // Issue is assigned Filter
    if (BooleanUtils.isTrue(query.assigned())) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.ASSIGNEE.field()));
    } else if (BooleanUtils.isFalse(query.assigned())) {
      esFilter.must(FilterBuilders.missingFilter(IssueNormalizer.IssueField.ASSIGNEE.field()));
    }

    // Issue is planned Filter
    if (BooleanUtils.isTrue(query.planned())) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.ACTION_PLAN.field()));
    } else if (BooleanUtils.isFalse(query.planned())) {
      esFilter.must(FilterBuilders.missingFilter(IssueNormalizer.IssueField.ACTION_PLAN.field()));
    }

    // Issue is Resolved Filter
    if (BooleanUtils.isTrue(query.resolved())) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.RESOLUTION.field()));
    } else if (BooleanUtils.isFalse(query.resolved())) {
      esFilter.must(FilterBuilders.missingFilter(IssueNormalizer.IssueField.RESOLUTION.field()));
    }

    // Field Filters
    matchFilter(esFilter, IssueNormalizer.IssueField.KEY, query.issueKeys());
    matchFilter(esFilter, IssueNormalizer.IssueField.ACTION_PLAN, query.actionPlans());
    matchFilter(esFilter, IssueNormalizer.IssueField.ASSIGNEE, query.assignees());
    matchFilter(esFilter, IssueNormalizer.IssueField.PROJECT, query.componentRoots());
    matchFilter(esFilter, IssueNormalizer.IssueField.COMPONENT, query.components());
    matchFilter(esFilter, IssueNormalizer.IssueField.LANGUAGE, query.languages());
    matchFilter(esFilter, IssueNormalizer.IssueField.RESOLUTION, query.resolutions());
    matchFilter(esFilter, IssueNormalizer.IssueField.REPORTER, query.reporters());
    matchFilter(esFilter, IssueNormalizer.IssueField.RULE_KEY, query.rules());
    matchFilter(esFilter, IssueNormalizer.IssueField.SEVERITY, query.severities());
    matchFilter(esFilter, IssueNormalizer.IssueField.STATUS, query.statuses());

    addDatesFilter(esFilter, query);

    return esFilter;
  }

  private void addAuthorizationFilter(BoolFilterBuilder esFilter, QueryContext options) {
    String user = options.getUserLogin();
    Set<String> groups = options.getUserGroups();
    OrFilterBuilder groupsAndUser = FilterBuilders.orFilter();
    if (user != null) {
      groupsAndUser.add(FilterBuilders.termFilter(IssueAuthorizationNormalizer.IssueAuthorizationField.USERS.field(), user));
    }
    for (String group : groups) {
      groupsAndUser.add(FilterBuilders.termFilter(IssueAuthorizationNormalizer.IssueAuthorizationField.GROUPS.field(), group));
    }
    esFilter.must(FilterBuilders.hasParentFilter(IndexDefinition.ISSUES_AUTHORIZATION.getIndexType(),
      QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter()
          .must(FilterBuilders.termFilter(IssueAuthorizationNormalizer.IssueAuthorizationField.PERMISSION.field(), UserRole.USER), groupsAndUser)
          .cache(true))
      ));
  }

  private void addDatesFilter(BoolFilterBuilder esFilter, IssueQuery query) {
    Date createdAfter = query.createdAfter();
    if (createdAfter != null) {
      esFilter.must(FilterBuilders
        .rangeFilter(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field())
        .gte(createdAfter));
    }
    Date createdBefore = query.createdBefore();
    if (createdBefore != null) {
      esFilter.must(FilterBuilders
        .rangeFilter(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field())
        .lte(createdBefore));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      esFilter.must(FilterBuilders.termFilter(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field(), createdAt));
    }
  }

  private void setFacets(QueryContext options, SearchRequestBuilder esSearch) {
    if (options.isFacet()) {
      // Execute Term aggregations
      esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.SEVERITY.field())
        .field(IssueNormalizer.IssueField.SEVERITY.field()));
      esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.STATUS.field())
        .field(IssueNormalizer.IssueField.STATUS.field()));
      esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.RESOLUTION.field())
        .field(IssueNormalizer.IssueField.RESOLUTION.field()));
      esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.ACTION_PLAN.field())
        .field(IssueNormalizer.IssueField.ACTION_PLAN.field()));
      esSearch.addAggregation(AggregationBuilders.terms(IssueNormalizer.IssueField.PROJECT.field())
        .field(IssueNormalizer.IssueField.PROJECT.field()));
    }
  }

  private void setSorting(IssueQuery query, SearchRequestBuilder esSearch) {
    /* integrate Query Sort */
    String sortField = query.sort();
    Boolean asc = query.asc();
    if (sortField != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(toIndexField(sortField).sortField());
      if (asc != null && asc) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else {
      esSearch.addSort(IssueNormalizer.IssueField.ISSUE_UPDATED_AT.sortField(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(IssueNormalizer.IssueField.KEY.sortField(), SortOrder.ASC);
    }
  }

  private IndexField toIndexField(String sort) {
    IndexField indexFieldSort = sortColumns.get(sort);
    if (indexFieldSort != null) {
      return indexFieldSort;
    }
    throw new IllegalStateException("Unknown sort field : " + sort);
  }

  protected void setPagination(QueryContext options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  private void matchFilter(BoolFilterBuilder filter, IndexField field, @Nullable Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      filter.must(FilterBuilders.termsFilter(field.field(), values));
    }
  }
}
