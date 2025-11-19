/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.es.searchrequest;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;

import static com.google.common.base.Preconditions.checkState;

public class TopAggregationHelper {

  public static final Consumer<BoolQueryBuilder> NO_EXTRA_FILTER = t -> {
  };
  public static final Consumer<FilterAggregationBuilder> NO_OTHER_SUBAGGREGATION = t -> {
  };

  private final RequestFiltersComputer filterComputer;
  private final SubAggregationHelper subAggregationHelper;

  public TopAggregationHelper(RequestFiltersComputer filterComputer, SubAggregationHelper subAggregationHelper) {
    this.filterComputer = filterComputer;
    this.subAggregationHelper = subAggregationHelper;
  }

  /**
   * Creates a top-level aggregation that will be correctly scoped (ie. filtered) to aggregate on
   * {@code TopAggregationDefinition#getFieldName} given the Request filters and the other top-aggregations
   * (see {@link RequestFiltersComputer#getTopAggregationFilter(TopAggregationDefinition)}).
   * <p>
   * Optionally, the scope (ie. filter) of the aggregation can be further reduced by providing {@code extraFilters}.
   * <p>
   * Aggregations <strong>must</strong> be added to the top-level one by providing {@code subAggregations} otherwise
   * the aggregation will be empty and will yield no result.
   *
   * @param topAggregationName the name of the top-aggregation in the request
   * @param topAggregation properties of the top-aggregation
   * @param extraFilters optional extra filters which could further restrict the scope of computation of the
   *                     top-terms aggregation
   * @param subAggregations sub aggregation(s) to actually compute something
   *
   * @throws IllegalStateException if no sub-aggregation has been added
   * @return the aggregation, that can be added on top level of the elasticsearch request
   */
  public FilterAggregationBuilder buildTopAggregation(String topAggregationName, TopAggregationDefinition<?> topAggregation,
    Consumer<BoolQueryBuilder> extraFilters, Consumer<FilterAggregationBuilder> subAggregations) {
    BoolQueryBuilder filter = filterComputer.getTopAggregationFilter(topAggregation)
      .orElseGet(QueryBuilders::boolQuery);
    // optionally add extra filter(s)
    extraFilters.accept(filter);

    FilterAggregationBuilder res = AggregationBuilders.filter(topAggregationName, filter);
    subAggregations.accept(res);
    checkState(
      !res.getSubAggregations().isEmpty(),
      "no sub-aggregation has been added to top-aggregation %s", topAggregationName);
    return res;
  }

  /**
   * Same as {@link #buildTopAggregation(String, TopAggregationDefinition, Consumer, Consumer)} with built-in addition of a
   * top-term sub aggregation based field defined by {@link TopAggregationDefinition.FilterScope#getFieldName()} of
   * {@link TopAggregationDefinition#getFilterScope()}.
   */
  public FilterAggregationBuilder buildTermTopAggregation(
    String topAggregationName,
    TopAggregationDefinition<?> topAggregation,
    @Nullable Integer numberOfTerms,
    Consumer<BoolQueryBuilder> extraFilters,
    Consumer<FilterAggregationBuilder> otherSubAggregations
  ) {
    Consumer<FilterAggregationBuilder> subAggregations = t -> {
      t.subAggregation(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, numberOfTerms));
      otherSubAggregations.accept(t);
    };
    return buildTopAggregation(topAggregationName, topAggregation, extraFilters, subAggregations);
  }

  public FilterAggregationBuilder buildTermTopAggregation(String topAggregationName, TopAggregationDefinition<?> topAggregation,
    @Nullable Integer numberOfTerms, String filterNameToExclude) {
    BoolQueryBuilder filter = filterComputer.getPostFiltersExcluding(filterNameToExclude).orElseGet(QueryBuilders::boolQuery);
    FilterAggregationBuilder filterAgg = AggregationBuilders.filter(topAggregationName, filter);
    return filterAgg.subAggregation(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, numberOfTerms));
  }

  public SubAggregationHelper getSubAggregationHelper() {
    return subAggregationHelper;
  }
}
