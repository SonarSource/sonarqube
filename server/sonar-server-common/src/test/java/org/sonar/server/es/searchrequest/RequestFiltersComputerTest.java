/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.mockito.Mockito.mock;
import static org.sonar.server.es.searchrequest.RequestFiltersComputer.newAllFilters;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.NON_STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;

public class RequestFiltersComputerTest {

  private static final Random RANDOM = new Random();

  @Test
  public void getTopAggregationFilters_fails_with_IAE_when_no_TopAggregation_provided_in_constructor() {
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), Collections.emptySet());

    assertThatThrownBy(() -> underTest.getTopAggregationFilter(mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getTopAggregationFilters_fails_with_IAE_when_TopAggregation_was_not_provided_in_constructor() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(underTest::getTopAggregationFilter);
    assertThatThrownBy(() -> underTest.getTopAggregationFilter(mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getQueryFilters_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getQueryFilters()).isEmpty();
  }

  @Test
  public void getPostFilters_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getPostFilters()).isEmpty();
  }

  @Test
  public void getTopAggregationFilter_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilter(topAgg)).isEmpty());
  }

  @Test
  public void getQueryFilters_contains_all_filters_when_no_declared_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, Collections.emptySet());

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(allFilters.stream()));
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
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(allFilters.stream()));
  }

  @Test
  public void getPostFilters_returns_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertThat(underTest.getPostFilters()).isEmpty();
  }

  @Test
  public void getTopAggregationFilters_return_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    atLeastOneNonStickyTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilter(topAgg)).isEmpty());
  }

  @Test
  public void filters_on_field_of_sticky_TopAggregation_go_to_PostFilters_and_TopAgg_Filters_on_other_fields() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter("filter_field1_1", filterScopeField1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeField1, filterField1_2);
    // has topAggs and one filter
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter("filter_field2", filterScopeField2, filterField2);
    // has only non-sticky top-agg and one filter
    String field3 = "field3";
    SimpleFieldFilterScope filterScopeField3 = new SimpleFieldFilterScope(field3);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField3 = new SimpleFieldTopAggregationDefinition(field3, NON_STICKY);
    QueryBuilder filterField3 = newQuery();
    allFilters.addFilter("filter_field3", filterScopeField3, filterField3);
    // has one filter but no top agg
    String field4 = "field4";
    SimpleFieldFilterScope filterScopeField4 = new SimpleFieldFilterScope(field4);
    QueryBuilder filterField4 = newQuery();
    allFilters.addFilter("filter_field4", filterScopeField4, filterField4);
    // has top-aggs by no filter
    String field5 = "field5";
    SimpleFieldTopAggregationDefinition stickyTopAggField5 = new SimpleFieldTopAggregationDefinition(field5, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField5 = new SimpleFieldTopAggregationDefinition(field5, NON_STICKY);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      nonStickyTopAggField3,
      stickyTopAggField5, nonStickyTopAggField5);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(filterField3, filterField4));
    QueryBuilder[] postFilters = {filterField1_1, filterField1_2, filterField2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1_1, filterField1_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField3, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField5, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField5, postFilters);
  }

  @Test
  public void getTopAggregationFilters_returns_empty_on_sticky_field_TopAgg_when_no_other_sticky_TopAgg() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter("filter_field1_1", filterScopeField1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeField1, filterField1_2);
    // has only non-sticky top-agg and one filter
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      nonStickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(filterField2));
    QueryBuilder[] postFilters = {filterField1_1, filterField1_2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField1)).isEmpty();
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
  }

  @Test
  public void filters_on_nestedField_of_sticky_TopAggregation_go_to_PostFilters_and_TopAgg_Filters_on_other_values_of_same_nestField() {
    String field1 = "field";
    String nestField = "nestedField";
    String nestField_value1 = "nestedField_value1";
    String nestField_value2 = "nestedField_value2";
    String nestField_value3 = "nestedField_value3";
    String nestField_value4 = "nestedField_value4";
    String nestField_value5 = "nestedField_value5";

    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    NestedFieldFilterScope<String> filterScopeNestField_value1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter("filter_field1_1", filterScopeNestField_value1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeNestField_value1, filterField1_2);
    // has topAggs and one filter
    NestedFieldFilterScope<String> filterScopeNestField_value2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter("filter_field2", filterScopeNestField_value2, filterField2);
    // has only non-sticky top-agg and one filter
    NestedFieldFilterScope<String> filterScopeField3 = new NestedFieldFilterScope<>(field1, nestField, nestField_value3);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField3 = newNestedFieldTopAggDef(field1, nestField, nestField_value3, NON_STICKY);
    QueryBuilder filterField3 = newQuery();
    allFilters.addFilter("filter_field3", filterScopeField3, filterField3);
    // has one filter but no top agg
    NestedFieldFilterScope<String> filterScopeField4 = new NestedFieldFilterScope<>(field1, nestField, nestField_value4);
    QueryBuilder filterField4 = newQuery();
    allFilters.addFilter("filter_field4", filterScopeField4, filterField4);
    // has top-aggs by no filter
    String field5 = "field5";
    NestedFieldTopAggregationDefinition<String> stickyTopAggField5 = newNestedFieldTopAggDef(field1, nestField, nestField_value5, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField5 = newNestedFieldTopAggDef(field1, nestField, nestField_value5, NON_STICKY);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      nonStickyTopAggField3,
      stickyTopAggField5, nonStickyTopAggField5);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(filterField3, filterField4));
    QueryBuilder[] postFilters = {filterField1_1, filterField1_2, filterField2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1_1, filterField1_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField3, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField5, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField5, postFilters);
  }

  @Test
  public void filters_on_nestedField_of_sticky_TopAggregation_go_to_PostFilters_and_TopAgg_Filters_on_other_fields() {
    String field1 = "field1";
    String field2 = "field2";
    String field3 = "field3";
    String nestField = "nestedField";
    String nestField_value1 = "nestedField_value1";
    String nestField_value2 = "nestedField_value2";

    AllFilters allFilters = newAllFilters();
    // filter without top aggregation
    QueryBuilder queryFilter = newQuery("query_filter");
    allFilters.addFilter("query_filter", new SimpleFieldFilterScope("text"), queryFilter);
    // nestedField of field1 with value1: has topAggs and two filters
    NestedFieldFilterScope<String> filterScopeNestField1_value1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField1_value1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField1_value1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    QueryBuilder filterNestedField1_value1_1 = newQuery("filterNestedField1_value1_1");
    QueryBuilder filterNestedField1_value1_2 = newQuery("filterNestedField1_value1_2");
    allFilters.addFilter("filter_nested_field1_value1_1", filterScopeNestField1_value1, filterNestedField1_value1_1);
    allFilters.addFilter("filter_nested_field1_value1_2", filterScopeNestField1_value1, filterNestedField1_value1_2);
    // nestedField of field1 with value2: has topAggs and two filters
    NestedFieldFilterScope<String> filterScopeNestField1_value2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField1_value2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField1_value2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    QueryBuilder filterNestedField1_value2_1 = newQuery("filterNestedField1_value2_1");
    QueryBuilder filterNestedField1_value2_2 = newQuery("filterNestedField1_value2_2");
    allFilters.addFilter("filter_nested_field1_value2_1", filterScopeNestField1_value2, filterNestedField1_value2_1);
    allFilters.addFilter("filter_nested_field1_value2_2", filterScopeNestField1_value2, filterNestedField1_value2_2);
    // [EDGE CASE] topAgg directly on field1: has topAggs and two filters
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery("filterField1_1");
    QueryBuilder filterField1_2 = newQuery("filterField1_2");
    allFilters.addFilter("filter_field1_1", filterScopeField1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeField1, filterField1_2);
    // other field: has topAggs and two filters too
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    QueryBuilder filterField2_1 = newQuery("filterField2_1");
    QueryBuilder filterField2_2 = newQuery("filterField2_2");
    allFilters.addFilter("filter_field2_1", filterScopeField2, filterField2_1);
    allFilters.addFilter("filter_field2_2", filterScopeField2, filterField2_2);
    // nestedField of another field (even though nestedField name and values are the same): has topAggs and two filters
    NestedFieldFilterScope<String> filterScopeNestField3_value = new NestedFieldFilterScope<>(field3, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField3 = newNestedFieldTopAggDef(field3, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField3 = newNestedFieldTopAggDef(field3, nestField, nestField_value1, NON_STICKY);
    QueryBuilder filterNestedField3_1 = newQuery("filterNestedField3_1");
    QueryBuilder filterNestedField3_2 = newQuery("filterNestedField3_2");
    allFilters.addFilter("filter_nested_field3_1", filterScopeNestField3_value, filterNestedField3_1);
    allFilters.addFilter("filter_nested_field3_2", filterScopeNestField3_value, filterNestedField3_2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggNestedField1_value1, nonStickyTopAggNestedField1_value1,
      stickyTopAggNestedField1_value2, nonStickyTopAggNestedField1_value2,
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      stickyTopAggNestedField3, nonStickyTopAggNestedField3);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(queryFilter));
    QueryBuilder[] postFilters = {
      filterNestedField1_value1_1, filterNestedField1_value1_2,
      filterNestedField1_value2_1, filterNestedField1_value2_2,
      filterField1_1, filterField1_2,
      filterField2_1, filterField2_2,
      filterNestedField3_1, filterNestedField3_2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggNestedField1_value1,
      filterNestedField1_value2_1, filterNestedField1_value2_2,
      filterField1_1, filterField1_2,
      filterField2_1, filterField2_2,
      filterNestedField3_1, filterNestedField3_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField1_value1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggNestedField1_value2,
      filterNestedField1_value1_1, filterNestedField1_value1_2,
      filterField1_1, filterField1_2,
      filterField2_1, filterField2_2,
      filterNestedField3_1, filterNestedField3_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField1_value2, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField1,
      filterField2_1, filterField2_2,
      filterNestedField3_1, filterNestedField3_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2,
      filterNestedField1_value1_1, filterNestedField1_value1_2,
      filterNestedField1_value2_1, filterNestedField1_value2_2,
      filterField1_1, filterField1_2,
      filterNestedField3_1, filterNestedField3_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggNestedField3,
      filterNestedField1_value1_1, filterNestedField1_value1_2,
      filterNestedField1_value2_1, filterNestedField1_value2_2,
      filterField1_1, filterField1_2,
      filterField2_1, filterField2_2);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField3, postFilters);
  }

  @Test
  public void getTopAggregationFilters_returns_empty_on_sticky_nestedField_TopAgg_when_no_other_sticky_TopAgg() {
    String field1 = "field";
    String nestField = "nestedField";
    String nestField_value1 = "nestedField_value1";
    String nestField_value2 = "nestedField_value2";

    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    NestedFieldFilterScope<String> filterScopeField1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter("filter_field1_1", filterScopeField1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeField1, filterField1_2);
    // has only non-sticky top-agg and one filter
    NestedFieldFilterScope<String> filterScopeField2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      nonStickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).contains(toBoolQuery(filterField2));
    QueryBuilder[] postFilters = {filterField1_1, filterField1_2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertThat(underTest.getTopAggregationFilter(stickyTopAggField1)).isEmpty();
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
  }

  @Test
  public void getQueryFilters_returns_empty_when_all_filters_have_sticky_TopAggs() {
    AllFilters allFilters = newAllFilters();
    // has topAggs and two filters
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    QueryBuilder filterField1_1 = newQuery();
    QueryBuilder filterField1_2 = newQuery();
    allFilters.addFilter("filter_field1_1", filterScopeField1, filterField1_1);
    allFilters.addFilter("filter_field1_2", filterScopeField1, filterField1_2);
    // has only sticky top-agg and one filter
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    QueryBuilder filterField2 = newQuery();
    allFilters.addFilter("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFilters()).isEmpty();
    QueryBuilder[] postFilters = {filterField1_1, filterField1_2, filterField2};
    assertThat(underTest.getPostFilters()).contains(toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1_1, filterField1_2);
  }

  private static Set<TopAggregationDefinition<?>> randomNonEmptyTopAggregations(Supplier<Boolean> isSticky) {
    return IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> new SimpleFieldTopAggregationDefinition("field_" + i, isSticky.get()))
      .collect(toSet());
  }

  private static BoolQueryBuilder toBoolQuery(QueryBuilder first, QueryBuilder... others) {
    return toBoolQuery(Stream.concat(
      Stream.of(first), Arrays.stream(others)));
  }

  private static BoolQueryBuilder toBoolQuery(QueryBuilder[] subQueries) {
    return toBoolQuery(Arrays.stream(subQueries));
  }

  private static BoolQueryBuilder toBoolQuery(Stream<QueryBuilder> stream) {
    BoolQueryBuilder res = boolQuery();
    stream.forEach(res::must);
    checkState(res.hasClauses(), "empty stream is not supported");
    return res;
  }

  private static AllFilters randomNonEmptyAllFilters() {
    AllFilters res = newAllFilters();
    IntStream.range(0, 1 + RANDOM.nextInt(22))
      .forEach(i -> res.addFilter("filter_" + i, mock(FilterScope.class), newQuery()));
    return res;
  }

  private static NestedFieldTopAggregationDefinition<String> newNestedFieldTopAggDef(String field1, String nestField, String nestField_value1, boolean sticky) {
    return new NestedFieldTopAggregationDefinition<>(field1 + "." + nestField, nestField_value1, sticky);
  }

  private static int queryCounter = 0;

  /**
   * Creates unique queries
   */
  private static QueryBuilder newQuery() {
    return QueryBuilders.termQuery("query_" + (queryCounter++), "foo");
  }

  private static QueryBuilder newQuery(String label) {
    return QueryBuilders.termQuery("query_" + label, "foo");
  }

  private static void assertTopAggregationFilter(RequestFiltersComputer underTest,
    TopAggregationDefinition<?> topAggregation, QueryBuilder firstFilter, QueryBuilder... otherFilters) {
    assertThat(underTest.getTopAggregationFilter(topAggregation)).contains(toBoolQuery(firstFilter, otherFilters));
  }

  private static void assertTopAggregationFilter(RequestFiltersComputer underTest,
    TopAggregationDefinition<?> topAggregation, QueryBuilder[] filters) {
    assertThat(underTest.getTopAggregationFilter(topAggregation)).contains(toBoolQuery(filters));
  }
}
