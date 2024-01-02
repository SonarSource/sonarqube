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

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_EXTRA_FILTER;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_OTHER_SUBAGGREGATION;

public class TopAggregationHelperTest {

  public static final int DEFAULT_BUCKET_SIZE = 10;
  private RequestFiltersComputer filtersComputer = mock(RequestFiltersComputer.class);
  private SubAggregationHelper subAggregationHelper = mock(SubAggregationHelper.class);
  private TopAggregationHelper underTest = new TopAggregationHelper(filtersComputer, subAggregationHelper);

  @Test
  public void buildTopAggregation_fails_with_ISE_if_no_subaggregation_added_by_lambda() {
    String aggregationName = "name";
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    assertThatThrownBy(() -> underTest.buildTopAggregation(aggregationName, topAggregation, NO_EXTRA_FILTER, NO_OTHER_SUBAGGREGATION))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("no sub-aggregation has been added to top-aggregation " + aggregationName);
  }

  @Test
  public void buildTopAggregation_adds_subAggregation_from_lambda_parameter() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    AggregationBuilder[] subAggs = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> AggregationBuilders.min("subAgg_" + i))
      .toArray(AggregationBuilder[]::new);
    String topAggregationName = randomAlphabetic(10);

    AggregationBuilder aggregationBuilder = underTest.buildTopAggregation(topAggregationName, topAggregation,
      NO_EXTRA_FILTER, t -> Arrays.stream(subAggs).forEach(t::subAggregation));

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getSubAggregations()).hasSize(subAggs.length);
    assertThat(aggregationBuilder.getSubAggregations()).containsExactlyInAnyOrder(subAggs);
  }

  @Test
  public void buildTopAggregation_adds_filter_from_FiltersComputer_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder computerFilter = boolQuery();
    BoolQueryBuilder otherFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.of(computerFilter));
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    MinAggregationBuilder subAggregation = AggregationBuilders.min("donut");
    String topAggregationName = randomAlphabetic(10);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTopAggregation(topAggregationName, topAggregation,
      NO_EXTRA_FILTER, t -> t.subAggregation(subAggregation));

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isSameAs(computerFilter);
  }

  @Test
  public void buildTopAggregation_has_empty_filter_when_FiltersComputer_returns_empty_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder otherFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.empty());
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    MinAggregationBuilder subAggregation = AggregationBuilders.min("donut");
    String topAggregationName = randomAlphabetic(10);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTopAggregation(topAggregationName, topAggregation,
      NO_EXTRA_FILTER, t -> t.subAggregation(subAggregation));

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isEqualTo(boolQuery()).isNotSameAs(otherFilter);
  }

  @Test
  public void buildTopAggregation_adds_filter_from_FiltersComputer_for_TopAggregation_and_extra_one() {
    String topAggregationName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder computerFilter = boolQuery();
    BoolQueryBuilder otherFilter = boolQuery();
    BoolQueryBuilder extraFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.of(computerFilter));
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    MinAggregationBuilder subAggregation = AggregationBuilders.min("donut");

    FilterAggregationBuilder aggregationBuilder = underTest.buildTopAggregation(topAggregationName, topAggregation,
      t -> t.must(extraFilter), t -> t.subAggregation(subAggregation));

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isEqualTo(computerFilter);
    assertThat(((BoolQueryBuilder) aggregationBuilder.getFilter()).must()).containsExactly(extraFilter);
  }

  @Test
  public void buildTopAggregation_does_not_add_subaggregation_from_subAggregationHelper() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.empty());
    MinAggregationBuilder subAggregation = AggregationBuilders.min("donut");
    String topAggregationName = randomAlphabetic(10);

    underTest.buildTopAggregation(topAggregationName, topAggregation, NO_EXTRA_FILTER, t -> t.subAggregation(subAggregation));

    verifyNoInteractions(subAggregationHelper);
  }

  @Test
  public void buildTermTopAggregation_adds_term_subaggregation_from_subAggregationHelper() {
    String topAggregationName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    TermsAggregationBuilder termSubAgg = AggregationBuilders.terms("foo");
    when(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, null)).thenReturn(termSubAgg);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTermTopAggregation(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER, NO_OTHER_SUBAGGREGATION);

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getSubAggregations()).hasSize(1);
    assertThat(aggregationBuilder.getSubAggregations().iterator().next()).isSameAs(termSubAgg);
  }

  @Test
  public void buildTermTopAggregation_adds_subAggregation_from_lambda_parameter() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    AggregationBuilder[] subAggs = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> AggregationBuilders.min("subAgg_" + i))
      .toArray(AggregationBuilder[]::new);
    String topAggregationName = randomAlphabetic(10);
    TermsAggregationBuilder termSubAgg = AggregationBuilders.terms("foo");
    when(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, null)).thenReturn(termSubAgg);
    AggregationBuilder[] allSubAggs = Stream.concat(Arrays.stream(subAggs), Stream.of(termSubAgg)).toArray(AggregationBuilder[]::new);

    AggregationBuilder aggregationBuilder = underTest.buildTermTopAggregation(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER, t -> Arrays.stream(subAggs).forEach(t::subAggregation));

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getSubAggregations()).hasSize(allSubAggs.length);
    assertThat(aggregationBuilder.getSubAggregations()).containsExactlyInAnyOrder(allSubAggs);
  }

  @Test
  public void buildTermTopAggregation_adds_filter_from_FiltersComputer_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder computerFilter = boolQuery();
    BoolQueryBuilder otherFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.of(computerFilter));
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    String topAggregationName = randomAlphabetic(10);
    TermsAggregationBuilder termSubAgg = AggregationBuilders.terms("foo");
    when(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, null)).thenReturn(termSubAgg);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTermTopAggregation(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER, NO_OTHER_SUBAGGREGATION);

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isSameAs(computerFilter);
  }

  @Test
  public void buildTermTopAggregation_has_empty_filter_when_FiltersComputer_returns_empty_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder otherFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.empty());
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    String topAggregationName = randomAlphabetic(10);
    TermsAggregationBuilder termSubAgg = AggregationBuilders.terms("foo");
    when(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, null)).thenReturn(termSubAgg);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTermTopAggregation(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER, NO_OTHER_SUBAGGREGATION);

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isEqualTo(boolQuery()).isNotSameAs(otherFilter);
  }

  @Test
  public void buildTermTopAggregation_adds_filter_from_FiltersComputer_for_TopAggregation_and_extra_one() {
    String topAggregationName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    SimpleFieldTopAggregationDefinition otherTopAggregation = new SimpleFieldTopAggregationDefinition("acme", false);
    BoolQueryBuilder computerFilter = boolQuery();
    BoolQueryBuilder otherFilter = boolQuery();
    BoolQueryBuilder extraFilter = boolQuery();
    when(filtersComputer.getTopAggregationFilter(topAggregation)).thenReturn(Optional.of(computerFilter));
    when(filtersComputer.getTopAggregationFilter(otherTopAggregation)).thenReturn(Optional.of(otherFilter));
    TermsAggregationBuilder termSubAgg = AggregationBuilders.terms("foo");
    when(subAggregationHelper.buildTermsAggregation(topAggregationName, topAggregation, null)).thenReturn(termSubAgg);

    FilterAggregationBuilder aggregationBuilder = underTest.buildTermTopAggregation(
      topAggregationName, topAggregation, null,
      t -> t.must(extraFilter), NO_OTHER_SUBAGGREGATION);

    assertThat(aggregationBuilder.getName()).isEqualTo(topAggregationName);
    assertThat(aggregationBuilder.getFilter()).isEqualTo(computerFilter);
    assertThat(((BoolQueryBuilder) aggregationBuilder.getFilter()).must()).containsExactly(extraFilter);
  }
}
