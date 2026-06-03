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

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.util.NamedValue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.Facets;

import static java.lang.Math.max;
import static java.util.Optional.of;

public class SubAggregationHelper {
  private static final int TERM_AGGREGATION_MIN_DOC_COUNT = 1;
  private static final int MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED = 50;
  private static final Collector<CharSequence, ?, String> PIPE_JOINER = Collectors.joining("|");

  private static final String DEFAULT_SUB_AGGREGATION_NAME_V2 = "subAggregation";

  @CheckForNull
  private final Aggregation subAggregationV2;
  @CheckForNull
  private final List<NamedValue<SortOrder>> orderV2;
  private final String subAggregationNameV2;

  public SubAggregationHelper() {
    this((Aggregation) null, null);
  }

  public SubAggregationHelper(@Nullable Aggregation subAggregationV2, @Nullable List<NamedValue<SortOrder>> orderV2) {
    this(subAggregationV2, orderV2, DEFAULT_SUB_AGGREGATION_NAME_V2);
  }

  /**
   * @param subAggregationNameV2 the key under which the {@code subAggregationV2} will be registered. Must match any
   *   bucket-order reference in {@code orderV2} that points at the sub-aggregation (e.g. ordering buckets by an effort
   *   sum named "effort" requires the helper to register the sum under that same name).
   */
  public SubAggregationHelper(@Nullable Aggregation subAggregationV2, @Nullable List<NamedValue<SortOrder>> orderV2,
    String subAggregationNameV2) {
    this.subAggregationV2 = subAggregationV2;
    this.orderV2 = orderV2;
    this.subAggregationNameV2 = subAggregationNameV2;
  }

  public Aggregation buildTermsAggregationV2(TopAggregationDefinition<?> topAggregation, @Nullable Integer numberOfTerms) {
    return Aggregation.of(a -> {
      a.terms(t -> {
        t.field(topAggregation.getFilterScope().getFieldName())
          .minDocCount(TERM_AGGREGATION_MIN_DOC_COUNT);
        if (numberOfTerms != null) {
          t.size(numberOfTerms);
        }
        if (orderV2 != null && !orderV2.isEmpty()) {
          t.order(orderV2);
        } else {
          t.order(List.of(new NamedValue<>("_count", SortOrder.Desc)));
        }
        return t;
      });
      if (subAggregationV2 != null) {
        a.aggregations(subAggregationNameV2, subAggregationV2);
      }
      return a;
    });
  }

  public <T> Optional<Aggregation> buildSelectedItemsAggregationV2(TopAggregationDefinition<?> topAggregation, T[] selected) {
    if (selected.length <= 0) {
      return Optional.empty();
    }

    String includes = Arrays.stream(selected)
      .filter(Objects::nonNull)
      .map(s -> EsUtils.escapeSpecialRegexChars(s.toString()))
      .collect(PIPE_JOINER);

    Aggregation selectedTerms = Aggregation.of(a -> {
      a.terms(t -> t
        .size(max(MAXIMUM_NUMBER_OF_SELECTED_ITEMS_WHOSE_DOC_COUNT_WILL_BE_CALCULATED, includes.length()))
        .field(topAggregation.getFilterScope().getFieldName())
        .include(i -> i.regexp(includes)));
      if (subAggregationV2 != null) {
        a.aggregations(subAggregationNameV2, subAggregationV2);
      }
      return a;
    });

    return of(selectedTerms);
  }
}
