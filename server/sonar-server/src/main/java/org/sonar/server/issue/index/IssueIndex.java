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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.SearchClient;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IssueIndex extends BaseIndex<IssueDoc, IssueDto, String> {

  public IssueIndex(IssueNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.ISSUES, normalizer, client);
  }

  @Override
  protected String getKeyValue(String keyString) {
    return keyString;
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build();
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

  public SearchResponse search(IssueQuery query, QueryOptions options) {

    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    BoolFilterBuilder esFilter = FilterBuilders.boolFilter();

    // Issue is assigned Filter
    if (query.assigned() != null && query.assigned()) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.ASSIGNEE.field()));
    }

    // Issue is planned Filter
    if (query.planned() != null && query.planned()) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.ACTION_PLAN.field()));
    }

    // Issue is Resolved Filter
    if (query.resolved() != null && query.resolved()) {
      esFilter.must(FilterBuilders.existsFilter(IssueNormalizer.IssueField.RESOLUTION.field()));
    }

    // Field Filters
    matchFilter(esFilter, IssueNormalizer.IssueField.ACTION_PLAN, query.actionPlans());
    matchFilter(esFilter, IssueNormalizer.IssueField.ASSIGNEE, query.assignees());
    matchFilter(esFilter, IssueNormalizer.IssueField.PROJECT, query.componentRoots());
    matchFilter(esFilter, IssueNormalizer.IssueField.COMPONENT, query.components());
    matchFilter(esFilter, IssueNormalizer.IssueField.KEY, query.issueKeys());
    // TODO need to either materialize the language or join with rule
    // query.languages(esFilter, IssueNormalizer.IssueField.L, query.issueKeys());
    matchFilter(esFilter, IssueNormalizer.IssueField.RESOLUTION, query.resolutions());
    matchFilter(esFilter, IssueNormalizer.IssueField.REPORTER, query.reporters());
    matchFilter(esFilter, IssueNormalizer.IssueField.RULE, query.rules());
    matchFilter(esFilter, IssueNormalizer.IssueField.SEVERITY, query.severities());
    matchFilter(esFilter, IssueNormalizer.IssueField.STATUS, query.statuses());

    // Date filters
    if (query.createdAfter() != null) {
      esFilter.must(FilterBuilders
        .rangeFilter(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field())
        .gte(query.createdAfter()));
    }
    if (query.createdBefore() != null) {
      esFilter.must(FilterBuilders
        .rangeFilter(IssueNormalizer.IssueField.ISSUE_CREATED_AT.field())
        .lte(query.createdBefore()));
    }
    // TODO match day bracket for day on createdAt
    // query.createdAt();

    QueryBuilder esQuery = QueryBuilders.matchAllQuery();

    if (esFilter.hasClauses()) {
      esSearch.setQuery(QueryBuilders.filteredQuery(esQuery, esFilter));
    } else {
      esSearch.setQuery(esQuery);
    }

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
    }

    // Sample Functional aggregation
    // esSearch.addAggregation(AggregationBuilders.sum("totalDuration")
    // .field(IssueNormalizer.IssueField.DEBT.field()));

    return getClient().execute(esSearch);
  }

  private void matchFilter(BoolFilterBuilder filter, IndexField field, Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      filter.must(FilterBuilders.termsFilter(field.field(), values));
    }
  }
}
