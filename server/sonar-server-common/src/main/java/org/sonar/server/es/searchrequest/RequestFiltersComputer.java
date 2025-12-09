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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

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

  private final Set<TopAggregationDefinition<?>> topAggregations;
  private final Map<FilterNameAndScope, QueryBuilder> postFilters;
  private final Map<FilterNameAndScope, QueryBuilder> queryFilters;

  public RequestFiltersComputer(AllFilters allFilters, Set<TopAggregationDefinition<?>> topAggregations) {
    this.topAggregations = ImmutableSet.copyOf(topAggregations);
    this.postFilters = computePostFilters((AllFiltersImpl) allFilters, topAggregations);
    this.queryFilters = computeQueryFilter((AllFiltersImpl) allFilters, postFilters);
  }

  public static AllFilters newAllFilters() {
    return new AllFiltersImpl();
  }

  /**
   * Any filter of the query which can not be applied to all top-aggregations must be applied as a PostFilter.
   * <p>
   * A filter with a given {@link FilterScope} can not be applied to the query when at least one sticky top-aggregation
   * is enabled which has the same {@link FilterScope}.
   */
  private static Map<FilterNameAndScope, QueryBuilder> computePostFilters(AllFiltersImpl allFilters,
    Set<TopAggregationDefinition<?>> topAggregations) {
    Set<FilterScope> enabledStickyTopAggregationtedFieldNames = topAggregations.stream()
      .filter(TopAggregationDefinition::isSticky)
      .map(TopAggregationDefinition::getFilterScope)
      .collect(Collectors.toSet());

    // use LinkedHashMap over MoreCollectors.uniqueIndex to preserve order and write UTs more easily
    Map<FilterNameAndScope, QueryBuilder> res = new LinkedHashMap<>();
    allFilters.internalStream()
      .filter(e -> enabledStickyTopAggregationtedFieldNames.contains(e.getKey().filterScope()))
      .forEach(e -> checkState(res.put(e.getKey(), e.getValue()) == null, "Duplicate: %s", e.getKey()));
    return res;
  }

  /**
   * Filters which can be applied directly to the query are only the filters which can also be applied to all
   * aggregations.
   * <p>
   * Aggregations are scoped by the filter of the query. If any top-aggregation need to not be applied a filter
   * (typical case is a filter on the field aggregated to implement sticky facet behavior), this filter can
   * not be applied to the query and therefor must be applied as PostFilter.
   */
  private static Map<FilterNameAndScope, QueryBuilder> computeQueryFilter(AllFiltersImpl allFilters,
    Map<FilterNameAndScope, QueryBuilder> postFilters) {
    Set<FilterNameAndScope> postFilterKeys = postFilters.keySet();

    // use LinkedHashMap over MoreCollectors.uniqueIndex to preserve order and write UTs more easily
    Map<FilterNameAndScope, QueryBuilder> res = new LinkedHashMap<>();
    allFilters.internalStream()
      .filter(e -> !postFilterKeys.contains(e.getKey()))
      .forEach(e -> checkState(res.put(e.getKey(), e.getValue()) == null, "Duplicate: %s", e.getKey()));
    return res;
  }

  /**
   * The {@link BoolQueryBuilder} to apply directly to the query in the ES request.
   * <p>
   * There could be no filter to apply to the query in the (unexpected but supported) case where all filters
   * need to be applied as PostFilter because none of them can be applied to all top-aggregations.
   */
  public Optional<BoolQueryBuilder> getQueryFilters() {
    return toBoolQuery(this.queryFilters, (e, v) -> true);
  }

  /**
   * The {@link BoolQueryBuilder} to add to the ES request as PostFilter
   * (see {@link org.elasticsearch.action.search.SearchRequestBuilder#setPostFilter(QueryBuilder)}).
   * <p>
   * There may be no PostFilter to apply at all. Typical case is when all filters apply to both the query and
   * all aggregations. (corner case: when there is no filter at all...)
   */
  public Optional<BoolQueryBuilder> getPostFilters() {
    return toBoolQuery(postFilters, (e, v) -> true);
  }

  /**
   * The {@link BoolQueryBuilder} to apply to the top aggregation for the specified {@link SimpleFieldTopAggregationDefinition}.
   * <p>
   * The filter of the aggregations for a top-aggregation will either be:
   * <ul>
   *     <li>the same as PostFilter, if the top-aggregation is non-sticky or none have the same FilterScope as
   *         {@code topAggregation}</li>
   *     <li>or the same as PostFilter minus any filter which applies to the same {@link FilterScope} of
   *         {@code topAggregation}for the (<strong>if it's sticky</strong>)</li>
   * </ul>
   *
   * @throws IllegalArgumentException if specified {@link TopAggregationDefinition} has not been specified in the constructor
   */
  public Optional<BoolQueryBuilder> getTopAggregationFilter(TopAggregationDefinition<?> topAggregation) {
    checkArgument(topAggregations.contains(topAggregation), "topAggregation must have been declared in constructor");
    return toBoolQuery(
      postFilters,
      (e, v) -> !topAggregation.isSticky() || !topAggregation.getFilterScope().intersect(e.filterScope()));
  }

  public Optional<BoolQueryBuilder> getPostFiltersExcluding(String filterNameToExclude) {
    return toBoolQuery(postFilters, (e, v) -> !e.filterName().equals(filterNameToExclude));
  }

  private static Optional<BoolQueryBuilder> toBoolQuery(Map<FilterNameAndScope, QueryBuilder> queryFilters,
    BiPredicate<FilterNameAndScope, QueryBuilder> predicate) {
    if (queryFilters.isEmpty()) {
      return empty();
    }

    List<QueryBuilder> selectQueryBuilders = queryFilters.entrySet().stream()
      .filter(e -> predicate.test(e.getKey(), e.getValue()))
      .map(Map.Entry::getValue)
      .toList();
    if (selectQueryBuilders.isEmpty()) {
      return empty();
    }

    BoolQueryBuilder res = boolQuery();
    selectQueryBuilders.forEach(res::must);
    return of(res);
  }

  /**
   * A mean to put together all filters which apply to a given Search request.
   */
  public interface AllFilters {

    /**
     * @throws IllegalArgumentException if a filter with the specified name has already been added
     */
    AllFilters addFilter(String name, FilterScope filterScope, @Nullable QueryBuilder filter);

    Stream<QueryBuilder> stream();
  }

  private static class AllFiltersImpl implements AllFilters {
    /**
     * Usage of LinkedHashMap only benefits unit tests by providing predictability of the order of the filters.
     * ES doesn't care of the order.
     */
    private final Map<FilterNameAndScope, QueryBuilder> filters = new LinkedHashMap<>();

    @Override
    public AllFilters addFilter(String name, FilterScope filterScope, @Nullable QueryBuilder filter) {
      requireNonNull(name, "name can't be null");
      requireNonNull(filterScope, "filterScope can't be null");

      if (filter == null) {
        return this;
      }

      checkArgument(
        filters.put(new FilterNameAndScope(name, filterScope), filter) == null,
        "A filter with name %s has already been added", name);
      return this;
    }

    @Override
    public Stream<QueryBuilder> stream() {
      return filters.values().stream();
    }

    private Stream<Map.Entry<FilterNameAndScope, QueryBuilder>> internalStream() {
      return filters.entrySet().stream();
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
