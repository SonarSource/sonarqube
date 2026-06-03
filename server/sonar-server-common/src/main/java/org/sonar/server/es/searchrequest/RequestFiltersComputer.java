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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.server.es.ES8QueryHelper;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Computes filters of a given ES search request given all the filters to apply and the top-aggregations to include in
 * the request:
 * <ul>
 *     <li>the ones for the query (see {@link #computeQueryFilter(AllFiltersImpl, Map) computeQueryFilter})</li>
 *     <li>the ones to apply as post filters (see {@link #computePostFilters(AllFiltersImpl, Set) computePostFilters})</li>
 *     <li>the ones for each top-aggregation (see {@link #getTopAggregationFilter(TopAggregationDefinition) getTopAggregationFilter})</li>
 * </ul>
 * <p>
 * To be able to provide accurate filters, all {@link TopAggregationDefinition} instances for which
 * {@link #getTopAggregationFilter(TopAggregationDefinition)} may be called, must all be declared in the constructor.
 */
public class RequestFiltersComputer {

  private static final String DUPLICATE_MESSAGE = "Duplicate: %s";

  private final Set<TopAggregationDefinition<?>> topAggregations;
  private final Map<FilterNameAndScope, Query> postFiltersV2;
  private final Map<FilterNameAndScope, Query> queryFiltersV2;

  public RequestFiltersComputer(AllFilters allFilters, Set<TopAggregationDefinition<?>> topAggregations) {
    this.topAggregations = ImmutableSet.copyOf(topAggregations);
    this.postFiltersV2 = computePostFiltersV2((AllFiltersImpl) allFilters, topAggregations);
    this.queryFiltersV2 = computeQueryFilterV2((AllFiltersImpl) allFilters, postFiltersV2);
  }

  public static AllFilters newAllFilters() {
    return new AllFiltersImpl();
  }

  private static Map<FilterNameAndScope, Query> computePostFiltersV2(AllFiltersImpl allFilters,
    Set<TopAggregationDefinition<?>> topAggregations) {
    Set<FilterScope> enabledStickyTopAggregationtedFieldNames = topAggregations.stream()
      .filter(TopAggregationDefinition::isSticky)
      .map(TopAggregationDefinition::getFilterScope)
      .collect(Collectors.toSet());

    Map<FilterNameAndScope, Query> res = new LinkedHashMap<>();
    allFilters.internalStreamV2()
      .filter(e -> enabledStickyTopAggregationtedFieldNames.contains(e.getKey().filterScope()))
      .forEach(e -> checkState(res.put(e.getKey(), e.getValue()) == null, DUPLICATE_MESSAGE, e.getKey()));
    return res;
  }

  private static Map<FilterNameAndScope, Query> computeQueryFilterV2(AllFiltersImpl allFilters,
    Map<FilterNameAndScope, Query> postFilters) {
    Set<FilterNameAndScope> postFilterKeys = postFilters.keySet();

    Map<FilterNameAndScope, Query> res = new LinkedHashMap<>();
    allFilters.internalStreamV2()
      .filter(e -> !postFilterKeys.contains(e.getKey()))
      .forEach(e -> checkState(res.put(e.getKey(), e.getValue()) == null, DUPLICATE_MESSAGE, e.getKey()));
    return res;
  }

  public Optional<Query> getQueryFiltersV2() {
    return toBoolQueryV2(this.queryFiltersV2, (e, v) -> true);
  }

  public Optional<Query> getPostFiltersV2() {
    return toBoolQueryV2(postFiltersV2, (e, v) -> true);
  }

  public Optional<Query> getTopAggregationFilterV2(TopAggregationDefinition<?> topAggregation) {
    checkArgument(topAggregations.contains(topAggregation), "topAggregation must have been declared in constructor");
    return toBoolQueryV2(
      postFiltersV2,
      (e, v) -> !topAggregation.isSticky() || !topAggregation.getFilterScope().intersect(e.filterScope()));
  }

  public Optional<Query> getPostFiltersExcludingV2(String filterNameToExclude) {
    return toBoolQueryV2(postFiltersV2, (e, v) -> !e.filterName().equals(filterNameToExclude));
  }

  private static Optional<Query> toBoolQueryV2(Map<FilterNameAndScope, Query> queryFilters,
    BiPredicate<FilterNameAndScope, Query> predicate) {
    if (queryFilters.isEmpty()) {
      return empty();
    }

    List<Query> selectQueries = queryFilters.entrySet().stream()
      .filter(e -> predicate.test(e.getKey(), e.getValue()))
      .map(Map.Entry::getValue)
      .toList();
    if (selectQueries.isEmpty()) {
      return empty();
    }

    return of(ES8QueryHelper.boolQuery(b -> selectQueries.forEach(b::must)));
  }

  /**
   * A mean to put together all filters which apply to a given Search request.
   */
  public interface AllFilters {

    /**
     * @throws IllegalArgumentException if a filter with the specified name has already been added
     */
    AllFilters addFilterV2(String name, FilterScope filterScope, @Nullable Query filter);

    Stream<Query> streamV2();
  }

  private static class AllFiltersImpl implements AllFilters {
    private final Map<FilterNameAndScope, Query> filtersV2 = new LinkedHashMap<>();

    @Override
    public AllFilters addFilterV2(String name, FilterScope filterScope, @Nullable Query filter) {
      requireNonNull(name, "name can't be null");
      requireNonNull(filterScope, "filterScope can't be null");

      if (filter == null) {
        return this;
      }

      checkArgument(
        filtersV2.put(new FilterNameAndScope(name, filterScope), filter) == null,
        "A filter with name %s has already been added", name);
      return this;
    }

    @Override
    public Stream<Query> streamV2() {
      return filtersV2.values().stream();
    }

    private Stream<Map.Entry<FilterNameAndScope, Query>> internalStreamV2() {
      return filtersV2.entrySet().stream();
    }
  }

  /**
   * Serves as a key in internal map of filters, it behaves the same as if the filterName was directly used as a key in
   * this map but also holds the name of the field each filter applies to.
   * <p>
   * This saves from using two internal maps.
   */
  private record FilterNameAndScope(String filterName, FilterScope filterScope) {

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FilterNameAndScope that = (FilterNameAndScope) o;
      return filterName.equals(that.filterName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(filterName);
    }
  }
}
