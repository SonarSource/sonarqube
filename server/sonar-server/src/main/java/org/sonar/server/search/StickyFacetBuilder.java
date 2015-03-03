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
package org.sonar.server.search;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.Map;

public class StickyFacetBuilder {

  private static final int FACET_DEFAULT_MIN_DOC_COUNT = 1;
  private static final int FACET_DEFAULT_SIZE = 10;

  private final QueryBuilder query;
  private final Map<String, FilterBuilder> filters;

  public StickyFacetBuilder(QueryBuilder query, Map<String, FilterBuilder> filters) {
    this.query = query;
    this.filters = filters;
  }

  public QueryBuilder query() {
    return query;
  }

  public Map<String, FilterBuilder> filters() {
    return filters;
  }

  public AggregationBuilder buildStickyFacet(String fieldName, String facetName, Object... selected) {
    return buildStickyFacet(fieldName, facetName, FACET_DEFAULT_SIZE, selected);
  }

  public AggregationBuilder buildStickyFacet(String fieldName, String facetName, int size, Object... selected) {
    BoolFilterBuilder facetFilter = getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = buildTopFacetAggregation(fieldName, facetName, facetFilter, size);
    facetTopAggregation = addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, selected);

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  public BoolFilterBuilder getStickyFacetFilter(String... fieldNames) {
    BoolFilterBuilder facetFilter = FilterBuilders.boolFilter().must(FilterBuilders.queryFilter(query));
    for (Map.Entry<String, FilterBuilder> filter : filters.entrySet()) {
      if (filter.getValue() != null && !ArrayUtils.contains(fieldNames, filter.getKey())) {
        facetFilter.must(filter.getValue());
      }
    }
    return facetFilter;
  }

  public FilterAggregationBuilder buildTopFacetAggregation(String fieldName, String facetName, BoolFilterBuilder facetFilter, int size) {
    return AggregationBuilders
      .filter(facetName + "_filter")
      .filter(facetFilter)
      .subAggregation(
        AggregationBuilders.terms(facetName)
          .field(fieldName)
          .order(Terms.Order.count(false))
          .size(size)
          .minDocCount(FACET_DEFAULT_MIN_DOC_COUNT));
  }

  public FilterAggregationBuilder addSelectedItemsToFacet(String fieldName, String facetName, FilterAggregationBuilder facetTopAggregation, Object... selected) {
    if (selected.length > 0) {
      facetTopAggregation.subAggregation(
        AggregationBuilders.terms(facetName + "_selected")
          .field(fieldName)
          .include(Joiner.on('|').join(selected)));
    }
    return facetTopAggregation;
  }
}
