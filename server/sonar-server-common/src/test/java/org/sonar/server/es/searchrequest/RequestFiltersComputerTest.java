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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.server.es.searchrequest.RequestFiltersComputer.newAllFilters;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.NON_STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;

public class RequestFiltersComputerTest {

  private static final Random RANDOM = new Random();

  @Test
  public void getTopAggregationFilterV2_fails_with_IAE_when_no_TopAggregation_provided_in_constructor() {
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), Collections.emptySet());

    assertThatThrownBy(() -> underTest.getTopAggregationFilterV2(mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getTopAggregationFilterV2_fails_with_IAE_when_TopAggregation_was_not_provided_in_constructor() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(underTest::getTopAggregationFilterV2);
    assertThatThrownBy(() -> underTest.getTopAggregationFilterV2(mock(TopAggregationDefinition.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("topAggregation must have been declared in constructor");
  }

  @Test
  public void getQueryFiltersV2_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getQueryFiltersV2()).isEmpty();
  }

  @Test
  public void getPostFiltersV2_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    assertThat(underTest.getPostFiltersV2()).isEmpty();
  }

  @Test
  public void getTopAggregationFilterV2_returns_empty_if_AllFilters_is_empty() {
    Set<TopAggregationDefinition<?>> atLeastOneTopAggs = randomNonEmptyTopAggregations(RANDOM::nextBoolean);
    RequestFiltersComputer underTest = new RequestFiltersComputer(newAllFilters(), atLeastOneTopAggs);

    atLeastOneTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilterV2(topAgg)).isEmpty());
  }

  @Test
  public void getQueryFiltersV2_contains_all_filters_when_no_declared_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, Collections.emptySet());

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(allFilters.streamV2()));
  }

  @Test
  public void getPostFiltersV2_returns_empty_when_no_declared_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, Collections.emptySet());

    assertThat(underTest.getPostFiltersV2()).isEmpty();
  }

  @Test
  public void getQueryFiltersV2_contains_all_filters_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(allFilters.streamV2()));
  }

  @Test
  public void getPostFiltersV2_returns_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    assertThat(underTest.getPostFiltersV2()).isEmpty();
  }

  @Test
  public void getTopAggregationFiltersV2_return_empty_when_no_declared_sticky_topAggregation() {
    AllFilters allFilters = randomNonEmptyAllFilters();
    Set<TopAggregationDefinition<?>> atLeastOneNonStickyTopAggs = randomNonEmptyTopAggregations(() -> false);
    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, atLeastOneNonStickyTopAggs);

    atLeastOneNonStickyTopAggs.forEach(topAgg -> assertThat(underTest.getTopAggregationFilterV2(topAgg)).isEmpty());
  }

  @Test
  public void filters_on_field_of_sticky_TopAggregation_go_to_PostFilters_and_TopAgg_Filters_on_other_fields() {
    AllFilters allFilters = newAllFilters();
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    Query filterField1A = newQuery();
    Query filterField1B = newQuery();
    allFilters.addFilterV2("filter_field1_1", filterScopeField1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeField1, filterField1B);
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    Query filterField2 = newQuery();
    allFilters.addFilterV2("filter_field2", filterScopeField2, filterField2);
    String field3 = "field3";
    SimpleFieldFilterScope filterScopeField3 = new SimpleFieldFilterScope(field3);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField3 = new SimpleFieldTopAggregationDefinition(field3, NON_STICKY);
    Query filterField3 = newQuery();
    allFilters.addFilterV2("filter_field3", filterScopeField3, filterField3);
    String field4 = "field4";
    SimpleFieldFilterScope filterScopeField4 = new SimpleFieldFilterScope(field4);
    Query filterField4 = newQuery();
    allFilters.addFilterV2("filter_field4", filterScopeField4, filterField4);
    String field5 = "field5";
    SimpleFieldTopAggregationDefinition stickyTopAggField5 = new SimpleFieldTopAggregationDefinition(field5, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField5 = new SimpleFieldTopAggregationDefinition(field5, NON_STICKY);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      nonStickyTopAggField3,
      stickyTopAggField5, nonStickyTopAggField5);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(filterField3, filterField4));
    Query[] postFilters = {filterField1A, filterField1B, filterField2};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1A, filterField1B);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField3, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField5, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField5, postFilters);
  }

  @Test
  public void getTopAggregationFiltersV2_returns_empty_on_sticky_field_TopAgg_when_no_other_sticky_TopAgg() {
    AllFilters allFilters = newAllFilters();
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    Query filterField1A = newQuery();
    Query filterField1B = newQuery();
    allFilters.addFilterV2("filter_field1_1", filterScopeField1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeField1, filterField1B);
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    Query filterField2 = newQuery();
    allFilters.addFilterV2("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      nonStickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(filterField2));
    Query[] postFilters = {filterField1A, filterField1B};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertThat(underTest.getTopAggregationFilterV2(stickyTopAggField1)).isEmpty();
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
    NestedFieldFilterScope<String> filterScopeNestField_value1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    Query filterField1A = newQuery();
    Query filterField1B = newQuery();
    allFilters.addFilterV2("filter_field1_1", filterScopeNestField_value1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeNestField_value1, filterField1B);
    NestedFieldFilterScope<String> filterScopeNestField_value2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    Query filterField2 = newQuery();
    allFilters.addFilterV2("filter_field2", filterScopeNestField_value2, filterField2);
    NestedFieldFilterScope<String> filterScopeField3 = new NestedFieldFilterScope<>(field1, nestField, nestField_value3);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField3 = newNestedFieldTopAggDef(field1, nestField, nestField_value3, NON_STICKY);
    Query filterField3 = newQuery();
    allFilters.addFilterV2("filter_field3", filterScopeField3, filterField3);
    NestedFieldFilterScope<String> filterScopeField4 = new NestedFieldFilterScope<>(field1, nestField, nestField_value4);
    Query filterField4 = newQuery();
    allFilters.addFilterV2("filter_field4", filterScopeField4, filterField4);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField5 = newNestedFieldTopAggDef(field1, nestField, nestField_value5, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField5 = newNestedFieldTopAggDef(field1, nestField, nestField_value5, NON_STICKY);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      nonStickyTopAggField3,
      stickyTopAggField5, nonStickyTopAggField5);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(filterField3, filterField4));
    Query[] postFilters = {filterField1A, filterField1B, filterField2};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1A, filterField1B);
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
    Query queryFilter = newQuery("query_filter");
    allFilters.addFilterV2("query_filter", new SimpleFieldFilterScope("text"), queryFilter);
    NestedFieldFilterScope<String> filterScopeNestField1_value1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField1_value1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField1_value1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    Query filterNestedField1Value1A = newQuery("filterNestedField1Value1A");
    Query filterNestedField1Value1B = newQuery("filterNestedField1Value1B");
    allFilters.addFilterV2("filter_nested_field1_value1_1", filterScopeNestField1_value1, filterNestedField1Value1A);
    allFilters.addFilterV2("filter_nested_field1_value1_2", filterScopeNestField1_value1, filterNestedField1Value1B);
    NestedFieldFilterScope<String> filterScopeNestField1_value2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField1_value2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField1_value2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    Query filterNestedField1Value2A = newQuery("filterNestedField1Value2A");
    Query filterNestedField1Value2B = newQuery("filterNestedField1Value2B");
    allFilters.addFilterV2("filter_nested_field1_value2_1", filterScopeNestField1_value2, filterNestedField1Value2A);
    allFilters.addFilterV2("filter_nested_field1_value2_2", filterScopeNestField1_value2, filterNestedField1Value2B);
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    Query filterField1A = newQuery("filterField1A");
    Query filterField1B = newQuery("filterField1B");
    allFilters.addFilterV2("filter_field1_1", filterScopeField1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeField1, filterField1B);
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, NON_STICKY);
    Query filterField2A = newQuery("filterField2A");
    Query filterField2B = newQuery("filterField2B");
    allFilters.addFilterV2("filter_field2_1", filterScopeField2, filterField2A);
    allFilters.addFilterV2("filter_field2_2", filterScopeField2, filterField2B);
    NestedFieldFilterScope<String> filterScopeNestField3_value = new NestedFieldFilterScope<>(field3, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggNestedField3 = newNestedFieldTopAggDef(field3, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggNestedField3 = newNestedFieldTopAggDef(field3, nestField, nestField_value1, NON_STICKY);
    Query filterNestedField3A = newQuery("filterNestedField3A");
    Query filterNestedField3B = newQuery("filterNestedField3B");
    allFilters.addFilterV2("filter_nested_field3_1", filterScopeNestField3_value, filterNestedField3A);
    allFilters.addFilterV2("filter_nested_field3_2", filterScopeNestField3_value, filterNestedField3B);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggNestedField1_value1, nonStickyTopAggNestedField1_value1,
      stickyTopAggNestedField1_value2, nonStickyTopAggNestedField1_value2,
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2, nonStickyTopAggField2,
      stickyTopAggNestedField3, nonStickyTopAggNestedField3);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(queryFilter));
    Query[] postFilters = {
      filterNestedField1Value1A, filterNestedField1Value1B,
      filterNestedField1Value2A, filterNestedField1Value2B,
      filterField1A, filterField1B,
      filterField2A, filterField2B,
      filterNestedField3A, filterNestedField3B};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggNestedField1_value1,
      filterNestedField1Value2A, filterNestedField1Value2B,
      filterField1A, filterField1B,
      filterField2A, filterField2B,
      filterNestedField3A, filterNestedField3B);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField1_value1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggNestedField1_value2,
      filterNestedField1Value1A, filterNestedField1Value1B,
      filterField1A, filterField1B,
      filterField2A, filterField2B,
      filterNestedField3A, filterNestedField3B);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField1_value2, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField1,
      filterField2A, filterField2B,
      filterNestedField3A, filterNestedField3B);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2,
      filterNestedField1Value1A, filterNestedField1Value1B,
      filterNestedField1Value2A, filterNestedField1Value2B,
      filterField1A, filterField1B,
      filterNestedField3A, filterNestedField3B);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggNestedField3,
      filterNestedField1Value1A, filterNestedField1Value1B,
      filterNestedField1Value2A, filterNestedField1Value2B,
      filterField1A, filterField1B,
      filterField2A, filterField2B);
    assertTopAggregationFilter(underTest, nonStickyTopAggNestedField3, postFilters);
  }

  @Test
  public void getTopAggregationFiltersV2_returns_empty_on_sticky_nestedField_TopAgg_when_no_other_sticky_TopAgg() {
    String field1 = "field";
    String nestField = "nestedField";
    String nestField_value1 = "nestedField_value1";
    String nestField_value2 = "nestedField_value2";

    AllFilters allFilters = newAllFilters();
    NestedFieldFilterScope<String> filterScopeField1 = new NestedFieldFilterScope<>(field1, nestField, nestField_value1);
    NestedFieldTopAggregationDefinition<String> stickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, STICKY);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField1 = newNestedFieldTopAggDef(field1, nestField, nestField_value1, NON_STICKY);
    Query filterField1A = newQuery();
    Query filterField1B = newQuery();
    allFilters.addFilterV2("filter_field1_1", filterScopeField1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeField1, filterField1B);
    NestedFieldFilterScope<String> filterScopeField2 = new NestedFieldFilterScope<>(field1, nestField, nestField_value2);
    NestedFieldTopAggregationDefinition<String> nonStickyTopAggField2 = newNestedFieldTopAggDef(field1, nestField, nestField_value2, NON_STICKY);
    Query filterField2 = newQuery();
    allFilters.addFilterV2("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      nonStickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertQueryEquals(underTest.getQueryFiltersV2(), toBoolQuery(filterField2));
    Query[] postFilters = {filterField1A, filterField1B};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertThat(underTest.getTopAggregationFilterV2(stickyTopAggField1)).isEmpty();
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, nonStickyTopAggField2, postFilters);
  }

  @Test
  public void getQueryFiltersV2_returns_empty_when_all_filters_have_sticky_TopAggs() {
    AllFilters allFilters = newAllFilters();
    String field1 = "field1";
    SimpleFieldFilterScope filterScopeField1 = new SimpleFieldFilterScope(field1);
    SimpleFieldTopAggregationDefinition stickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, STICKY);
    SimpleFieldTopAggregationDefinition nonStickyTopAggField1 = new SimpleFieldTopAggregationDefinition(field1, NON_STICKY);
    Query filterField1A = newQuery();
    Query filterField1B = newQuery();
    allFilters.addFilterV2("filter_field1_1", filterScopeField1, filterField1A);
    allFilters.addFilterV2("filter_field1_2", filterScopeField1, filterField1B);
    String field2 = "field2";
    SimpleFieldFilterScope filterScopeField2 = new SimpleFieldFilterScope(field2);
    SimpleFieldTopAggregationDefinition stickyTopAggField2 = new SimpleFieldTopAggregationDefinition(field2, STICKY);
    Query filterField2 = newQuery();
    allFilters.addFilterV2("filter_field2", filterScopeField2, filterField2);
    Set<TopAggregationDefinition<?>> declaredTopAggregations = ImmutableSet.of(
      stickyTopAggField1, nonStickyTopAggField1,
      stickyTopAggField2);

    RequestFiltersComputer underTest = new RequestFiltersComputer(allFilters, declaredTopAggregations);

    assertThat(underTest.getQueryFiltersV2()).isEmpty();
    Query[] postFilters = {filterField1A, filterField1B, filterField2};
    assertQueryEquals(underTest.getPostFiltersV2(), toBoolQuery(postFilters));
    assertTopAggregationFilter(underTest, stickyTopAggField1, filterField2);
    assertTopAggregationFilter(underTest, nonStickyTopAggField1, postFilters);
    assertTopAggregationFilter(underTest, stickyTopAggField2, filterField1A, filterField1B);
  }

  private static Set<TopAggregationDefinition<?>> randomNonEmptyTopAggregations(Supplier<Boolean> isSticky) {
    return IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> new SimpleFieldTopAggregationDefinition("field_" + i, isSticky.get()))
      .collect(toSet());
  }

  private static Query toBoolQuery(Query first, Query... others) {
    return toBoolQuery(Stream.concat(Stream.of(first), Arrays.stream(others)));
  }

  private static Query toBoolQuery(Query[] subQueries) {
    return toBoolQuery(Arrays.stream(subQueries));
  }

  private static Query toBoolQuery(Stream<Query> stream) {
    List<Query> queries = stream.toList();
    checkState(!queries.isEmpty(), "empty stream is not supported");
    return Query.of(q -> q.bool(b -> {
      queries.forEach(b::must);
      return b;
    }));
  }

  private static AllFilters randomNonEmptyAllFilters() {
    AllFilters res = newAllFilters();
    IntStream.range(0, 1 + RANDOM.nextInt(22))
      .forEach(i -> res.addFilterV2("filter_" + i, mock(FilterScope.class), newQuery()));
    return res;
  }

  private static NestedFieldTopAggregationDefinition<String> newNestedFieldTopAggDef(String field1, String nestField, String nestField_value1, boolean sticky) {
    return new NestedFieldTopAggregationDefinition<>(field1 + "." + nestField, nestField_value1, sticky);
  }

  private static int queryCounter = 0;

  private static Query newQuery() {
    return Query.of(q -> q.term(t -> t.field("query_" + (queryCounter++)).value("foo")));
  }

  private static Query newQuery(String label) {
    return Query.of(q -> q.term(t -> t.field("query_" + label).value("foo")));
  }

  private static void assertTopAggregationFilter(RequestFiltersComputer underTest,
    TopAggregationDefinition<?> topAggregation, Query firstFilter, Query... otherFilters) {
    assertQueryEquals(underTest.getTopAggregationFilterV2(topAggregation), toBoolQuery(firstFilter, otherFilters));
  }

  private static void assertTopAggregationFilter(RequestFiltersComputer underTest,
    TopAggregationDefinition<?> topAggregation, Query[] filters) {
    assertQueryEquals(underTest.getTopAggregationFilterV2(topAggregation), toBoolQuery(filters));
  }

  private static void assertQueryEquals(java.util.Optional<Query> actual, Query expected) {
    assertThat(actual).isPresent();
    assertThat(actual.get()).hasToString(expected.toString());
  }
}