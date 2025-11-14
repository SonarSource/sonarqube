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
package org.sonar.server.es.searchrequest;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.Facets;

import static java.lang.Math.max;
import static java.util.Optional.of;

public class SubAggregationHelper {
  private static final int TERM_AGGREGATION_MIN_DOC_COUNT = 1;
  private static final BucketOrder ORDER_BY_BUCKET_SIZE_DESC = BucketOrder.count(false);
  /** In some cases the user selects >15 items for one facet. In that case, we want to calculate the doc count for all of them (not just the first 15 items, which would be the
   * default for the TermsAggregation). */
  private static final int MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED = 50;
  private static final Collector<CharSequence, ?, String> PIPE_JOINER = Collectors.joining("|");

  @CheckForNull
  private final AbstractAggregationBuilder<?> subAggregation;
  private final BucketOrder order;

  public SubAggregationHelper() {
    this(null, null);
  }

  public SubAggregationHelper(@Nullable AbstractAggregationBuilder<?> subAggregation) {
    this(subAggregation, null);
  }

  public SubAggregationHelper(@Nullable AbstractAggregationBuilder<?> subAggregation, @Nullable BucketOrder order) {
    this.subAggregation = subAggregation;
    this.order = order == null ? ORDER_BY_BUCKET_SIZE_DESC : order;
  }

  public TermsAggregationBuilder buildTermsAggregation(String name,
    TopAggregationDefinition<?> topAggregation, @Nullable Integer numberOfTerms) {
    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(name)
      .field(topAggregation.getFilterScope().getFieldName())
      .order(order)
      .minDocCount(TERM_AGGREGATION_MIN_DOC_COUNT);
    if (numberOfTerms != null) {
      termsAggregation.size(numberOfTerms);
    }
    if (subAggregation != null) {
      termsAggregation = termsAggregation.subAggregation(subAggregation);
    }
    return termsAggregation;
  }

  public <T> Optional<TermsAggregationBuilder> buildSelectedItemsAggregation(String name, TopAggregationDefinition<?> topAggregation, T[] selected) {
    if (selected.length <= 0) {
      return Optional.empty();
    }

    String includes = Arrays.stream(selected)
      .filter(Objects::nonNull)
      .map(s -> EsUtils.escapeSpecialRegexChars(s.toString()))
      .collect(PIPE_JOINER);

    TermsAggregationBuilder selectedTerms = AggregationBuilders.terms(name + Facets.SELECTED_SUB_AGG_NAME_SUFFIX)
      .size(max(MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED, includes.length()))
      .field(topAggregation.getFilterScope().getFieldName())
      .includeExclude(new IncludeExclude(includes, null));
    if (subAggregation != null) {
      selectedTerms = selectedTerms.subAggregation(subAggregation);
    }

    return of(selectedTerms);
  }
}
