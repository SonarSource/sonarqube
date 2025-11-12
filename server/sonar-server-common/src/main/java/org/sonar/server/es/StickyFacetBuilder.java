/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.es;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.NamedValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;

import static java.lang.Math.max;

public class StickyFacetBuilder {

  private static final int FACET_DEFAULT_MIN_DOC_COUNT = 1;
  public static final int FACET_DEFAULT_SIZE = 10;

  /** In some cases the user selects >15 items for one facet. In that case, we want to calculate the doc count for all of them (not just the first 15 items, which would be the
   * default for the TermsAggregation). */
  private static final int MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED = 50;
  private static final Collector<CharSequence, ?, String> PIPE_JOINER = Collectors.joining("|");
  public static final String FILTER = "_filter";

  private final Query queryV2;
  private final Map<String, Query> filtersV2;
  private final Map<String, SortOrder> orderV2;

  public StickyFacetBuilder(Query query, Map<String, Query> filters, @Nullable Map<String, SortOrder> order) {
    this.queryV2 = query;
    this.filtersV2 = filters;
    this.orderV2 = order;
  }

  public Aggregation buildStickyFacetV2(String fieldName, String facetName, Object... selected) {
    return buildStickyFacetV2(fieldName, facetName, FACET_DEFAULT_SIZE, t -> t, selected);
  }

  public Aggregation buildStickyFacetV2(String fieldName, String facetName, int size, Object... selected) {
    return buildStickyFacetV2(fieldName, facetName, size, t -> t, selected);
  }

  /**
   * Creates an aggregation, that will return the top-terms for <code>fieldName</code>.
   *
   * It will filter according to the filters of every of the <em>other</em> fields, but will not apply filters to <em>this</em> field (so that the user can see all terms, even
   * after having chosen for one of the terms).
   *
   * If special filtering is required (like for nested types), additional functionality can be passed into the method in the <code>additionalAggregationFilter</code> parameter.
   *
   * @param fieldName the name of the field that contains the terms
   * @param facetName the name of the aggregation (use this for to find the corresponding results in the response)
   * @param size number of facet items
   * @param additionalAggregationFilter additional features (like filtering using childQuery)
   * @param selected the terms, that the user already has selected
   * @return the (global) aggregation, that can be added on top level of the elasticsearch request
   */
  public Aggregation buildStickyFacetV2(String fieldName, String facetName, int size, UnaryOperator<Aggregation> additionalAggregationFilter,
    Object... selected) {
    Query facetFilter = getStickyFacetFilterV2(fieldName);
    Aggregation facetTopAggregation = buildTopFacetAggregationV2(fieldName, facetName, facetFilter, size, additionalAggregationFilter);
    facetTopAggregation = addSelectedItemsToFacetV2(fieldName, facetName, facetFilter, facetTopAggregation, additionalAggregationFilter, selected);

    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggs.put(facetName + FILTER, facetTopAggregation);

    return Aggregation.of(a -> a
      .global(g -> g)
      .aggregations(subAggs));
  }

  public Aggregation buildStickyFacetV2(String fieldName, String filterToExclude, String facetName, int size,
    UnaryOperator<Aggregation> additionalAggregationFilter, Object... selected) {
    Query facetFilter = getStickyFacetFilterV2(filterToExclude);
    Aggregation facetTopAggregation = buildTopFacetAggregationV2(fieldName, facetName, facetFilter, size, additionalAggregationFilter);
    facetTopAggregation = addSelectedItemsToFacetV2(fieldName, facetName, facetFilter, facetTopAggregation, additionalAggregationFilter, selected);

    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggs.put(facetName + FILTER, facetTopAggregation);

    return Aggregation.of(a -> a
      .global(g -> g)
      .aggregations(subAggs));
  }

  public Aggregation buildNestedAggregationStickyFacetV2(String parentFieldName, String childFieldName, String facetName, Aggregation additionalAggregationFilter) {
    Query facetFilter = getStickyFacetFilterV2(parentFieldName + "." + childFieldName, parentFieldName);

    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggs.put(facetName + FILTER, Aggregation.of(a -> a
      .filter(facetFilter)
      .aggregations(Map.of(facetName, additionalAggregationFilter))));

    return Aggregation.of(a -> a
      .global(g -> g)
      .aggregations(subAggs));
  }

  public Query getStickyFacetFilterV2(String... fieldNames) {
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
    boolQueryBuilder.must(queryV2);
    for (Map.Entry<String, Query> filter : filtersV2.entrySet()) {
      if (filter.getValue() != null && !ArrayUtils.contains(fieldNames, filter.getKey())) {
        boolQueryBuilder.must(filter.getValue());
      }
    }
    return Query.of(q -> q.bool(boolQueryBuilder.build()));
  }

  private Aggregation buildTopFacetAggregationV2(String fieldName, String facetName, Query facetFilter, int size,
    UnaryOperator<Aggregation> additionalAggregationFilter) {
    Aggregation termsAggregation = buildTermsFacetAggregationV2(fieldName, size);
    Aggregation improvedAggregation = additionalAggregationFilter.apply(termsAggregation);

    Map<String, Aggregation> subAggs = new HashMap<>();
    subAggs.put(facetName, improvedAggregation);

    return Aggregation.of(a -> a
      .filter(facetFilter)
      .aggregations(subAggs));
  }

  private Aggregation buildTermsFacetAggregationV2(String fieldName, int size) {
    return Aggregation.of(a -> a.terms(t -> {
      t.field(fieldName)
        .size(size)
        .minDocCount(FACET_DEFAULT_MIN_DOC_COUNT);

      if (orderV2 != null && !orderV2.isEmpty()) {
        List<NamedValue<SortOrder>> orderList = orderV2.entrySet().stream()
          .map(e -> new NamedValue<>(e.getKey(), e.getValue()))
          .toList();
        t.order(orderList);
      }

      return t;
    }));
  }

  public Aggregation addSelectedItemsToFacetV2(String fieldName, String facetName, Query facetFilter, Aggregation facetTopAggregation,
    UnaryOperator<Aggregation> additionalAggregationFilter, Object... selected) {
    if (selected.length <= 0) {
      return facetTopAggregation;
    }
    String includes = Arrays.stream(selected)
      .filter(Objects::nonNull)
      .map(s -> EsUtils.escapeSpecialRegexChars(s.toString()))
      .collect(PIPE_JOINER);

    Aggregation selectedTerms = Aggregation.of(a -> a.terms(t -> t
      .size(max(MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED, includes.length()))
      .field(fieldName)
      .include(i -> i.regexp(includes))));

    Aggregation improvedAggregation = additionalAggregationFilter.apply(selectedTerms);

    Map<String, Aggregation> existingSubAggs = new HashMap<>(facetTopAggregation.aggregations());
    existingSubAggs.put(facetName + "_selected", improvedAggregation);

    return Aggregation.of(a -> a
      .filter(facetFilter)
      .aggregations(existingSubAggs));
  }

}
