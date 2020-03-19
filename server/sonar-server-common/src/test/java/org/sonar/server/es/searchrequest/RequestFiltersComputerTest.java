/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.sonar.server.es.searchrequest.RequestFiltersComputer.newAllFilters;

public class RequestFiltersComputerTest {

  private static final Random RANDOM = new Random();

  @Test
  public void getTopAggregationFilters_fails_with_IAE_when_no_TopAggregation_provided_in_constructor() {
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), Collections.emptySet());

    assertThatThrownBy(() -> underTest.getTopAggregationFilter(Mockito.mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getTopAggregationFilters_fails_with_IAE_when_TopAggregation_was_not_provided_in_constructor() {
    Set<TopAggregationDefinition> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(underTest::getTopAggregationFilter);
    assertThatThrownBy(() -> underTest.getTopAggregationFilter(Mockito.mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getQueryFilters_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getQueryFilters()).isEmpty();
  }

  @Test
  public void getPostFilters_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getPostFilters()).isEmpty();
  }

  @Test
  public void getTopAggregationFilter_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilter(topAgg)).isEmpty());
  }

  @Test
  public void getQueryFilters_contains_all_filters_when_no_declared_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, Collections.emptySet());

    assertThat(underTest.getQueryFilters().get()).isEqualTo(toBoolQuery(allFilters.stream()));
  }

  @Test
  public void getPostFilters_returns_empty_when_no_declared_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, Collections.emptySet());

    assertThat(underTest.getPostFilters()).isEmpty();
  }

  @Test
  public void getQueryFilters_contains_all_filters_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertThat(underTest.getQueryFilters().get()).isEqualTo(toBoolQuery(allFilters.stream()));
  }

  @Test
  public void getPostFilters_returns_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertThat(underTest.getPostFilters()).isEmpty();
  }

  @Test
  public void getTopAggregationFilters_return_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    atLeastOneNonStickyTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilter(topAgg)).isEmpty());
  }

  @Test
  public void filters_on_field_of_sticky_TopAggregation_go_to_PostFilters_and_TopAgg_Filters_on_other_fields() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    TopAggregationDefinition stickyTopAggField1 = new TopAggregationDef(field1, true);
    TopAggregationDefinition nonStickyTopAggField1 = new TopAggregationDef(field1, false);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter(field1, filterField1_1);
    allFilters.addFilter(field1 + "_2", field1, filterField1_2);
    // has topAggs and one filter
    String field2 = "field2";
    TopAggregationDefinition stickyTopAggField2 = new TopAggregationDef(field2, true);
    TopAggregationDefinition nonStickyTopAggField2 = new TopAggregationDef(field2, false);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter(field2, filterField2);
    // has only non-sticky top-agg and one filter
    String field3 = "field3";
    TopAggregationDefinition nonStickyTopAggField3 = new TopAggregationDef(field3, false);
    QueryBuilder filterField3 = newQuery();
    allFilters.addFilter(field3, filterField3);
    // has one filter but no top agg
    String field4 = "field4";
    QueryBuilder filterField4 = newQuery();
    allFilters.addFilter(field4, filterField4);
    // has top-aggs by no filter
    String field5 = "field5";
    TopAggregationDefinition stickyTopAggField5 = new TopAggregationDef(field5, true);
    TopAggregationDefinition nonStickyTopAggField5 = new TopAggregationDef(field5, false);
    Set<TopAggregationDefinition> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      nonStickyTopAggField3,
      stickyTopAggField5, nonStickyTopAggField5);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters().get()).isEqualTo(toBoolQuery(filterField3, filterField4));
    BoolQueryBuilder postFilterQuery = toBoolQuery(filterField1_1, filterField1_2, filterField2);
    assertThat(underTest.getPostFilters().get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField1).get()).isEqualTo(toBoolQuery(filterField2));
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField1).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField2).get()).isEqualTo(toBoolQuery(filterField1_1, filterField1_2));
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField2).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField3).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField5).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField5).get()).isEqualTo(postFilterQuery);
  }

  @Test
  public void getTopAggregationFilters_returns_empty_on_sticky_TopAgg_when_no_other_sticky_TopAgg() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    TopAggregationDefinition stickyTopAggField1 = new TopAggregationDef(field1, true);
    TopAggregationDefinition nonStickyTopAggField1 = new TopAggregationDef(field1, false);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter(field1, filterField1_1);
    allFilters.addFilter(field1 + "_2", field1, filterField1_2);
    // has only non-sticky top-agg and one filter
    String field2 = "field2";
    TopAggregationDefinition nonStickyTopAggField2 = new TopAggregationDef(field2, false);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter(field2, filterField2);
    Set<TopAggregationDefinition> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      nonStickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters().get()).isEqualTo(toBoolQuery(filterField2));
    BoolQueryBuilder postFilterQuery = toBoolQuery(filterField1_1, filterField1_2);
    assertThat(underTest.getPostFilters().get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField1)).isEmpty();
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField1).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField2).get()).isEqualTo(postFilterQuery);
  }

  @Test
  public void getQueryFilters_returns_empty_when_all_filters_have_sticky_TopAggs() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    TopAggregationDefinition stickyTopAggField1 = new TopAggregationDef(field1, true);
    TopAggregationDefinition nonStickyTopAggField1 = new TopAggregationDef(field1, false);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter(field1, filterField1_1);
    allFilters.addFilter(field1 + "_2", field1, filterField1_2);
    // has only sticky top-agg and one filter
    String field2 = "field2";
    TopAggregationDefinition stickyTopAggField2 = new TopAggregationDef(field2, true);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter(field2, filterField2);
    Set<TopAggregationDefinition> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).isEmpty();
    BoolQueryBuilder postFilterQuery = toBoolQuery(filterField1_1, filterField1_2, filterField2);
    assertThat(underTest.getPostFilters().get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField1).get()).isEqualTo(toBoolQuery(filterField2));
    assertThat(underTest.getTopAggregationFilter(nonStickyTopAggField1).get()).isEqualTo(postFilterQuery);
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField2).get()).isEqualTo(toBoolQuery(filterField1_1, filterField1_2));
  }

  private static Set<TopAggregationDefinition> randomNonEmptyTopAggregations(Supplier<Boolean> isSticky) {
    return IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> new TopAggregationDef("field_" + i, isSticky.get()))
      .collect(toSet());
  }

  private static BoolQueryBuilder toBoolQuery(QueryBuilder first, QueryBuilder... others) {
    return toBoolQuery(Stream.concat(
      Stream.of(first), Arrays.stream(others)));
  }

  private static BoolQueryBuilder toBoolQuery(Stream<QueryBuilder> stream) {
    BoolQueryBuilder res = boolQuery();
    stream.forEach(res::must);
    return res;
  }

  private static AllFilters randomNonEmptyAllFilters() {
    AllFilters res = newAllFilters();
    IntStream.range(0, 1 + RANDOM.nextInt(22))
      .forEach(i -> res.addFilter("field_" + i, newQuery()));
    return res;
  }

  private static int queryCounter = 0;

  /**
   * Creates unique queries
   */
  private static QueryBuilder newQuery() {
    return QueryBuilders.termQuery("query_" + (queryCounter++), "foo");
  }
}
