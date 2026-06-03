/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.server.es.ES8QueryHelper;

import static com.google.common.base.Preconditions.checkState;

public class TopAggregationHelper {

  public static final Consumer<BoolQuery.Builder> NO_EXTRA_FILTER_V2 = b -> {
  };
  public static final Consumer<Map<String, Aggregation>> NO_OTHER_SUBAGGREGATION_V2 = m -> {
  };

  private final RequestFiltersComputer filterComputer;
  private final SubAggregationHelper subAggregationHelper;

  public TopAggregationHelper(RequestFiltersComputer filterComputer, SubAggregationHelper subAggregationHelper) {
    this.filterComputer = filterComputer;
    this.subAggregationHelper = subAggregationHelper;
  }

  public SubAggregationHelper getSubAggregationHelper() {
    return subAggregationHelper;
  }

  /**
   * ES 8 variant of {@link #buildTopAggregation(String, TopAggregationDefinition, Consumer, Consumer)}.
   * Returns an ES 8 {@link Aggregation} (a filter aggregation wrapping a filter {@link Query} and sub-aggregations).
   */
  public Aggregation buildTopAggregationV2(String topAggregationName, TopAggregationDefinition<?> topAggregation,
    Consumer<BoolQuery.Builder> extraFilters, Consumer<Map<String, Aggregation>> subAggregations) {
    Query filter = filterComputer.getTopAggregationFilterV2(topAggregation)
      .orElseGet(ES8QueryHelper::boolQuery);
    Query filterWithExtras = ES8QueryHelper.boolQuery(b -> {
      b.must(filter);
      extraFilters.accept(b);
    });

    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggregations.accept(subAggs);
    checkState(
      !subAggs.isEmpty(),
      "no sub-aggregation has been added to top-aggregation %s", topAggregationName);

    return Aggregation.of(a -> a
      .filter(filterWithExtras)
      .aggregations(subAggs));
  }

  /**
   * ES 8 variant of {@link #buildTermTopAggregation(String, TopAggregationDefinition, Integer, Consumer, Consumer)}.
   */
  public Aggregation buildTermTopAggregationV2(
    String topAggregationName,
    TopAggregationDefinition<?> topAggregation,
    @Nullable Integer numberOfTerms,
    Consumer<BoolQuery.Builder> extraFilters,
    Consumer<Map<String, Aggregation>> otherSubAggregations) {
    Consumer<Map<String, Aggregation>> subAggregations = subAggs -> {
      subAggs.put(topAggregationName, subAggregationHelper.buildTermsAggregationV2(topAggregation, numberOfTerms));
      otherSubAggregations.accept(subAggs);
    };
    return buildTopAggregationV2(topAggregationName, topAggregation, extraFilters, subAggregations);
  }

  public Aggregation buildTermTopAggregationV2(String topAggregationName, TopAggregationDefinition<?> topAggregation,
    @Nullable Integer numberOfTerms, String filterNameToExclude) {
    Query filter = filterComputer.getPostFiltersExcludingV2(filterNameToExclude).orElseGet(ES8QueryHelper::boolQuery);
    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggs.put(topAggregationName, subAggregationHelper.buildTermsAggregationV2(topAggregation, numberOfTerms));
    return Aggregation.of(a -> a
      .filter(filter)
      .aggregations(subAggs));
  }
}
