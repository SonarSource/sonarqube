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
package org.sonar.server.es;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;

import static java.lang.Math.max;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

public class StickyFacetBuilder {

  private static final int FACET_DEFAULT_MIN_DOC_COUNT = 1;
  private static final int FACET_DEFAULT_SIZE = 10;
  private static final Order FACET_DEFAULT_ORDER = Terms.Order.count(false);
  /** In some cases the user selects >15 items for one facet. In that case, we want to calculate the doc count for all of them (not just the first 15 items, which would be the
   * default for the TermsAggregation). */
  private static final int MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED = 50;
  private static final Collector<CharSequence, ?, String> PIPE_JOINER = Collectors.joining("|");

  private final QueryBuilder query;
  private final Map<String, QueryBuilder> filters;
  private final AbstractAggregationBuilder subAggregation;
  private final Order order;

  public StickyFacetBuilder(QueryBuilder query, Map<String, QueryBuilder> filters) {
    this(query, filters, null, FACET_DEFAULT_ORDER);
  }

  public StickyFacetBuilder(QueryBuilder query, Map<String, QueryBuilder> filters, @Nullable AbstractAggregationBuilder subAggregation, @Nullable Order order) {
    this.query = query;
    this.filters = filters;
    this.subAggregation = subAggregation;
    this.order = order;
  }

  public AggregationBuilder buildStickyFacet(String fieldName, String facetName, Object... selected) {
    return buildStickyFacet(fieldName, facetName, t -> t, selected);
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
   * @param additionalAggregationFilter additional features (like filtering using childQuery)
   * @param selected the terms, that the user already has selected
   * @return the (global) aggregation, that can be added on top level of the elasticsearch request
   */
  public AggregationBuilder buildStickyFacet(String fieldName, String facetName, Function<TermsAggregationBuilder, AggregationBuilder> additionalAggregationFilter,
    Object... selected) {
    return buildStickyFacet(fieldName, facetName, FACET_DEFAULT_SIZE, additionalAggregationFilter, selected);
  }

  public AggregationBuilder buildStickyFacet(String fieldName, String facetName, int size, Object... selected) {
    return buildStickyFacet(fieldName, facetName, size, t -> t, selected);
  }

  private AggregationBuilder buildStickyFacet(String fieldName, String facetName, int size, Function<TermsAggregationBuilder, AggregationBuilder> additionalAggregationFilter,
    Object... selected) {
    BoolQueryBuilder facetFilter = getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = buildTopFacetAggregation(fieldName, facetName, facetFilter, size, additionalAggregationFilter);
    facetTopAggregation = addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, additionalAggregationFilter, selected);

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  public BoolQueryBuilder getStickyFacetFilter(String... fieldNames) {
    BoolQueryBuilder facetFilter = boolQuery().must(query);
    for (Map.Entry<String, QueryBuilder> filter : filters.entrySet()) {
      if (filter.getValue() != null && !ArrayUtils.contains(fieldNames, filter.getKey())) {
        facetFilter.must(filter.getValue());
      }
    }
    return facetFilter;
  }

  public FilterAggregationBuilder buildTopFacetAggregation(String fieldName, String facetName, BoolQueryBuilder facetFilter, int size) {
    return buildTopFacetAggregation(fieldName, facetName, facetFilter, size, t -> t);
  }

  private FilterAggregationBuilder buildTopFacetAggregation(String fieldName, String facetName, BoolQueryBuilder facetFilter, int size,
    Function<TermsAggregationBuilder, AggregationBuilder> additionalAggregationFilter) {
    TermsAggregationBuilder termsAggregation = buildTermsFacetAggregation(fieldName, facetName, size);
    AggregationBuilder improvedAggregation = additionalAggregationFilter.apply(termsAggregation);
    return AggregationBuilders
      .filter(facetName + "_filter", facetFilter)
      .subAggregation(improvedAggregation);
  }

  private TermsAggregationBuilder buildTermsFacetAggregation(String fieldName, String facetName, int size) {
    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(facetName)
      .field(fieldName)
      .order(order)
      .size(size)
      .minDocCount(FACET_DEFAULT_MIN_DOC_COUNT);
    if (subAggregation != null) {
      termsAggregation = termsAggregation.subAggregation(subAggregation);
    }
    return termsAggregation;
  }

  public FilterAggregationBuilder addSelectedItemsToFacet(String fieldName, String facetName, FilterAggregationBuilder facetTopAggregation,
    Function<TermsAggregationBuilder, AggregationBuilder> additionalAggregationFilter, Object... selected) {
    if (selected.length <= 0) {
      return facetTopAggregation;
    }
    String includes = Arrays.stream(selected)
      .filter(Objects::nonNull)
      .map(s -> EsUtils.escapeSpecialRegexChars(s.toString()))
      .collect(PIPE_JOINER);

    TermsAggregationBuilder selectedTerms = AggregationBuilders.terms(facetName + "_selected")
      .size(max(MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED, includes.length()))
      .field(fieldName)
      .includeExclude(new IncludeExclude(includes, null));
    if (subAggregation != null) {
      selectedTerms = selectedTerms.subAggregation(subAggregation);
    }

    AggregationBuilder improvedAggregation = additionalAggregationFilter.apply(selectedTerms);
    facetTopAggregation.subAggregation(improvedAggregation);
    return facetTopAggregation;
  }

}
